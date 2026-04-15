package com.polsl.poiw.server;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.ActorIdGenerator;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.collision.CollisionSystem;
import com.polsl.poiw.engine.gameframework.GameMode;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.gameframework.PlayerState;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.gamemode.MainGameMode;
import com.polsl.poiw.engine.net.driver.ConnectionManager;
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.net.driver.PlayerConnection;
import com.polsl.poiw.engine.net.replication.ReplicationSystem;
import com.polsl.poiw.engine.system.ControllerSystem;
import com.polsl.poiw.engine.system.MovementSystem;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *  game server - headless LibGDX AcpplicationListener
 * runs GameWorld with server systems (no rendering)
 */
public class GameServer implements ApplicationListener {

    private static final String TAG = "GameServer";

    private GameWorld gameWorld;
    private NetDriver netDriver;
    private GameMode gameMode;
    private ReplicationSystem replicationSystem;

    // maps connectionId -> PlayerController (server posesses a controller for each player)
    private final Map<Integer, PlayerController> playerControllers = new HashMap<>();

    // playerId generator
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    private int tcpPort = NetworkProtocol.DEFAULT_TCP_PORT;
    private int udpPort = NetworkProtocol.DEFAULT_UDP_PORT;
    private int maxPlayers = 4;

    public GameServer() {}

    public GameServer(int tcpPort, int udpPort, int maxPlayers) {
        this.tcpPort = tcpPort;
        this.udpPort = udpPort;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void create() {
        Gdx.app.log(TAG, "Inicjalizacja serwera gry...");

        // server is authoritative source of IDs
        ActorIdGenerator.setServerMode(true);

        // GameWorld with physics (no rendering)
        gameWorld = new GameWorld();

        // server systems (NO Render, Camera, Debug)
        gameWorld.addSystem(new CollisionSystem(gameWorld.getBox2dWorld()));
        gameWorld.addSystem(new ControllerSystem());
        gameWorld.addSystem(new MovementSystem());

        // NetDriver
        netDriver = new NetDriver(true);
        netDriver.setConnectHandler(this::onClientConnected);
        netDriver.setDisconnectHandler(this::onClientDisconnected);
        netDriver.setMessageHandler(this::onMessageReceived);

        // ReplicationSystem
        replicationSystem = new ReplicationSystem(netDriver, gameWorld);
        gameWorld.addSystem(replicationSystem);

        // GameMode
        gameMode = new MainGameMode();
        gameMode.initGame(gameWorld);

        // start server
        try {
            netDriver.startServer(tcpPort, udpPort);
            Gdx.app.log(TAG, "Serwer gotowy, max graczy: " + maxPlayers);
        } catch (IOException e) {
            Gdx.app.error(TAG, "Nie można uruchomić serwera", e);
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // 1. process network messages (main thread!)
        netDriver.processMessages();

        // 2. update GameWorld (physics + ECS + ReplicationSystem)
        gameWorld.update(delta);

        // 3. GameMode tick
        gameMode.tick(delta);

        // 4. tick PlayerControllers
        for (PlayerController pc : playerControllers.values()) {
            pc.tick(delta);
        }


    }

    /**
     * connection handlers
     */

    private void onClientConnected(int connectionId) {
        Gdx.app.log(TAG, "Nowe połączenie: " + connectionId);
        // waiting for clientconnect message to accept and assign playerId
    }

    private void onClientDisconnected(int connectionId) {
        Gdx.app.log(TAG, "Rozłączenie: " + connectionId);

        ConnectionManager connMgr = netDriver.getConnectionManager();
        PlayerConnection conn = connMgr.getByConnectionId(connectionId);
        if (conn == null) return;

        // inform gamemode
        gameMode.onPlayerLogout(connectionId);

        // destroy player pawn and controller
        PlayerController pc = playerControllers.remove(connectionId);
        if (pc != null) {
            var pawn = pc.getPossessedPawn();
            if (pawn != null) {
                // inform clients
                var destroy = new NetworkProtocol.ActorDestroy();
                destroy.actorId = pawn.getActorId();
                netDriver.sendToAllClients(destroy, true);

                gameWorld.destroyActor(pawn);
            }
            pc.destroy();
        }

        // delete gamestate for player
        gameMode.getGameState().removePlayerState(conn.getPlayerId());
        gameMode.getGameState().setPlayerCount(connMgr.getPlayerCount() - 1);

        connMgr.removeConnection(connectionId);
    }

    private void onMessageReceived(int connectionId, Object message) {
        if (message instanceof NetworkProtocol.ClientConnect connect) {
            handleClientConnect(connectionId, connect);
        } else if (message instanceof NetworkProtocol.ClientInputUpdate input) {
            handleClientInput(connectionId, input);
        } else if (message instanceof NetworkProtocol.Ping ping) {
            handlePing(connectionId, ping);
        } else if (message instanceof NetworkProtocol.ClientDisconnect) {
            onClientDisconnected(connectionId);
        }
    }

    /**
     * message handlers
     */

    private void handleClientConnect(int connectionId, NetworkProtocol.ClientConnect connect) {
        // protocol validation
        if (connect.protocolVersion != NetworkProtocol.PROTOCOL_VERSION) {
            var reject = new NetworkProtocol.ServerReject();
            reject.reason = "Niezgodna wersja protokołu: " + connect.protocolVersion
                + " (wymagana: " + NetworkProtocol.PROTOCOL_VERSION + ")";
            netDriver.sendToClient(connectionId, reject, true);
            return;
        }

        // check player limit
        ConnectionManager connMgr = netDriver.getConnectionManager();
        if (connMgr.getPlayerCount() >= maxPlayers) {
            var reject = new NetworkProtocol.ServerReject();
            reject.reason = "Serwer pełny (" + maxPlayers + "/" + maxPlayers + ")";
            netDriver.sendToClient(connectionId, reject, true);
            return;
        }

        // assign playerId
        int playerId = nextPlayerId.getAndIncrement();
        String playerName = connect.playerName != null ? connect.playerName : "Player" + playerId;

        // register connection
        connMgr.addConnection(connectionId, playerId, playerName);

        // send accept message
        var accept = new NetworkProtocol.ServerAccept();
        accept.assignedPlayerId = playerId;
        accept.mapId = "game"; // TODO: current levelId
        accept.serverTime = replicationSystem.getTickCounter() * (1f / 20f);
        netDriver.sendToClient(connectionId, accept, true);

        Gdx.app.log(TAG, "Gracz zaakceptowany: " + playerName + " (ID=" + playerId + ")");

        // send existing actors to the new client
        sendExistingActors(connectionId);

        // spawn pawn for the new player
        spawnPlayerPawn(connectionId, playerId, playerName);

        // inform GameMode
        gameMode.onPlayerLogin(connectionId, playerId, playerName);

        // update GameState
        gameMode.getGameState().addPlayerState(new PlayerState(playerId, playerName));
        gameMode.getGameState().setPlayerCount(connMgr.getPlayerCount());
    }

    private void handleClientInput(int connectionId, NetworkProtocol.ClientInputUpdate input) {
        // validation: input magnitude <= 1.1 (margin for floating point)
        float magnitude = (float) Math.sqrt(input.dirX * input.dirX + input.dirY * input.dirY);
        if (magnitude > 1.1f) {
            float scale = 1f / magnitude;
            input.dirX *= scale;
            input.dirY *= scale;
        }

        PlayerController pc = playerControllers.get(connectionId);
        if (pc == null) return;

        pc.receiveClientInput(input.dirX, input.dirY, input.sequenceNumber);

        // update last processed sequence
        ConnectionManager connMgr = netDriver.getConnectionManager();
        PlayerConnection conn = connMgr.getByConnectionId(connectionId);
        if (conn != null) {
            conn.setLastProcessedInputSeq(input.sequenceNumber);
        }

        // send position correction
        var pawn = pc.getPossessedPawn();
        if (pawn != null) {
            var correction = new NetworkProtocol.ServerPositionCorrection();
            correction.actorId = pawn.getActorId();
            var pos = pawn.getPosition();
            correction.x = pos.x;
            correction.y = pos.y;
            correction.lastProcessedInput = input.sequenceNumber;
            correction.serverTime = replicationSystem.getTickCounter() * (1f / 20f);
            netDriver.sendToClient(connectionId, correction, false); // UDP
        }
    }

    private void handlePing(int connectionId, NetworkProtocol.Ping ping) {
        var pong = new NetworkProtocol.Pong();
        pong.clientTimestamp = ping.clientTimestamp;
        pong.serverTimestamp = System.currentTimeMillis();
        netDriver.sendToClient(connectionId, pong, false);
    }

    /**
     * helper methods
     */

    private void sendExistingActors(int connectionId) {
        Gdx.app.log(TAG, "Sending " + gameWorld.getAllActors().size() + " existing actors to connection " + connectionId);
        for (var actor : gameWorld.getAllActors()) {
            var spawn = new NetworkProtocol.ActorSpawn();
            spawn.actorId = actor.getActorId();
            spawn.actorClass = actor.getClass().getName();
            var pos = actor.getPosition();
            spawn.x = pos.x;
            spawn.y = pos.y;
            spawn.ownerId = actor.getOwnerId();
            netDriver.sendToClient(connectionId, spawn, true);
            Gdx.app.log(TAG, "  Sent ActorSpawn #" + spawn.actorId + " class=" + spawn.actorClass
                + " owner=" + spawn.ownerId + " pos=(" + spawn.x + "," + spawn.y + ")");
        }
    }

    private void spawnPlayerPawn(int connectionId, int playerId, String playerName) {
        // start pos
        ConnectionManager connMgr = netDriver.getConnectionManager();
        Vector2 spawnPos = gameMode.getPlayerStartPosition(connMgr.getPlayerCount() - 1);

        // create server-side actor
        // actor configuration without atlas (headless - no textures)
        AbstractActor pawn;
        if (gameMode.getDefaultPawnClass() != null) {
            try {
                pawn = gameMode.getDefaultPawnClass().getDeclaredConstructor().newInstance();
                // server configuration - components without rendering
                if (pawn instanceof PlayerCharacter pc) {
                    pc.configureServer();
                }
            } catch (Exception e) {
                Gdx.app.error(TAG, "Nie można stworzyć pawn: " + gameMode.getDefaultPawnClass(), e);
                return;
            }
        } else {
            Gdx.app.error(TAG, "Brak defaultPawnClass w GameMode");
            return;
        }

        pawn.setNetRole(NetRole.AUTHORITY);
        pawn.setOwnerId(playerId);
        pawn.setReplicated(true);
        gameWorld.spawnActor(pawn, spawnPos);

        // PlayerController
        PlayerController pc = new PlayerController();
        pc.setPlayerId(playerId);
        pc.setConnectionId(connectionId);
        pc.possess(pawn);
        playerControllers.put(connectionId, pc);

        // send ActorSpawn to all clients
        var spawn = new NetworkProtocol.ActorSpawn();
        spawn.actorId = pawn.getActorId();
        spawn.actorClass = pawn.getClass().getName();
        spawn.x = spawnPos.x;
        spawn.y = spawnPos.y;
        spawn.ownerId = playerId;
        netDriver.sendToAllClients(spawn, true);

        Gdx.app.log(TAG, "Spawned pawn for player " + playerName
            + " at (" + spawnPos.x + ", " + spawnPos.y + ")");
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        Gdx.app.log(TAG, "Shutting down server...");
        if (netDriver != null) netDriver.dispose();
        if (gameWorld != null) gameWorld.dispose();
    }
}

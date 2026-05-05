package com.polsl.poiw.server;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.ActorIdGenerator;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.asset.MapAsset;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.collision.CollisionSystem;
import com.polsl.poiw.engine.gameframework.GameMode;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.gameframework.PlayerState;
import com.polsl.poiw.engine.tiled.TiledMapParser;
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
 * game server — headless LibGDX ApplicationListener.
 * runs GameWorld with server systems (no rendering).
 * loads full logical Tiled map (collisions, triggers, spawn points).
 */
public class GameServer implements ApplicationListener {

    private static final String TAG = "GameServer";

    private GameWorld gameWorld;
    private NetDriver netDriver;
    private GameMode gameMode;
    private ReplicationSystem replicationSystem;
    private TiledMapParser tiledParser;

    // maps connectionId -> PlayerController (server possesses a controller for each player)
    private final Map<Integer, PlayerController> playerControllers = new HashMap<>();

    // playerId generator
    private final AtomicInteger nextPlayerId = new AtomicInteger(1);

    // debug: frame counter for heartbeat
    private long frameCount = 0;
    private float heartbeatTimer = 0f;
    private static final float HEARTBEAT_INTERVAL = 30f; // log every 30s

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
        Gdx.app.setLogLevel(com.badlogic.gdx.Application.LOG_DEBUG);
        Gdx.app.log(TAG, "Inicjalizacja serwera gry...");

        // server is authoritative source of IDs
        ActorIdGenerator.setServerMode(true);

        // GameWorld with physics (no rendering)
        gameWorld = new GameWorld();

        // server systems (NO Render, Camera, Debug)
        gameWorld.addSystem(new CollisionSystem(gameWorld.getBox2dWorld()));
        gameWorld.addSystem(new ControllerSystem());
        gameWorld.addSystem(new MovementSystem());

        // load and parse Tiled map — collisions, triggers, spawn points
        loadServerMap();

        // NetDriver
        netDriver = new NetDriver(true);
        netDriver.setConnectHandler(this::onClientConnected);
        netDriver.setDisconnectHandler(this::onClientDisconnected);
        netDriver.setMessageHandler(this::onMessageReceived);

        // ReplicationSystem — gameplay properties via TCP
        replicationSystem = new ReplicationSystem(netDriver, gameWorld);
        gameWorld.addSystem(replicationSystem);

        // MovementReplicationSystem — position/velocity via UDP
        var movementReplication = new com.polsl.poiw.engine.net.replication.MovementReplicationSystem(netDriver, gameWorld);
        gameWorld.addSystem(movementReplication);

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

    /**
     * loads Tiled map on server — creates collision bodies, triggers, and spawn points.
     * no rendering systems or textures.
     */
    private void loadServerMap() {
        var objectFactory = new ServerTiledObjectFactory(gameWorld);
        tiledParser = new TiledMapParser(gameWorld, null);
        tiledParser.setObjectFactory(objectFactory);

        // headless loader — parses TMX XML without loading textures
        var loader = new HeadlessTmxLoader();
        TiledMap map = loader.load(MapAsset.MAIN.getDescriptor().fileName);
        objectFactory.setMap(map);
        objectFactory.setTmxLoader(loader);
        tiledParser.parse(map);

        Gdx.app.log(TAG, "Mapa załadowana — spawn points: " + tiledParser.getAllPlayerStartPositions().size());
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        // cap delta to prevent physics accumulator spiral on first frames
        if (delta > 0.25f) {
            Gdx.app.debug(TAG, "Delta capped: " + delta + " → 0.25");
            delta = 0.25f;
        }

        frameCount++;
        heartbeatTimer += delta;
        if (heartbeatTimer >= HEARTBEAT_INTERVAL) {
            heartbeatTimer -= HEARTBEAT_INTERVAL;
            Gdx.app.debug(TAG, "[HEARTBEAT] frame=" + frameCount + " players=" + playerControllers.size());
        }

        // 1. process network messages (main thread!)
        netDriver.processMessages();

        // 2. update GameWorld (physics + ECS + ReplicationSystem)
        gameWorld.update(delta);

        // 3. send position corrections to each player (AFTER physics, so position is accurate)
        sendPlayerCorrections();

        // 4. GameMode tick
        gameMode.tick(delta);

        // 5. tick PlayerControllers
        for (PlayerController pc : playerControllers.values()) {
            pc.tick(delta);
        }
    }

    /**
     * connection handlers
     */

    private void onClientConnected(int connectionId) {
        Gdx.app.log(TAG, "Nowe połączenie: " + connectionId
            + " [frame=" + frameCount + " t=" + System.currentTimeMillis() + "]");
    }

    private void onClientDisconnected(int connectionId) {
        Gdx.app.log(TAG, "Rozłączenie: " + connectionId
            + " [frame=" + frameCount + " t=" + System.currentTimeMillis() + "]");

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
        Gdx.app.debug(TAG, "handleClientConnect from conn=" + connectionId
            + " player=" + connect.playerName + " proto=" + connect.protocolVersion
            + " [frame=" + frameCount + "]");

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

        Gdx.app.log(TAG, "Gracz zaakceptowany: " + playerName + " (ID=" + playerId
            + ") [frame=" + frameCount + "]");

        // send existing actors to the new client
        sendExistingActors(connectionId);

        // spawn pawn for the new player
        spawnPlayerPawn(connectionId, playerId, playerName);

        // send immediate movement snapshot so new client has up-to-date positions
        sendImmediateSnapshot(connectionId);

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

        // update last processed sequence — correction sent after physics in render()
        ConnectionManager connMgr = netDriver.getConnectionManager();
        PlayerConnection conn = connMgr.getByConnectionId(connectionId);
        if (conn != null) {
            conn.setLastProcessedInputSeq(input.sequenceNumber);
        }
    }

    private void handlePing(int connectionId, NetworkProtocol.Ping ping) {
        var pong = new NetworkProtocol.Pong();
        pong.clientTimestamp = ping.clientTimestamp;
        pong.serverTimestamp = System.currentTimeMillis();
        netDriver.sendToClient(connectionId, pong, false);
    }

    /**
     * sends authoritative position correction to each connected player.
     * called AFTER physics step in render() so body positions reflect processed inputs.
     * uses body position (not TransformComponent) for accuracy.
     */
    private void sendPlayerCorrections() {
        float serverTime = replicationSystem.getTickCounter() * (1f / 20f);

        for (var entry : playerControllers.entrySet()) {
            int connId = entry.getKey();
            PlayerController pc = entry.getValue();
            var pawn = pc.getPossessedPawn();
            if (pawn == null) continue;

            ConnectionManager connMgr = netDriver.getConnectionManager();
            PlayerConnection conn = connMgr.getByConnectionId(connId);
            if (conn == null) continue;

            // only send correction if new input was processed since last correction
            int lastInput = conn.getLastProcessedInputSeq();
            if (lastInput == conn.getLastCorrectedInputSeq()) continue;
            conn.setLastCorrectedInputSeq(lastInput);

            // get authoritative body position (post-physics)
            CollisionComponent collision = pawn.getComponentByType(CollisionComponent.class);
            if (collision == null || collision.getBody() == null) continue;

            var body = collision.getBody();
            Vector2 pos = body.getPosition();
            Vector2 vel = body.getLinearVelocity();

            var correction = new NetworkProtocol.ServerPositionCorrection();
            correction.actorId = pawn.getActorId();
            correction.x = pos.x;
            correction.y = pos.y;
            correction.velX = vel.x;
            correction.velY = vel.y;
            correction.lastProcessedInput = lastInput;
            correction.serverTime = serverTime;
            netDriver.sendToClient(connId, correction, false); // UDP
        }
    }

    /**
     * helper methods
     */

    private void sendExistingActors(int connectionId) {
        int count = 0;
        for (var actor : gameWorld.getAllActors()) {
            // only send replicated actors (PlayerCharacter etc.)
            // static world props (ServerPropActor, TriggerActor) exist on both sides from Tiled map
            if (!actor.isReplicated()) continue;

            var spawn = new NetworkProtocol.ActorSpawn();
            spawn.actorId = actor.getActorId();
            spawn.actorClass = actor.getClass().getName();

            // use TransformComponent position (bottom-left) — matches spawnActorInternal() on client
            var pos = actor.getPosition();
            spawn.x = pos.x;
            spawn.y = pos.y;

            spawn.ownerId = actor.getOwnerId();
            netDriver.sendToClient(connectionId, spawn, true);
            Gdx.app.log(TAG, "  Sent ActorSpawn #" + spawn.actorId + " class=" + spawn.actorClass
                + " owner=" + spawn.ownerId + " pos=(" + spawn.x + "," + spawn.y + ")");
            count++;
        }
        Gdx.app.log(TAG, "Sent " + count + " replicated actors to connection " + connectionId);
    }

    /**
     * sends immediate BatchMovementSnapshot to a single client.
     * ensures new client gets up-to-date positions for all actors on connect.
     */
    private void sendImmediateSnapshot(int connectionId) {
        java.util.List<NetworkProtocol.MovementSnapshot> snapshots = new java.util.ArrayList<>();

        for (var actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) continue;

            CollisionComponent collision = actor.getComponentByType(CollisionComponent.class);
            float x, y, velX = 0f, velY = 0f;
            if (collision != null && collision.getBody() != null) {
                var body = collision.getBody();
                x = body.getPosition().x;
                y = body.getPosition().y;
                velX = body.getLinearVelocity().x;
                velY = body.getLinearVelocity().y;
            } else {
                var pos = actor.getPosition();
                x = pos.x;
                y = pos.y;
            }

            var snapshot = new NetworkProtocol.MovementSnapshot();
            snapshot.actorId = actor.getActorId();
            snapshot.x = x;
            snapshot.y = y;
            snapshot.velX = velX;
            snapshot.velY = velY;
            snapshot.sequenceNumber = 0;
            snapshots.add(snapshot);
        }

        if (snapshots.isEmpty()) return;

        var batch = new NetworkProtocol.BatchMovementSnapshot();
        batch.snapshots = snapshots.toArray(new NetworkProtocol.MovementSnapshot[0]);
        batch.serverTime = replicationSystem.getTickCounter() * (1f / 20f);
        batch.serverTick = replicationSystem.getTickCounter();
        netDriver.sendToClient(connectionId, batch, false); // UDP
    }

    private void spawnPlayerPawn(int connectionId, int playerId, String playerName) {
        // spawn position from tiled map (or fallback from GameMode)
        ConnectionManager connMgr = netDriver.getConnectionManager();
        int playerIndex = connMgr.getPlayerCount() - 1;
        Vector2 spawnPos;
        if (tiledParser != null && !tiledParser.getAllPlayerStartPositions().isEmpty()) {
            spawnPos = tiledParser.getPlayerStartPosition(playerIndex);
        } else {
            spawnPos = gameMode.getPlayerStartPosition(playerIndex);
        }

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

    // ===== Server Travel =====
    private int travelIdCounter = 0;

    /**
     * initiates server travel — notifies all clients to travel to levelId.
     * preserveControllers = true → clients keep their PlayerController across levels.
     */
    public void serverTravel(String levelId, boolean preserveControllers) {
        travelIdCounter++;
        Gdx.app.log(TAG, "serverTravel → " + levelId + " (travelId=" + travelIdCounter + ")");

        var travel = new com.polsl.poiw.shared.protocol.NetworkProtocol.ServerTravel();
        travel.levelId = levelId;
        travel.travelId = travelIdCounter;
        travel.preserveControllers = preserveControllers;
        netDriver.sendToAllClients(travel, true);
    }

    @Override
    public void dispose() {
        Gdx.app.log(TAG, "Shutting down server...");
        if (netDriver != null) netDriver.dispose();
        if (gameWorld != null) gameWorld.dispose();
    }
}

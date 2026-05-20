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
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.collision.CollisionSystem;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.gameframework.GameMode;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.gameframework.PlayerState;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.engine.net.replication.ReplicationInfo;
import com.polsl.poiw.engine.tiled.TiledMapParser;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.gameplay.actor.TiledVisualActor;
import com.polsl.poiw.gameplay.actor.TrainingDummyActor;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.gamemode.MainGameMode;
import com.polsl.poiw.engine.net.driver.ConnectionManager;
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.net.driver.PlayerConnection;
import com.polsl.poiw.engine.net.replication.ReplicationSystem;
import com.polsl.poiw.engine.system.CombatSystem;
import com.polsl.poiw.engine.system.ControllerSystem;
import com.polsl.poiw.engine.system.MovementSystem;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.shared.protocol.NetworkProtocol;



import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * game server — headless LibGDX ApplicationListener.
 * runs GameWorld with server systems (no rendering).
 * loads full logical Tiled map (collisions, triggers, spawn points).
 */
public class GameServer implements ApplicationListener {

    private static final String TAG = "GameServer";
    private static final int ATTACK_INPUT_FLAG = 1 << 30;
    private static final int INPUT_SEQUENCE_MASK = ATTACK_INPUT_FLAG - 1;
    private static final int INITIAL_REPLICATION_BATCH_SIZE = 32;
    private static final int INITIAL_MOVEMENT_SNAPSHOT_BATCH_SIZE = 48;

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
    private final Set<Integer> knownReplicatedActorIds = new HashSet<>();

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
        gameWorld.addSystem(new CombatSystem());
        gameWorld.addSystem(new MovementSystem());

        // load and parse Tiled map — collisions, triggers, spawn points
        loadServerMap();
        syncKnownReplicatedActors();

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
        replicateActorLifecycleChanges();

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

        String leavingName = conn.getPlayerName();

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

        // broadcast leave message to remaining clients
        broadcastSystemMessage(leavingName + " left the game");
        broadcastPlayerList();
    }

    private void onMessageReceived(int connectionId, Object message) {
        if (message instanceof NetworkProtocol.ClientConnect connect) {
            handleClientConnect(connectionId, connect);
        } else if (message instanceof NetworkProtocol.ClientInputUpdate input) {
            handleClientInput(connectionId, input);
        } else if (message instanceof NetworkProtocol.ClientInventoryAction inventoryAction) {
            handleClientInventoryAction(connectionId, inventoryAction);
        } else if (message instanceof NetworkProtocol.ChatMessage chat) {
            handleChatMessage(connectionId, chat);
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
        sendFullReplicationState(connectionId);

        // spawn pawn for the new player
        spawnPlayerPawn(connectionId, playerId, playerName);

        // send immediate movement snapshot so new client has up-to-date positions
        sendImmediateSnapshot(connectionId);

        // inform GameMode
        gameMode.onPlayerLogin(connectionId, playerId, playerName);

        // update GameState
        gameMode.getGameState().addPlayerState(new PlayerState(playerId, playerName));
        gameMode.getGameState().setPlayerCount(connMgr.getPlayerCount());

        // broadcast join message to all clients
        broadcastSystemMessage(playerName + " joined the game");
        broadcastPlayerList();
    }

    private void handleClientInput(int connectionId, NetworkProtocol.ClientInputUpdate input) {
        boolean attackPressed = (input.sequenceNumber & ATTACK_INPUT_FLAG) != 0;
        int sequenceNumber = input.sequenceNumber & INPUT_SEQUENCE_MASK;

        // validation: input magnitude <= 1.1 (margin for floating point)
        float magnitude = (float) Math.sqrt(input.dirX * input.dirX + input.dirY * input.dirY);
        if (magnitude > 1.1f) {
            float scale = 1f / magnitude;
            input.dirX *= scale;
            input.dirY *= scale;
        }

        PlayerController pc = playerControllers.get(connectionId);
        if (pc == null) return;

        pc.receiveClientInput(input.dirX, input.dirY, sequenceNumber);

        if (attackPressed) {
            var pawn = pc.getPossessedPawn();
            if (pawn != null) {
                CombatComponent combat = pawn.getComponent(CombatComponent.class);
                if (combat != null) {
                    combat.requestAttack();
                }
            }
        }

        // update last processed sequence — correction sent after physics in render()
        ConnectionManager connMgr = netDriver.getConnectionManager();
        PlayerConnection conn = connMgr.getByConnectionId(connectionId);
        if (conn != null) {
            conn.setLastProcessedInputSeq(sequenceNumber);
        }
    }

    private void handlePing(int connectionId, NetworkProtocol.Ping ping) {
        var pong = new NetworkProtocol.Pong();
        pong.clientTimestamp = ping.clientTimestamp;
        pong.serverTimestamp = System.currentTimeMillis();
        netDriver.sendToClient(connectionId, pong, false);
    }

    private void handleChatMessage(int connectionId, NetworkProtocol.ChatMessage chat) {
        ConnectionManager connMgr = netDriver.getConnectionManager();
        PlayerConnection conn = connMgr.getByConnectionId(connectionId);
        if (conn == null) return;

        // Sanitize: enforce length limit, strip leading/trailing whitespace
        if (chat.message == null || chat.message.isBlank()) return;
        String msg = chat.message.trim();
        if (msg.length() > NetworkProtocol.MAX_CHAT_MESSAGE_LENGTH) {
            msg = msg.substring(0, NetworkProtocol.MAX_CHAT_MESSAGE_LENGTH);
        }

        // Build broadcast message with server-verified data
        var broadcast = new NetworkProtocol.ChatMessage();
        broadcast.playerId = conn.getPlayerId();
        broadcast.playerName = conn.getPlayerName();
        broadcast.message = msg;
        broadcast.timestamp = System.currentTimeMillis();
        broadcast.type = NetworkProtocol.ChatMessageType.PLAYER;

        netDriver.sendToAllClients(broadcast, true); // TCP — reliable
        Gdx.app.debug(TAG, "[CHAT] " + conn.getPlayerName() + ": " + msg);
    }

    private void broadcastSystemMessage(String message) {
        var chat = new NetworkProtocol.ChatMessage();
        chat.playerId = -1;
        chat.playerName = "Server";
        chat.message = message;
        chat.timestamp = System.currentTimeMillis();
        chat.type = NetworkProtocol.ChatMessageType.SYSTEM;
        netDriver.sendToAllClients(chat, true);
        Gdx.app.debug(TAG, "[SYSTEM] " + message);
    }

    private void broadcastPlayerList() {
        ConnectionManager connMgr = netDriver.getConnectionManager();
        var update = new NetworkProtocol.PlayerListUpdate();
        update.playerNames = connMgr.getAllConnections().stream()
            .map(PlayerConnection::getPlayerName)
            .toArray(String[]::new);
        netDriver.sendToAllClients(update, true);
    }

    private void handleClientInventoryAction(int connectionId, NetworkProtocol.ClientInventoryAction request) {
        if (request == null || request.action == null || request.itemId == null || request.itemId.isBlank()) {
            return;
        }

        PlayerController controller = playerControllers.get(connectionId);
        if (controller == null) {
            return;
        }

        if (request.playerId > 0 && request.playerId != controller.getPlayerId()) {
            Gdx.app.debug(TAG, "Ignoring inventory action with mismatched playerId from conn=" + connectionId);
            return;
        }

        if (!(controller.getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        if (inventory == null) {
            return;
        }

        switch (request.action) {
            case USE -> inventory.useItem(request.itemId);
            case DROP -> dropInventoryItem(player, inventory, request.itemId);
        }
    }

    private void dropInventoryItem(PlayerCharacter player, InventoryComponent inventory, String itemId) {
        InventoryStack stack = inventory.getStack(itemId);
        if (stack == null) {
            return;
        }

        if (inventory.removeItem(itemId, 1) > 0) {
            spawnItemNearPlayer(player, stack.getDefinition(), 1, 0.35f, 0.45f);
        }
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

    private void spawnItemNearPlayer(PlayerCharacter player,
                                     ItemDefinition item,
                                     int quantity,
                                     float heightOffset,
                                     float pickupGraceSeconds) {
        if (gameWorld == null || player == null || item == null || quantity <= 0) {
            return;
        }

        TransformComponent transform = player.getComponent(TransformComponent.class);
        Vector2 playerPosition = player.getPosition();
        float playerWidth = transform != null ? transform.getSize().x : 1f;
        float playerHeight = transform != null ? transform.getSize().y : 1f;
        float itemSize = 0.5f;

        Vector2 spawnPosition = new Vector2(
            playerPosition.x + playerWidth * 0.5f - itemSize * 0.5f,
            playerPosition.y + playerHeight + heightOffset
        );

        ItemPickupActor pickupActor = new ItemPickupActor();
        pickupActor.configureServer(item, quantity);
        pickupActor.setReplicated(true);
        pickupActor.setPickupGrace(player.getActorId(), pickupGraceSeconds);
        gameWorld.spawnActor(pickupActor, spawnPosition);
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
            spawn.initialProperties = buildInitialSpawnProperties(actor);
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

        sendMovementSnapshotsInChunks(connectionId, snapshots, INITIAL_MOVEMENT_SNAPSHOT_BATCH_SIZE);
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
        spawn.initialProperties = buildInitialSpawnProperties(pawn);
        netDriver.sendToAllClients(spawn, true);
        knownReplicatedActorIds.add(pawn.getActorId());

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

    private void sendFullReplicationState(int connectionId) {
        java.util.List<NetworkProtocol.ReplicationUpdate> updates = new java.util.ArrayList<>();

        for (var actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) {
                continue;
            }

            collectFullComponentUpdates(actor, updates);
        }

        if (updates.isEmpty()) {
            return;
        }

        sendReplicationUpdatesInChunks(connectionId, updates, INITIAL_REPLICATION_BATCH_SIZE);
    }

    private void sendReplicationUpdatesInChunks(int connectionId,
                                                java.util.List<NetworkProtocol.ReplicationUpdate> updates,
                                                int batchSize) {
        float serverTime = replicationSystem.getTickCounter() * (1f / 20f);
        int serverTick = replicationSystem.getTickCounter();

        for (int start = 0; start < updates.size(); start += batchSize) {
            int end = Math.min(start + batchSize, updates.size());
            var batch = new NetworkProtocol.BatchReplicationUpdate();
            batch.updates = updates.subList(start, end).toArray(new NetworkProtocol.ReplicationUpdate[0]);
            batch.serverTime = serverTime;
            batch.serverTick = serverTick;
            netDriver.sendToClient(connectionId, batch, true);
        }
    }

    private void sendMovementSnapshotsInChunks(int connectionId,
                                               java.util.List<NetworkProtocol.MovementSnapshot> snapshots,
                                               int batchSize) {
        float serverTime = replicationSystem.getTickCounter() * (1f / 20f);
        int serverTick = replicationSystem.getTickCounter();

        for (int start = 0; start < snapshots.size(); start += batchSize) {
            int end = Math.min(start + batchSize, snapshots.size());
            var batch = new NetworkProtocol.BatchMovementSnapshot();
            batch.snapshots = snapshots.subList(start, end).toArray(new NetworkProtocol.MovementSnapshot[0]);
            batch.serverTime = serverTime;
            batch.serverTick = serverTick;
            netDriver.sendToClient(connectionId, batch, false);
        }
    }

    private void collectFullComponentUpdates(com.polsl.poiw.engine.actor.Actor actor,
                                             java.util.List<NetworkProtocol.ReplicationUpdate> updates) {
        var components = actor.getAshleyEntity().getComponents();

        for (var component : components) {
            if (!(component instanceof com.polsl.poiw.engine.actor.ActorComponent actorComponent)) {
                continue;
            }
            if (!actorComponent.isReplicated()) {
                continue;
            }

            ReplicationInfo info = ReplicationInfo.scan(component.getClass());
            if (!info.hasReplicatedProperties()) {
                continue;
            }

            java.util.Map<String, Object> properties = new java.util.HashMap<>();
            for (var property : info.getProperties()) {
                properties.put(property.getFieldName(), property.getValue(component));
            }
            if (properties.isEmpty()) {
                continue;
            }

            var update = new NetworkProtocol.ReplicationUpdate();
            update.actorId = actor.getActorId();
            update.componentClass = component.getClass().getName();
            update.properties = properties;
            update.sequenceNumber = replicationSystem.getTickCounter();
            updates.add(update);
        }
    }

    private java.util.Map<String, Object> buildInitialSpawnProperties(com.polsl.poiw.engine.actor.Actor actor) {
        if (actor instanceof TrainingDummyActor trainingDummy) {
            return trainingDummy.buildInitialReplicationProperties();
        }

        if (actor instanceof com.polsl.poiw.gameplay.actor.AbstractCreatureActor creature) {
            return creature.buildInitialReplicationProperties();
        }

        if (actor instanceof com.polsl.poiw.gameplay.actor.AbstractTiledTargetActor tiledTargetActor) {
            return tiledTargetActor.buildInitialReplicationProperties();
        }

        if (actor instanceof TiledVisualActor tiledVisualActor) {
            return tiledVisualActor.buildInitialReplicationProperties();
        }

        if (actor instanceof ItemPickupActor itemPickupActor) {
            return itemPickupActor.buildInitialReplicationProperties();
        }

        return null;
    }

    private void replicateActorLifecycleChanges() {
        Map<Integer, com.polsl.poiw.engine.actor.Actor> currentActors = new HashMap<>();
        for (var actor : gameWorld.getAllActors()) {
            if (actor.isReplicated()) {
                currentActors.put(actor.getActorId(), actor);
            }
        }

        for (var entry : currentActors.entrySet()) {
            if (knownReplicatedActorIds.contains(entry.getKey())) {
                continue;
            }

            var actor = entry.getValue();
            var spawn = new NetworkProtocol.ActorSpawn();
            spawn.actorId = actor.getActorId();
            spawn.actorClass = actor.getClass().getName();

            var pos = actor.getPosition();
            spawn.x = pos.x;
            spawn.y = pos.y;
            spawn.ownerId = actor.getOwnerId();
            spawn.initialProperties = buildInitialSpawnProperties(actor);
            netDriver.sendToAllClients(spawn, true);
        }

        Set<Integer> currentIds = new HashSet<>(currentActors.keySet());

        for (Integer knownId : new HashSet<>(knownReplicatedActorIds)) {
            if (currentIds.contains(knownId)) {
                continue;
            }

            var destroy = new NetworkProtocol.ActorDestroy();
            destroy.actorId = knownId;
            netDriver.sendToAllClients(destroy, true);
            knownReplicatedActorIds.remove(knownId);
        }

        knownReplicatedActorIds.addAll(currentIds);
    }

    private void syncKnownReplicatedActors() {
        knownReplicatedActorIds.clear();
        for (var actor : gameWorld.getAllActors()) {
            if (actor.isReplicated()) {
                knownReplicatedActorIds.add(actor.getActorId());
            }
        }
    }
}

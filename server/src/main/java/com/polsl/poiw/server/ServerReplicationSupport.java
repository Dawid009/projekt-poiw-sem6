package com.polsl.poiw.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.gameframework.GameMode;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.net.driver.ConnectionManager;
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.net.driver.PlayerConnection;
import com.polsl.poiw.engine.net.replication.ReplicationInfo;
import com.polsl.poiw.engine.net.replication.ReplicationSystem;
import com.polsl.poiw.engine.tiled.TiledMapParser;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.gameplay.actor.NpcTraderActor;
import com.polsl.poiw.gameplay.actor.TiledVisualActor;
import com.polsl.poiw.gameplay.actor.TrainingDummyActor;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class ServerReplicationSupport {
    private static final String TAG = "GameServer";
    private static final float CORRECTION_VELOCITY_EPSILON = 0.04f;
    private static final float CORRECTION_POSITION_EPSILON = 0.02f;

    private final GameWorld gameWorld;
    private final NetDriver netDriver;
    private final ReplicationSystem replicationSystem;
    private final Set<Integer> knownReplicatedActorIds;

    ServerReplicationSupport(GameWorld gameWorld,
                             NetDriver netDriver,
                             ReplicationSystem replicationSystem,
                             Set<Integer> knownReplicatedActorIds) {
        this.gameWorld = gameWorld;
        this.netDriver = netDriver;
        this.replicationSystem = replicationSystem;
        this.knownReplicatedActorIds = knownReplicatedActorIds;
    }

    void syncKnownReplicatedActors() {
        knownReplicatedActorIds.clear();
        for (var actor : gameWorld.getAllActors()) {
            if (actor.isReplicated()) {
                knownReplicatedActorIds.add(actor.getActorId());
            }
        }
    }

    void replicateActorLifecycleChanges() {
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

    void sendPlayerCorrections(Map<Integer, PlayerController> playerControllers) {
        int serverTick = replicationSystem.getTickCounter();
        if (serverTick <= 0) {
            return;
        }

        float serverTime = serverTick * (1f / 20f);

        for (var entry : playerControllers.entrySet()) {
            int connId = entry.getKey();
            PlayerController pc = entry.getValue();
            var pawn = pc.getPossessedPawn();
            if (pawn == null) {
                continue;
            }

            ConnectionManager connMgr = netDriver.getConnectionManager();
            PlayerConnection conn = connMgr.getByConnectionId(connId);
            if (conn == null) {
                continue;
            }

            int lastInput = conn.getLastProcessedInputSeq();
            if (conn.getLastCorrectionSentTick() == serverTick) {
                continue;
            }

            CollisionComponent collision = pawn.getComponentByType(CollisionComponent.class);
            if (collision == null || collision.getBody() == null) {
                continue;
            }

            var body = collision.getBody();
            Vector2 pos = body.getPosition();
            Vector2 vel = body.getLinearVelocity();
            boolean moving = vel.len2() > CORRECTION_VELOCITY_EPSILON * CORRECTION_VELOCITY_EPSILON;
            boolean inputAdvanced = lastInput != conn.getLastCorrectionSentSeq();
            boolean positionChanged = hasCorrectionPositionChanged(conn, pos, vel);
            if (!moving && !inputAdvanced && !positionChanged) {
                continue;
            }

            var correction = new NetworkProtocol.ServerPositionCorrection();
            correction.actorId = pawn.getActorId();
            correction.x = pos.x;
            correction.y = pos.y;
            correction.velX = vel.x;
            correction.velY = vel.y;
            correction.lastProcessedInput = lastInput;
            correction.serverTime = serverTime;
            netDriver.sendToClient(connId, correction, false);
            conn.setLastCorrectionSentTick(serverTick);
            conn.setLastCorrectionSentSeq(lastInput);
            conn.setLastCorrectionX(pos.x);
            conn.setLastCorrectionY(pos.y);
            conn.setLastCorrectionVelX(vel.x);
            conn.setLastCorrectionVelY(vel.y);
        }
    }

    void sendExistingActors(int connectionId) {
        int count = 0;
        for (var actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) {
                continue;
            }

            var spawn = new NetworkProtocol.ActorSpawn();
            spawn.actorId = actor.getActorId();
            spawn.actorClass = actor.getClass().getName();
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

    void sendFullReplicationState(int connectionId, int batchSize) {
        List<NetworkProtocol.ReplicationUpdate> updates = new ArrayList<>();
        for (var actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) {
                continue;
            }
            collectFullComponentUpdates(actor, updates);
        }

        if (updates.isEmpty()) {
            return;
        }

        sendReplicationUpdatesInChunks(connectionId, updates, batchSize);
    }

    void sendImmediateSnapshot(int connectionId, int batchSize) {
        List<NetworkProtocol.MovementSnapshot> snapshots = new ArrayList<>();

        for (var actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) {
                continue;
            }
            if (actor instanceof ItemPickupActor) {
                continue;
            }

            CollisionComponent collision = actor.getComponentByType(CollisionComponent.class);
            float x;
            float y;
            float velX = 0f;
            float velY = 0f;
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

        if (snapshots.isEmpty()) {
            return;
        }

        sendMovementSnapshotsInChunks(connectionId, snapshots, batchSize);
    }

    void spawnPlayerPawn(GameMode gameMode,
                         TiledMapParser tiledParser,
                         Map<Integer, PlayerController> playerControllers,
                         int connectionId,
                         int playerId,
                         String playerName,
                         int spawnIndex) {
        Vector2 spawnPos = resolvePlayerSpawnPosition(gameMode, tiledParser, spawnIndex);

        AbstractActor pawn;
        if (gameMode.getDefaultPawnClass() != null) {
            try {
                pawn = gameMode.getDefaultPawnClass().getDeclaredConstructor().newInstance();
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

        PlayerController pc = new PlayerController();
        pc.setPlayerId(playerId);
        pc.setConnectionId(connectionId);
        pc.possess(pawn);
        playerControllers.put(connectionId, pc);

        var spawn = new NetworkProtocol.ActorSpawn();
        spawn.actorId = pawn.getActorId();
        spawn.actorClass = pawn.getClass().getName();
        spawn.x = spawnPos.x;
        spawn.y = spawnPos.y;
        spawn.ownerId = pawn.getOwnerId();
        spawn.initialProperties = buildInitialSpawnProperties(pawn);
        netDriver.sendToAllClients(spawn, true);
        knownReplicatedActorIds.add(pawn.getActorId());

        Gdx.app.log(TAG, "Spawned pawn for player " + playerName
            + " at (" + spawnPos.x + ", " + spawnPos.y + ")");
    }

    Vector2 resolvePlayerSpawnPosition(GameMode gameMode, TiledMapParser tiledParser, int spawnIndex) {
        if (tiledParser != null && !tiledParser.getAllPlayerStartPositions().isEmpty()) {
            return new Vector2(tiledParser.getPlayerStartPosition(spawnIndex));
        }

        return new Vector2(gameMode.getPlayerStartPosition(spawnIndex));
    }

    private void sendReplicationUpdatesInChunks(int connectionId,
                                                List<NetworkProtocol.ReplicationUpdate> updates,
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
                                               List<NetworkProtocol.MovementSnapshot> snapshots,
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
                                             List<NetworkProtocol.ReplicationUpdate> updates) {
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

            Map<String, Object> properties = new HashMap<>();
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

    private Map<String, Object> buildInitialSpawnProperties(com.polsl.poiw.engine.actor.Actor actor) {
        if (actor instanceof TrainingDummyActor trainingDummy) {
            return toSerializableSpawnProperties(trainingDummy.buildInitialReplicationProperties());
        }
        if (actor instanceof com.polsl.poiw.gameplay.actor.AbstractCreatureActor creature) {
            return toSerializableSpawnProperties(creature.buildInitialReplicationProperties());
        }
        if (actor instanceof com.polsl.poiw.gameplay.actor.AbstractTiledTargetActor tiledTargetActor) {
            return toSerializableSpawnProperties(tiledTargetActor.buildInitialReplicationProperties());
        }
        if (actor instanceof TiledVisualActor tiledVisualActor) {
            return toSerializableSpawnProperties(tiledVisualActor.buildInitialReplicationProperties());
        }
        if (actor instanceof ItemPickupActor itemPickupActor) {
            return toSerializableSpawnProperties(itemPickupActor.buildInitialReplicationProperties());
        }
        if (actor instanceof NpcTraderActor npcTraderActor) {
            return toSerializableSpawnProperties(npcTraderActor.buildInitialReplicationProperties());
        }
        return new HashMap<>();
    }

    private Map<String, Object> toSerializableSpawnProperties(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return new HashMap<>();
        }

        return new HashMap<>(properties);
    }

    private boolean hasCorrectionPositionChanged(PlayerConnection conn, Vector2 pos, Vector2 vel) {
        if (Float.isNaN(conn.getLastCorrectionX()) || Float.isNaN(conn.getLastCorrectionY())) {
            return true;
        }

        float dx = pos.x - conn.getLastCorrectionX();
        float dy = pos.y - conn.getLastCorrectionY();
        float dvx = vel.x - conn.getLastCorrectionVelX();
        float dvy = vel.y - conn.getLastCorrectionVelY();
        return dx * dx + dy * dy > CORRECTION_POSITION_EPSILON * CORRECTION_POSITION_EPSILON
            || dvx * dvx + dvy * dvy > CORRECTION_VELOCITY_EPSILON * CORRECTION_VELOCITY_EPSILON;
    }
}

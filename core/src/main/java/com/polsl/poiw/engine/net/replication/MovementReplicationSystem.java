package com.polsl.poiw.engine.net.replication;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * server-side movement replication — sends position/velocity snapshots via UDP.
 * separate from property replication (TCP) for low-latency movement updates.
 * runs at configurable rate (default 20 Hz).
 */
public class MovementReplicationSystem extends EntitySystem {

    private static final int MAX_SNAPSHOTS_PER_BATCH = 48;
    private static final float POSITION_EPSILON = 0.015f;
    private static final float VELOCITY_EPSILON = 0.03f;

    private final NetDriver netDriver;
    private final GameWorld gameWorld;
    private final Map<Integer, SentMovementState> lastSentStates = new HashMap<>();
    private float replicationRate = 1f / 20f; // 20 Hz
    private float timer = 0f;
    private int tickCounter = 0;

    public MovementReplicationSystem(NetDriver netDriver, GameWorld gameWorld) {
        super(91); // after ReplicationSystem (90)
        this.netDriver = netDriver;
        this.gameWorld = gameWorld;
    }

    @Override
    public void update(float delta) {
        timer += delta;
        if (timer < replicationRate) return;
        timer -= replicationRate;
        tickCounter++;

        sendMovementSnapshots();
    }

    private void sendMovementSnapshots() {
        List<NetworkProtocol.MovementSnapshot> snapshots = new ArrayList<>();
        Map<Integer, SentMovementState> currentStates = new HashMap<>();

        for (Actor actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) continue;

            CollisionComponent collision = actor.getComponentByType(CollisionComponent.class);
            if (!shouldReplicateMovement(actor, collision)) continue;

            float posX, posY, velX = 0f, velY = 0f;

            // use body position (post-physics, authoritative)
            if (collision != null && collision.getBody() != null) {
                Body body = collision.getBody();
                posX = body.getPosition().x;
                posY = body.getPosition().y;
                Vector2 vel = body.getLinearVelocity();
                velX = vel.x;
                velY = vel.y;
            } else {
                var pos = actor.getPosition();
                posX = pos.x;
                posY = pos.y;
            }

            SentMovementState nextState = new SentMovementState(posX, posY, velX, velY);
            currentStates.put(actor.getActorId(), nextState);
            SentMovementState previousState = lastSentStates.get(actor.getActorId());
            if (previousState != null && !hasMeaningfulMovementChange(previousState, nextState)) {
                continue;
            }

            var snapshot = new NetworkProtocol.MovementSnapshot();
            snapshot.actorId = actor.getActorId();
            snapshot.x = posX;
            snapshot.y = posY;
            snapshot.velX = velX;
            snapshot.velY = velY;
            snapshot.sequenceNumber = tickCounter;
            snapshots.add(snapshot);
        }

        if (snapshots.isEmpty()) return;
        lastSentStates.clear();
        lastSentStates.putAll(currentStates);

        for (int start = 0; start < snapshots.size(); start += MAX_SNAPSHOTS_PER_BATCH) {
            int end = Math.min(start + MAX_SNAPSHOTS_PER_BATCH, snapshots.size());
            var batch = new NetworkProtocol.BatchMovementSnapshot();
            batch.snapshots = snapshots.subList(start, end).toArray(new NetworkProtocol.MovementSnapshot[0]);
            batch.serverTime = tickCounter * replicationRate;
            batch.serverTick = tickCounter;

            // UDP — unreliable, latest-wins
            netDriver.sendToAllClients(batch, false);
        }
    }

    private boolean shouldReplicateMovement(Actor actor, CollisionComponent collision) {
        if (actor instanceof ItemPickupActor) {
            return false;
        }

        if (actor.getComponent(MovementComponent.class) != null) {
            return true;
        }

        if (collision == null || collision.getBody() == null) {
            return false;
        }

        return collision.getBody().getType() != BodyDef.BodyType.StaticBody;
    }

    public void setReplicationRate(float hz) {
        this.replicationRate = 1f / hz;
    }

    public int getTickCounter() { return tickCounter; }

    private boolean hasMeaningfulMovementChange(SentMovementState previous, SentMovementState current) {
        float dx = current.x - previous.x;
        float dy = current.y - previous.y;
        float dvx = current.velX - previous.velX;
        float dvy = current.velY - previous.velY;
        return dx * dx + dy * dy > POSITION_EPSILON * POSITION_EPSILON
            || dvx * dvx + dvy * dvy > VELOCITY_EPSILON * VELOCITY_EPSILON;
    }

    private record SentMovementState(float x, float y, float velX, float velY) {
    }
}

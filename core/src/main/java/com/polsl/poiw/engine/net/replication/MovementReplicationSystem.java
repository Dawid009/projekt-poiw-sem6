package com.polsl.poiw.engine.net.replication;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.ArrayList;
import java.util.List;

/**
 * server-side movement replication — sends position/velocity snapshots via UDP.
 * separate from property replication (TCP) for low-latency movement updates.
 * runs at configurable rate (default 20 Hz).
 */
public class MovementReplicationSystem extends EntitySystem {

    private final NetDriver netDriver;
    private final GameWorld gameWorld;
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

        for (Actor actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) continue;

            float posX, posY, velX = 0f, velY = 0f;

            // use body position (post-physics, authoritative)
            CollisionComponent collision = actor.getComponentByType(CollisionComponent.class);
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

        var batch = new NetworkProtocol.BatchMovementSnapshot();
        batch.snapshots = snapshots.toArray(new NetworkProtocol.MovementSnapshot[0]);
        batch.serverTime = tickCounter * replicationRate;
        batch.serverTick = tickCounter;

        // UDP — unreliable, latest-wins
        netDriver.sendToAllClients(batch, false);
    }

    public void setReplicationRate(float hz) {
        this.replicationRate = 1f / hz;
    }

    public int getTickCounter() { return tickCounter; }
}

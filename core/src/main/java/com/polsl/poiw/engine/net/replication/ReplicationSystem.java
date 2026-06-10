package com.polsl.poiw.engine.net.replication;

import com.badlogic.ashley.core.EntitySystem;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.ActorComponent;
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * replication system runs on the server every N ticks (set by setReplicationRate)
 * scans actors with replicated components, collect dirty properties
 * and sends BatchReplicationUpdate to clients
 */
public class ReplicationSystem extends EntitySystem {

    private static final int MAX_UPDATES_PER_BATCH = 32;

    private final NetDriver netDriver;
    private final GameWorld gameWorld;
    private float replicationRate = 1f / 20f; // 20 Hz
    private float timer = 0f;
    private int tickCounter = 0;

    public ReplicationSystem(NetDriver netDriver, GameWorld gameWorld) {
        super(90); // high priority — after physics and movement
        this.netDriver = netDriver;
        this.gameWorld = gameWorld;
    }

    @Override
    public void update(float delta) {
        timer += delta;
        if (timer < replicationRate) return;
        timer -= replicationRate;
        tickCounter++;

        replicateToClients();
    }

    private void replicateToClients() {
        List<NetworkProtocol.ReplicationUpdate> updates = new ArrayList<>();

        for (Actor actor : gameWorld.getAllActors()) {
            if (!actor.isReplicated()) continue;

            // position is now sent via MovementReplicationSystem (UDP)
            // scan each actor component for @Replicated property changes
            collectComponentUpdates(actor, updates);
        }

        if (updates.isEmpty()) return;

        for (int start = 0; start < updates.size(); start += MAX_UPDATES_PER_BATCH) {
            int end = Math.min(start + MAX_UPDATES_PER_BATCH, updates.size());
            NetworkProtocol.BatchReplicationUpdate batch = new NetworkProtocol.BatchReplicationUpdate();
            batch.updates = updates.subList(start, end).toArray(new NetworkProtocol.ReplicationUpdate[0]);
            batch.serverTick = tickCounter;
            batch.serverTime = tickCounter * replicationRate;
            netDriver.sendToAllClients(batch, true);
        }
    }

    private void collectComponentUpdates(Actor actor, List<NetworkProtocol.ReplicationUpdate> updates) {
        var entity = actor.getAshleyEntity();
        var components = entity.getComponents();

        for (var component : components) {
            if (!(component instanceof ActorComponent actorComp)) continue;
            if (!actorComp.isReplicated()) continue;

            ReplicationInfo info = ReplicationInfo.scan(component.getClass());
            if (!info.hasReplicatedProperties()) continue;

            Map<String, Object> dirty = info.collectDirty(component);
            if (dirty.isEmpty()) continue;

            NetworkProtocol.ReplicationUpdate update = new NetworkProtocol.ReplicationUpdate();
            update.actorId = actor.getActorId();
            update.componentClass = component.getClass().getName();
            update.properties = dirty;
            update.sequenceNumber = tickCounter;
            updates.add(update);

            // mark as clean after collecting
            info.markAllClean(component);
        }
    }

    public void setReplicationRate(float hz) {
        this.replicationRate = 1f / hz;
    }

    public int getTickCounter() { return tickCounter; }
}

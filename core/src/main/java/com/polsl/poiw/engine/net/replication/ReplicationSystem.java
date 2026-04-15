package com.polsl.poiw.engine.net.replication;

import com.badlogic.ashley.core.EntitySystem;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.ActorComponent;
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * replication system runs on the server every N ticks (set by setReplicationRate)
 * scans actors with replicated components, collect dirty properties
 * and sends BatchReplicationUpdate to clients
 */
public class ReplicationSystem extends EntitySystem {

    private static final String TAG = "ReplicationSystem";

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

            // always broadcast the position of each replicated actor
            var pos = actor.getPosition();
            NetworkProtocol.ReplicationUpdate posUpdate = new NetworkProtocol.ReplicationUpdate();
            posUpdate.actorId = actor.getActorId();
            posUpdate.componentClass = "_position";
            posUpdate.properties = new HashMap<>();
            posUpdate.properties.put("posX", pos.x);
            posUpdate.properties.put("posY", pos.y);
            posUpdate.sequenceNumber = tickCounter;
            updates.add(posUpdate);

            // scan each actor component
            collectComponentUpdates(actor, updates);
        }

        if (updates.isEmpty()) return;

        // send batch to all clients
        NetworkProtocol.BatchReplicationUpdate batch = new NetworkProtocol.BatchReplicationUpdate();
        batch.updates = updates.toArray(new NetworkProtocol.ReplicationUpdate[0]);
        batch.serverTick = tickCounter;
        batch.serverTime = tickCounter * replicationRate;

        netDriver.sendToAllClients(batch, true); // TCP — reliable
    }

    private void collectComponentUpdates(Actor actor, List<NetworkProtocol.ReplicationUpdate> updates) {
        // iterate over actor components — need access to AbstractActor
        if (!(actor instanceof com.polsl.poiw.engine.actor.AbstractActor abstractActor)) return;

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

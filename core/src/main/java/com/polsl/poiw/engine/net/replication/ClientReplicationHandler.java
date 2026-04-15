package com.polsl.poiw.engine.net.replication;

import com.badlogic.gdx.Gdx;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.ActorComponent;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.net.prediction.EntityInterpolation;
import com.polsl.poiw.engine.net.prediction.InterpolationSystem;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.Map;

/**
 * client-side replication handler
 * parse ActorSpawn, ActorDestroy, BatchReplicationUpdate messages
 */
public class ClientReplicationHandler {

    private static final String TAG = "ClientReplicationHandler";

    private final GameWorld gameWorld;
    private final int localPlayerId;

    /** Actor factory — creates and configures an actor based on the class name */
    private ActorFactory actorFactory;

    /** Interpolation system (client) — for feeding position snapshots */
    private InterpolationSystem interpolationSystem;

    public ClientReplicationHandler(GameWorld gameWorld, int localPlayerId) {
        this.gameWorld = gameWorld;
        this.localPlayerId = localPlayerId;
    }

    // processes actor spawn from the server.
    public void handleActorSpawn(NetworkProtocol.ActorSpawn spawn) {
        Gdx.app.log(TAG, "handleActorSpawn: actorId=" + spawn.actorId
            + " class=" + spawn.actorClass
            + " owner=" + spawn.ownerId
            + " localPlayer=" + localPlayerId
            + " pos=(" + spawn.x + "," + spawn.y + ")");
        // check if exists
        if (gameWorld.getActorById(spawn.actorId) != null) {
            Gdx.app.debug(TAG, "Actor " + spawn.actorId + " already exists, skipping spawn");
            return;
        }

        // create actor
        AbstractActor actor;
        if (actorFactory != null) {
            actor = actorFactory.createActor(spawn.actorClass, spawn.initialProperties);
        } else {
            actor = createActorByClassName(spawn.actorClass);
        }

        if (actor == null) {
            Gdx.app.error(TAG, "Nie można stworzyć aktora: " + spawn.actorClass);
            return;
        }

        // set NetRole based on ownerId
        if (spawn.ownerId == localPlayerId && spawn.ownerId >= 0) {
            actor.setNetRole(NetRole.AUTONOMOUS_PROXY);
        } else {
            actor.setNetRole(NetRole.SIMULATED_PROXY);
        }
        actor.setOwnerId(spawn.ownerId);
        actor.setReplicated(true);

        // spawn id from server
        gameWorld.spawnActorWithId(actor, spawn.actorId,
            new com.badlogic.gdx.math.Vector2(spawn.x, spawn.y));

        Gdx.app.debug(TAG, "Spawned actor " + spawn.actorId + " (" + spawn.actorClass
            + ") role=" + actor.getNetRole());
    }

    public void handleActorDestroy(NetworkProtocol.ActorDestroy destroy) {
        gameWorld.destroyActorById(destroy.actorId);
        Gdx.app.debug(TAG, "Destroyed actor " + destroy.actorId);
    }

    public void handleBatchUpdate(NetworkProtocol.BatchReplicationUpdate batch) {
        if (batch.updates == null) return;

        Gdx.app.debug(TAG, "handleBatchUpdate: " + batch.updates.length
            + " updates, serverTime=" + batch.serverTime + " tick=" + batch.serverTick);

        for (NetworkProtocol.ReplicationUpdate update : batch.updates) {
            handleReplicationUpdate(update, batch.serverTime);
        }
    }

    private void handleReplicationUpdate(NetworkProtocol.ReplicationUpdate update, float serverTime) {
        Actor actor = gameWorld.getActorById(update.actorId);
        if (actor == null) return;

        // hande position updates broadcast by server (synthetic componentClass="_position")
        if ("_position".equals(update.componentClass)) {
            Float x = getFloat(update.properties, "posX");
            Float y = getFloat(update.properties, "posY");
            if (x != null && y != null) {
                if (actor.getNetRole() == NetRole.SIMULATED_PROXY) {
                    // feed interpolation system
                    if (interpolationSystem != null) {
                        interpolationSystem.addSnapshot(actor.getActorId(), serverTime, x, y);
                        interpolationSystem.setServerTime(serverTime);
                    } else {
                        // fallback: directly set position
                        actor.setPosition(x, y);
                    }
                } else {
                    // AUTONOMOUS_PROXY — ignore, client has client prediction
                    Gdx.app.debug(TAG, "Ignoring position update for actor " + actor.getActorId()
                        + " because it's " + actor.getNetRole());
                }
            }
            return;
        }

        // find component by class name
        var entity = actor.getAshleyEntity();
        for (var component : entity.getComponents()) {
            if (component.getClass().getName().equals(update.componentClass)) {
                // apply properties
                ReplicationInfo info = ReplicationInfo.scan(component.getClass());
                info.apply(component, update.properties);
                break;
            }
        }
    }

    private Float getFloat(Map<String, Object> props, String key) {
        Object val = props.get(key);
        if (val instanceof Float f) return f;
        if (val instanceof Number n) return n.floatValue();
        return null;
    }

    private AbstractActor createActorByClassName(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            return (AbstractActor) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Gdx.app.error(TAG, "Cannot create actor: " + className, e);
            return null;
        }
    }

    public void setActorFactory(ActorFactory factory) {
        this.actorFactory = factory;
    }

    public ActorFactory getActorFactory() {
        return actorFactory;
    }

    public void setInterpolationSystem(InterpolationSystem system) {
        this.interpolationSystem = system;
    }

    public InterpolationSystem getInterpolationSystem() {
        return interpolationSystem;
    }

    // actor factory interface
    // allows game code to define how to create and configure actors on the client
    public interface ActorFactory {
        AbstractActor createActor(String actorClass, Map<String, Object> initialProperties);
    }
}

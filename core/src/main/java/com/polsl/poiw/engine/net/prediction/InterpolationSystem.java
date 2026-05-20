package com.polsl.poiw.engine.net.prediction;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.component.KnockbackComponent;
import com.polsl.poiw.engine.component.PickupCollectAnimationComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;

import java.util.HashMap;
import java.util.Map;

/**
 * interpolation system on client - processes SIMULATED_PROXY entities
 * sets position from EntityInterpolation (100ms in the past)
 * DO NOT add on server.
 */
public class InterpolationSystem extends IteratingSystem {

    // actorId -> EntityInterpolation
    private final Map<Integer, EntityInterpolation> interpolators = new HashMap<>();
    private NetworkClock networkClock;

    public InterpolationSystem() {
        super(Family.all(TransformComponent.class).get(), 15);
    }

    public void setNetworkClock(NetworkClock clock) {
        this.networkClock = clock;
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        if (networkClock == null || !networkClock.isInitialized()) return;

        TransformComponent transform = TransformComponent.MAPPER.get(entity);
        Actor owner = transform.getOwner();
        if (owner == null || owner.getNetRole() != NetRole.SIMULATED_PROXY) return;
        if (owner instanceof ItemPickupActor) return;

        EntityInterpolation interp = interpolators.get(owner.getActorId());
        if (interp == null) return;

        KnockbackComponent knockback = KnockbackComponent.MAPPER.get(entity);
        PickupCollectAnimationComponent collectAnimation = owner.getComponent(PickupCollectAnimationComponent.class);
        boolean allowExtrapolation = (knockback == null || !knockback.isActive())
            && (collectAnimation == null || !collectAnimation.isCollecting());
        Vector2 pos = interp.interpolate(networkClock.getRenderTime(), allowExtrapolation);
        if (pos != null) {
            // snapshots contain body-center position; TransformComponent stores bottom-left
            Vector2 size = transform.getSize();
            transform.getPosition().set(pos.x - size.x * 0.5f, pos.y - size.y * 0.5f);
        }
    }

    // adds pos snapshot for an actor (called from network message handler)
    public void addSnapshot(int actorId, float serverTime, float x, float y) {
        addSnapshot(actorId, serverTime, x, y, 0f, 0f);
    }

    // adds pos + velocity snapshot for an actor (movement replication)
    public void addSnapshot(int actorId, float serverTime, float x, float y, float velX, float velY) {
        EntityInterpolation interp = interpolators.computeIfAbsent(actorId, id -> new EntityInterpolation());
        interp.addSnapshot(serverTime, x, y, velX, velY);
    }

    // clears interpolation data for a destroyed actor
    public void removeInterpolator(int actorId) {
        interpolators.remove(actorId);
    }
    
    public void clear() {
        interpolators.clear();
    }
}

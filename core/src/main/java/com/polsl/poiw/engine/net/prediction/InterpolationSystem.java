package com.polsl.poiw.engine.net.prediction;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.component.TransformComponent;

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
    private float serverTime = 0f;

    public InterpolationSystem() {
        super(Family.all(TransformComponent.class).get(), 15);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent transform = TransformComponent.MAPPER.get(entity);
        Actor owner = transform.getOwner();
        if (owner == null || owner.getNetRole() != NetRole.SIMULATED_PROXY) return;

        EntityInterpolation interp = interpolators.get(owner.getActorId());
        if (interp == null) return;

        Vector2 pos = interp.interpolate(serverTime);
        if (pos != null) {
            // pos is already the sprite position (left-bottom), same as TransformComponent.position
            transform.getPosition().set(pos.x, pos.y);
        }
    }

    // adds pos snapshot for an actor (called from network message handler)
    public void addSnapshot(int actorId, float serverTime, float x, float y) {
        EntityInterpolation interp = interpolators.computeIfAbsent(actorId, id -> new EntityInterpolation());
        interp.addSnapshot(serverTime, x, y);
    }

    // updates server time (called every frame or per batch update)
    public void setServerTime(float time) {
        this.serverTime = time;
    }

    // clears interpolation data for a destroyed actor
    public void removeInterpolator(int actorId) {
        interpolators.remove(actorId);
    }

    public float getServerTime() { return serverTime; }
    
    public void clear() {
        interpolators.clear();
    }
}

package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.TransformComponent;

/**
 * System ruchu — przemieszcza entity na podstawie MovementComponent.direction × maxSpeed.
 *
 * Dla uproszczenia (bez Box2D physics body na graczu) stosuje bezpośredni ruch pozycji.
 * W przyszłości można zastąpić PhysicMoveSystem operującym na Body.setLinearVelocity().
 */
public class MovementSystem extends IteratingSystem {
    private static final Vector2 TMP = new Vector2();

    public MovementSystem() {
        super(Family.all(TransformComponent.class, MovementComponent.class).get(), 10);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        MovementComponent move = MovementComponent.MAPPER.get(entity);
        TransformComponent scene = TransformComponent.MAPPER.get(entity);

        if (move.isRooted() || move.getDirection().isZero()) return;

        TMP.set(move.getDirection()).nor();
        float speed = move.getMaxSpeed();
        Vector2 pos = scene.getPosition();
        pos.x += TMP.x * speed * deltaTime;
        pos.y += TMP.y * speed * deltaTime;
    }
}

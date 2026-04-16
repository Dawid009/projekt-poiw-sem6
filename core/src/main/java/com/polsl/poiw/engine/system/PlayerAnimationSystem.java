package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.PlayerAnimationComponent;
import com.polsl.poiw.engine.component.SpriteComponent;

/**
 * System animacji gracza — aktualizuje klatkę sprite'a na podstawie kierunku ruchu.
 */
public class PlayerAnimationSystem extends IteratingSystem {

    public PlayerAnimationSystem() {
        super(Family.all(SpriteComponent.class, MovementComponent.class, PlayerAnimationComponent.class).get(), 15);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        SpriteComponent sprite = SpriteComponent.MAPPER.get(entity);
        MovementComponent movement = MovementComponent.MAPPER.get(entity);
        PlayerAnimationComponent animation = PlayerAnimationComponent.MAPPER.get(entity);

        animation.update(movement.getDirection(), deltaTime);
        sprite.setRegion(animation.getCurrentFrame());
    }
}
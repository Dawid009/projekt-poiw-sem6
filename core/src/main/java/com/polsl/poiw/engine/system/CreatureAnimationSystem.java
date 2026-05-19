package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.polsl.poiw.engine.component.CreatureAnimationComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;

public class CreatureAnimationSystem extends IteratingSystem {

    public CreatureAnimationSystem() {
        super(Family.all(SpriteComponent.class, TransformComponent.class, MovementComponent.class, CreatureAnimationComponent.class).get(), 16);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        SpriteComponent sprite = SpriteComponent.MAPPER.get(entity);
        TransformComponent transform = TransformComponent.MAPPER.get(entity);
        MovementComponent movement = MovementComponent.MAPPER.get(entity);
        CreatureAnimationComponent animation = CreatureAnimationComponent.MAPPER.get(entity);
        DamageReactionComponent damageReaction = DamageReactionComponent.MAPPER.get(entity);

        if (damageReaction != null && damageReaction.consumeReactionTrigger()) {
            animation.triggerDamageFlash();
        }

        animation.tickDamageFlash(deltaTime);
        animation.update(movement.getDirection(), deltaTime);

        transform.getScaling().set(animation.shouldFlipHorizontally() ? -1f : 1f, 1f);
        sprite.getColor().set(animation.isDamageFlashActive() ? 1f : 1f,
            animation.isDamageFlashActive() ? 0.45f : 1f,
            animation.isDamageFlashActive() ? 0.45f : 1f,
            1f);
        sprite.setRegion(animation.getCurrentFrame());
    }
}
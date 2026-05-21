package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.component.CreatureAnimationComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;

/**
 * Steruje animacją stworzeń na podstawie ruchu i reakcji na trafienie.
 * Jeśli ciało fizyczne nadal się przesuwa, to ono ma pierwszeństwo przed lokalnym wektorem ruchu.
 */
public class CreatureAnimationSystem extends IteratingSystem {
    private static final float BODY_MOVE_EPSILON = 0.03f;

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
        Vector2 animationDirection = movement.getDirection();
        CollisionComponent collision = transform.getOwner() != null
            ? transform.getOwner().getComponentByType(CollisionComponent.class)
            : null;
        Body body = collision != null ? collision.getBody() : null;
        if (body != null && !body.getLinearVelocity().isZero(BODY_MOVE_EPSILON)) {
            animationDirection = body.getLinearVelocity();
        }
        animation.update(animationDirection, deltaTime);

        transform.getScaling().set(animation.shouldFlipHorizontally() ? -1f : 1f, 1f);
        sprite.getColor().set(animation.isDamageFlashActive() ? 1f : 1f,
            animation.isDamageFlashActive() ? 0.45f : 1f,
            animation.isDamageFlashActive() ? 0.45f : 1f,
            1f);
        sprite.setRegion(animation.getCurrentFrame());
    }
}
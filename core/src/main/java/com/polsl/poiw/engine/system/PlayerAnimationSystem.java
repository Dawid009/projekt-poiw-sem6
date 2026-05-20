package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.PlayerAnimationComponent;
import com.polsl.poiw.engine.component.PlayerToolComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

/**
 * System animacji gracza — aktualizuje klatkę sprite'a na podstawie kierunku ruchu.
 */
public class PlayerAnimationSystem extends IteratingSystem {
    private static final float BASE_PLAYER_FRAME_SIZE_PX = 32f;

    public PlayerAnimationSystem() {
        super(Family.all(SpriteComponent.class, TransformComponent.class, MovementComponent.class, PlayerAnimationComponent.class).get(), 15);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        SpriteComponent sprite = SpriteComponent.MAPPER.get(entity);
        TransformComponent transform = TransformComponent.MAPPER.get(entity);
        MovementComponent movement = MovementComponent.MAPPER.get(entity);
        PlayerAnimationComponent animation = PlayerAnimationComponent.MAPPER.get(entity);
        CombatComponent combat = CombatComponent.MAPPER.get(entity);
        DamageReactionComponent damageReaction = DamageReactionComponent.MAPPER.get(entity);
        PlayerToolComponent toolComponent = PlayerToolComponent.MAPPER.get(entity);
        PlayerToolType toolType = toolComponent != null ? toolComponent.getActiveTool() : PlayerToolType.SWORD;
        TextureRegion currentFrame;

        if (damageReaction != null && damageReaction.consumeReactionTrigger()) {
            animation.triggerDamageFlash();
        }

        animation.tickDamageFlash(deltaTime);
        animation.tickAttackVisual(deltaTime);
        if (animation.isDamageFlashActive()) {
            sprite.getColor().set(1f, 0.45f, 0.45f, 1f);
        } else {
            sprite.getColor().set(1f, 1f, 1f, 1f);
        }

        if (combat == null) {
            animation.update(movement.getDirection(), deltaTime);
            currentFrame = animation.getCurrentFrame();
            sprite.setRegion(currentFrame);
            applyFrameScale(transform, currentFrame);
            return;
        }

        if (combat.consumeAttackTrigger()) {
            animation.startAttack(combat.getFacingDirection(), toolType);
        }

        boolean moving = movement != null
            && !movement.getDirection().isZero(0.001f)
            && !combat.isAttacking()
            && !animation.isAttackVisualActive();
        animation.applyState(combat.getFacingDirection(), moving, combat.isAttacking(), deltaTime);
        currentFrame = animation.getCurrentFrame();
        sprite.setRegion(currentFrame);
        applyFrameScale(transform, currentFrame);
    }

    private void applyFrameScale(TransformComponent transform, TextureRegion currentFrame) {
        if (transform == null || currentFrame == null) {
            return;
        }

        transform.setRotationOriginNormalized(0.5f, 0f);
        transform.getScaling().set(
            currentFrame.getRegionWidth() / BASE_PLAYER_FRAME_SIZE_PX,
            currentFrame.getRegionHeight() / BASE_PLAYER_FRAME_SIZE_PX
        );
        transform.setRenderOffset(
            0f,
            -Math.max(0f, currentFrame.getRegionHeight() - BASE_PLAYER_FRAME_SIZE_PX) * 0.5f * Main.UNIT_SCALE
        );
    }
}
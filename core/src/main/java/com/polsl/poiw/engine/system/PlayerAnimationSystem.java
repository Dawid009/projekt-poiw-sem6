package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.PlayerAnimationComponent;
import com.polsl.poiw.engine.component.PlayerToolComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

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
        CombatComponent combat = CombatComponent.MAPPER.get(entity);
        DamageReactionComponent damageReaction = DamageReactionComponent.MAPPER.get(entity);
        PlayerToolComponent toolComponent = PlayerToolComponent.MAPPER.get(entity);
        PlayerToolType toolType = toolComponent != null ? toolComponent.getActiveTool() : PlayerToolType.SWORD;

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
            sprite.setRegion(animation.getCurrentFrame());
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
        sprite.setRegion(animation.getCurrentFrame());
    }
}
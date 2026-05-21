package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.PlayerToolComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.gameplay.actor.AttackHitboxActor;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

/**
 * System walki melee. Odpala atak po Command.SELECT, blokuje ruch na czas animacji
 * i spawnuje krótko żyjący hitbox przed postacią.
 */
public class CombatSystem extends IteratingSystem {
    private static final float HORIZONTAL_ATTACK_REACH_OFFSET = 0.52f;
    private static final float VERTICAL_ATTACK_REACH_OFFSET = 0.65f;
    private static final Vector2 TMP_SPAWN = new Vector2();
    private static final Vector2 TMP_KNOCKBACK = new Vector2();

    public CombatSystem() {
        super(Family.all(CombatComponent.class, TransformComponent.class).get(), 6);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        CombatComponent combat = CombatComponent.MAPPER.get(entity);
        TransformComponent transform = TransformComponent.MAPPER.get(entity);
        MovementComponent movement = MovementComponent.MAPPER.get(entity);
        Actor owner = transform.getOwner();

        if (owner == null) {
            return;
        }

        if (movement != null) {
            combat.updateFacing(movement.getDirection());
        }

        boolean wasAttacking = combat.isAttacking();
        combat.tickTimers(deltaTime);

        if (movement != null) {
            if (combat.isAttacking()) {
                movement.setRooted(true);
            } else if (wasAttacking) {
                movement.setRooted(false);
            }
        }

        if (!combat.consumeAttackRequest() || !combat.canStartAttack()) {
            return;
        }

        if (!owner.hasAuthority() && !owner.isLocallyControlled()) {
            return;
        }

        combat.startAttack();
        if (movement != null) {
            movement.setRooted(true);
        }

        if (owner.hasAuthority() && owner.getWorld() != null) {
            spawnAttackHitbox(owner, transform, combat);
        }
    }

    private void spawnAttackHitbox(Actor owner, TransformComponent transform, CombatComponent combat) {
        float hitboxWidth = combat.getHitboxWidth();
        float hitboxHeight = combat.getHitboxHeight();
        float centerX = transform.getPosition().x + transform.getSize().x * 0.5f;
        float centerY = transform.getPosition().y + transform.getSize().y * 0.5f;
        float horizontalReach = Math.max(0f, combat.getAttackReach() - HORIZONTAL_ATTACK_REACH_OFFSET);
        float verticalReach = Math.max(0f, combat.getAttackReach() - VERTICAL_ATTACK_REACH_OFFSET);

        switch (combat.getFacingDirection()) {
            case DOWN -> centerY -= verticalReach;
            case UP -> centerY += verticalReach;
            case LEFT -> centerX -= horizontalReach;
            case RIGHT -> centerX += horizontalReach;
        }

        AttackHitboxActor hitbox = new AttackHitboxActor();
        PlayerToolComponent toolComponent = owner.getComponent(PlayerToolComponent.class);
        PlayerToolType toolType = toolComponent != null ? toolComponent.getActiveTool() : PlayerToolType.SWORD;
        TMP_KNOCKBACK.set(switch (combat.getFacingDirection()) {
            case DOWN -> new Vector2(0f, -1f);
            case UP -> new Vector2(0f, 1f);
            case LEFT -> new Vector2(-1f, 0f);
            case RIGHT -> new Vector2(1f, 0f);
        });
        hitbox.configure(owner.getActorId(), combat.rollDamage(), toolType, hitboxWidth, hitboxHeight,
            TMP_KNOCKBACK, combat.getHitboxLifetime());
        hitbox.setOwnerId(owner.getOwnerId());

        TMP_SPAWN.set(centerX - hitboxWidth * 0.5f, centerY - hitboxHeight * 0.5f);
        owner.getWorld().spawnActor(hitbox, TMP_SPAWN);
    }
}

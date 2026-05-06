package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.gameplay.actor.AttackHitboxActor;

/**
 * System walki melee. Odpala atak po Command.SELECT, blokuje ruch na czas animacji
 * i spawnuje krótko żyjący hitbox przed postacią.
 */
public class CombatSystem extends IteratingSystem {
    private static final Vector2 TMP_SPAWN = new Vector2();

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

        CollisionComponent collision = owner.getComponentByType(CollisionComponent.class);
        if (collision != null && collision.getBody() != null) {
            centerX = collision.getBody().getPosition().x;
            centerY = collision.getBody().getPosition().y;
        }

        switch (combat.getFacingDirection()) {
            case DOWN -> centerY -= combat.getAttackReach();
            case UP -> centerY += combat.getAttackReach();
            case LEFT -> centerX -= combat.getAttackReach();
            case RIGHT -> centerX += combat.getAttackReach();
        }

        AttackHitboxActor hitbox = new AttackHitboxActor();
        hitbox.configure(owner.getActorId(), combat.rollDamage(), hitboxWidth, hitboxHeight, combat.getHitboxLifetime());

        TMP_SPAWN.set(centerX - hitboxWidth * 0.5f, centerY - hitboxHeight * 0.5f);
        owner.getWorld().spawnActor(hitbox, TMP_SPAWN);
    }
}
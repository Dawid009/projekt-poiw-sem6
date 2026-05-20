package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.collision.CollisionResult;
import com.polsl.poiw.engine.collision.OverlapListener;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.TransformComponent;

import java.util.HashSet;
import java.util.Set;

/**
 * Krótkotrwały sensor melee. Zadaje obrażenia raz na cel i znika po krótkim czasie.
 */
public class AttackHitboxActor extends AbstractActor implements OverlapListener {
    private static final float KNOCKBACK_STRENGTH = 2.8f;

    private final Set<Integer> hitActorIds = new HashSet<>();
    private final Vector2 knockbackDirection = new Vector2();

    private int instigatorActorId;
    private int damage;

    public void configure(int instigatorActorId,
                          int damage,
                          float width,
                          float height,
                          Vector2 knockbackDirection,
                          float lifeSpanSeconds) {
        this.instigatorActorId = instigatorActorId;
        this.damage = damage;
        this.knockbackDirection.set(knockbackDirection != null ? knockbackDirection : Vector2.Zero);

        addComponent(new TransformComponent(
            new Vector2(),
            0,
            new Vector2(width, height)
        ));

        BoxCollisionComponent collision = new BoxCollisionComponent(
            CollisionProfile.TRIGGER,
            width * 0.5f,
            height * 0.5f
        );
        collision.setBodyTypeOverride(BodyDef.BodyType.DynamicBody);
        collision.addOverlapListener(this);
        addComponent(collision);

        setLifeSpan(lifeSpanSeconds);
    }

    @Override
    public void onBeginOverlap(Actor self, Actor other, CollisionResult result) {
        if (!hasAuthority()) {
            return;
        }
        if (other.getActorId() == instigatorActorId) {
            return;
        }
        if (!hitActorIds.add(other.getActorId())) {
            return;
        }

        HealthComponent health = other.getComponent(HealthComponent.class);
        if (health == null || !health.isAlive()) {
            return;
        }

        health.applyDamage(damage, getOwnerId());

        DamageReactionComponent damageReaction = other.getComponent(DamageReactionComponent.class);
        if (damageReaction != null) {
            damageReaction.triggerReaction();
        }

        com.polsl.poiw.engine.component.KnockbackComponent knockback =
            other.getComponent(com.polsl.poiw.engine.component.KnockbackComponent.class);
        if (knockback != null) {
            knockback.apply(knockbackDirection, KNOCKBACK_STRENGTH);
        }
    }

    @Override
    public void onEndOverlap(Actor self, Actor other) {
    }
}
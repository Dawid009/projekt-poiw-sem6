package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.CreatureAnimationComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.KnockbackComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.auth.GameplayStatsBridge;
import com.polsl.poiw.engine.inventory.ItemDefinition;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractCreatureActor extends AbstractActor {
    public static final float DEFAULT_MAX_HEALTH = 50f;

    public static final String PROP_SIZE_W = "sizeW";
    public static final String PROP_SIZE_H = "sizeH";
    public static final String PROP_COLL_HALF_W = "collHalfW";
    public static final String PROP_COLL_HALF_H = "collHalfH";
    public static final String PROP_COLL_OFFSET_X = "collOffsetX";
    public static final String PROP_COLL_OFFSET_Y = "collOffsetY";
    public static final String PROP_SORT_OFFSET_Y = "sortOffsetY";
    public static final String PROP_Z_ORDER = "zOrder";
    public static final String PROP_MAX_HEALTH = "maxHealth";
    public static final String PROP_CURRENT_HEALTH = "currentHealth";

    private static final float MIN_IDLE_DURATION = 2.5f;
    private static final float MAX_IDLE_DURATION = 6.0f;
    private static final float MIN_WANDER_DISTANCE = 0.6f;
    private static final int MAX_TARGET_ATTEMPTS = 8;
    private static final float TARGET_REACHED_DISTANCE = 0.18f;
    private static final float STUCK_VELOCITY_EPSILON = 0.01f;
    private static final float STUCK_TIMEOUT = 0.2f;

    private final Vector2 homePosition = new Vector2();
    private final Vector2 targetPosition = new Vector2();
    private boolean homeInitialized;
    private boolean hasTarget;
    private boolean deathHandled;
    private boolean deathStatsHandled;
    private float idleRemaining;
    private float movementAttemptTimer;

    public void configure(TextureAtlas atlas,
                          float sizeW,
                          float sizeH,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          float sortOffsetY,
                          int zOrder,
                          float maxHealth,
                          float currentHealth) {
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder, maxHealth, currentHealth);

        TextureRegion idleFrame = atlas.findRegion(getIdleRegionName());
        if (idleFrame == null) {
            throw new IllegalArgumentException("Nie znaleziono regionu: " + getIdleRegionName());
        }

        addComponent(new SpriteComponent(idleFrame, Color.WHITE.cpy()));
        addComponent(new CreatureAnimationComponent(
            atlas,
            getIdleRegionName(),
            getWalkRegionName(),
            usesRightFacingSourceFrames()
        ));
    }

    public void configureServer(float sizeW,
                                float sizeH,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                float sortOffsetY,
                                int zOrder,
                                float maxHealth,
                                float currentHealth) {
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder, maxHealth, currentHealth);
    }

    public void configureFromReplication(TextureAtlas atlas, Map<String, Object> initialProperties) {
        float sizeW = getFloat(initialProperties, PROP_SIZE_W, 2f);
        float sizeH = getFloat(initialProperties, PROP_SIZE_H, 2f);
        float collHalfW = getFloat(initialProperties, PROP_COLL_HALF_W, 0.4f);
        float collHalfH = getFloat(initialProperties, PROP_COLL_HALF_H, 0.12f);
        float collOffsetX = getFloat(initialProperties, PROP_COLL_OFFSET_X, 0f);
        float collOffsetY = getFloat(initialProperties, PROP_COLL_OFFSET_Y, 0f);
        float sortOffsetY = getFloat(initialProperties, PROP_SORT_OFFSET_Y, 0f);
        int zOrder = getInt(initialProperties, PROP_Z_ORDER, 1);
        float maxHealth = getFloat(initialProperties, PROP_MAX_HEALTH, DEFAULT_MAX_HEALTH);
        float currentHealth = getFloat(initialProperties, PROP_CURRENT_HEALTH, maxHealth);

        configure(atlas, sizeW, sizeH, collHalfW, collHalfH,
            new Vector2(collOffsetX, collOffsetY), sortOffsetY, zOrder, maxHealth, currentHealth);
    }

    public Map<String, Object> buildInitialReplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        TransformComponent transform = getComponent(TransformComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        HealthComponent health = getComponent(HealthComponent.class);

        if (transform != null) {
            properties.put(PROP_SIZE_W, transform.getSize().x);
            properties.put(PROP_SIZE_H, transform.getSize().y);
            properties.put(PROP_SORT_OFFSET_Y, transform.getSortOffsetY());
            properties.put(PROP_Z_ORDER, transform.getZOrder());
        }
        if (collision != null) {
            properties.put(PROP_COLL_HALF_W, collision.getHalfWidth());
            properties.put(PROP_COLL_HALF_H, collision.getHalfHeight());
            properties.put(PROP_COLL_OFFSET_X, collision.getOffset().x);
            properties.put(PROP_COLL_OFFSET_Y, collision.getOffset().y);
        }
        if (health != null) {
            properties.put(PROP_MAX_HEALTH, health.getMaxHealth());
            properties.put(PROP_CURRENT_HEALTH, health.getCurrentHealth());
        }

        return properties;
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null && !health.isAlive()) {
            if (!deathStatsHandled) {
                deathStatsHandled = true;
                onDeathObserved();
            }
            if (hasAuthority() && !deathHandled) {
                deathHandled = true;
                onBeforeDestroy();
            }
            if (getWorld() != null) {
                getWorld().destroyActor(this);
            }
            return;
        }

        if (!hasAuthority()) {
            return;
        }

        updateWander(delta);
    }

    protected float getMoveSpeed() {
        return 1.1f;
    }

    protected float getWanderRadius() {
        return 3f;
    }

    protected abstract String getIdleRegionName();

    protected abstract String getWalkRegionName();

    protected boolean usesRightFacingSourceFrames() {
        return false;
    }

    protected void onBeforeDestroy() {
    }

    protected void onDeathObserved() {
    }

    protected final void spawnItemDrops(ItemDefinition itemDefinition, int minCount, int maxCount) {
        if (getWorld() == null || itemDefinition == null || maxCount <= 0) {
            return;
        }

        int dropCount = MathUtils.random(Math.max(0, minCount), Math.max(minCount, maxCount));
        if (dropCount <= 0) {
            return;
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        Vector2 position = transform != null ? transform.getPosition() : getPosition();
        Vector2 size = transform != null ? transform.getSize() : new Vector2(1f, 1f);
        float centerX = position.x + size.x * 0.5f;
        float centerY = position.y + Math.min(size.y * 0.3f, 0.8f);

        for (int index = 0; index < dropCount; index++) {
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float radius = MathUtils.random(0.2f, 0.55f);
            Vector2 dropPosition = new Vector2(
                centerX + MathUtils.cos(angle) * radius - 0.25f,
                centerY + MathUtils.sin(angle) * radius - 0.25f
            );

            ItemPickupActor pickupActor = new ItemPickupActor();
            if (isReplicated()) {
                pickupActor.configureServer(itemDefinition, 1);
                pickupActor.setReplicated(true);
            } else {
                pickupActor.configure(itemDefinition, 1);
            }
            getWorld().spawnActor(pickupActor, dropPosition);
        }
    }

    private void addSharedComponents(float sizeW,
                                     float sizeH,
                                     float collHalfW,
                                     float collHalfH,
                                     Vector2 collOffset,
                                     float sortOffsetY,
                                     int zOrder,
                                     float maxHealth,
                                     float currentHealth) {
        addComponent(new TransformComponent(
            new Vector2(),
            zOrder,
            new Vector2(sizeW, sizeH),
            new Vector2(1f, 1f),
            0f,
            sortOffsetY
        ));
        addComponent(new MovementComponent(getMoveSpeed()));
        addComponent(new KnockbackComponent());
        addComponent(new HealthComponent(maxHealth, currentHealth));
        addComponent(new DamageReactionComponent());
        addComponent(CombatComponent.createPassiveTarget());
        addComponent(new BoxCollisionComponent(CollisionProfile.ENEMY, collHalfW, collHalfH, collOffset));
    }

    private void updateWander(float delta) {
        MovementComponent movement = getComponent(MovementComponent.class);
        if (movement == null) {
            return;
        }

        KnockbackComponent knockback = getComponent(KnockbackComponent.class);
        if (knockback != null && knockback.isActive()) {
            movement.getDirection().setZero();
            movementAttemptTimer = 0f;
            return;
        }

        Vector2 currentPosition = getNavigationPosition();
        if (!homeInitialized) {
            homePosition.set(currentPosition);
            homeInitialized = true;
            enterIdle(movement);
            return;
        }

        if (!hasTarget) {
            movement.getDirection().setZero();
            idleRemaining = Math.max(0f, idleRemaining - delta);
            if (idleRemaining > 0f) {
                return;
            }

            if (!chooseNewTarget(currentPosition)) {
                enterIdle(movement);
                return;
            }
        }

        float targetReachedDistance2 = TARGET_REACHED_DISTANCE * TARGET_REACHED_DISTANCE;
        if (currentPosition.dst2(targetPosition) <= targetReachedDistance2) {
            enterIdle(movement);
            return;
        }

        movement.getDirection().set(targetPosition).sub(currentPosition);
        if (movement.getDirection().isZero(0.001f)) {
            enterIdle(movement);
            return;
        }
        movement.getDirection().nor();
        movementAttemptTimer += delta;

        CollisionComponent collision = getComponentByType(CollisionComponent.class);
        Body body = collision != null ? collision.getBody() : null;
        if (body == null) {
            return;
        }

        if (movementAttemptTimer >= STUCK_TIMEOUT
            && body.getLinearVelocity().len2() <= STUCK_VELOCITY_EPSILON
            && currentPosition.dst2(targetPosition) > targetReachedDistance2) {
            enterIdle(movement);
        }
    }

    private void enterIdle(MovementComponent movement) {
        hasTarget = false;
        movementAttemptTimer = 0f;
        idleRemaining = MathUtils.random(MIN_IDLE_DURATION, MAX_IDLE_DURATION);
        movement.getDirection().setZero();
    }

    private boolean chooseNewTarget(Vector2 currentPosition) {
        movementAttemptTimer = 0f;
        idleRemaining = 0f;

        float minDistance2 = MIN_WANDER_DISTANCE * MIN_WANDER_DISTANCE;
        for (int attempt = 0; attempt < MAX_TARGET_ATTEMPTS; attempt++) {
            float radius = MathUtils.random(MIN_WANDER_DISTANCE, getWanderRadius());
            float angle = MathUtils.random(0f, MathUtils.PI2);
            targetPosition.set(homePosition.x + MathUtils.cos(angle) * radius,
                homePosition.y + MathUtils.sin(angle) * radius);

            if (currentPosition.dst2(targetPosition) >= minDistance2) {
                hasTarget = true;
                return true;
            }
        }

        hasTarget = false;
        return false;
    }

    private Vector2 getNavigationPosition() {
        CollisionComponent collision = getComponentByType(CollisionComponent.class);
        if (collision != null && collision.getBody() != null) {
            return collision.getBody().getPosition();
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform == null) {
            return getPosition();
        }

        return new Vector2(transform.getPosition()).add(transform.getSize().x * 0.5f, transform.getSize().y * 0.5f);
    }

    private float getFloat(Map<String, Object> properties, String key, float defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> properties, String key, int defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
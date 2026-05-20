package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;

import java.util.HashMap;
import java.util.Map;

/**
 * Statyczny cel treningowy z HP i prostym feedbackiem trafienia.
 */
public class TrainingDummyActor extends AbstractActor {
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

    private static final String IDLE_REGION = "training_dummy/idle_down";
    private static final String DAMAGED_REGION = "training_dummy/damaged_down";
    private static final float DAMAGE_FRAME_DURATION = 0.05f;

    private TextureRegion idleFrame;
    private Animation<TextureRegion> damagedAnimation;
    private boolean damageFeedbackActive;
    private float damageStateTime;

    public void configure(TextureAtlas atlas,
                          float sizeW,
                          float sizeH,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          float sortOffsetY,
                          int zOrder,
                          float maxHealth) {
        configure(atlas, sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder, maxHealth, maxHealth);
    }

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
        idleFrame = atlas.findRegion(IDLE_REGION);
        if (idleFrame == null) {
            throw new IllegalArgumentException("Nie znaleziono regionu: " + IDLE_REGION);
        }

        var damagedFrames = atlas.findRegions(DAMAGED_REGION);
        if (damagedFrames == null || damagedFrames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek animacji: " + DAMAGED_REGION);
        }
        damagedAnimation = new Animation<>(DAMAGE_FRAME_DURATION, damagedFrames, Animation.PlayMode.NORMAL);

        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder, maxHealth, currentHealth);
        addComponent(new SpriteComponent(idleFrame, Color.WHITE.cpy()));
    }

    public void configureServer(float sizeW,
                                float sizeH,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                float sortOffsetY,
                                int zOrder,
                                float maxHealth) {
        configureServer(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder, maxHealth, maxHealth);
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
        float collHalfW = getFloat(initialProperties, PROP_COLL_HALF_W, 0.3f);
        float collHalfH = getFloat(initialProperties, PROP_COLL_HALF_H, 0.22f);
        float collOffsetX = getFloat(initialProperties, PROP_COLL_OFFSET_X, 0f);
        float collOffsetY = getFloat(initialProperties, PROP_COLL_OFFSET_Y, 0f);
        float sortOffsetY = getFloat(initialProperties, PROP_SORT_OFFSET_Y, 0f);
        int zOrder = getInt(initialProperties, PROP_Z_ORDER, 1);
        float maxHealth = getFloat(initialProperties, PROP_MAX_HEALTH, 100f);
        float currentHealth = getFloat(initialProperties, PROP_CURRENT_HEALTH, maxHealth);

        configure(
            atlas,
            sizeW,
            sizeH,
            collHalfW,
            collHalfH,
            new Vector2(collOffsetX, collOffsetY),
            sortOffsetY,
            zOrder,
            maxHealth,
            currentHealth
        );
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
        addComponent(new HealthComponent(maxHealth, currentHealth));
        addComponent(CombatComponent.createPassiveTarget());
        addComponent(new DamageReactionComponent());

        BoxCollisionComponent collision = new BoxCollisionComponent(
            CollisionProfile.ENEMY,
            collHalfW,
            collHalfH,
            collOffset
        );
        collision.setBodyTypeOverride(BodyDef.BodyType.StaticBody);
        addComponent(collision);
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null && !health.isAlive() && getWorld() != null) {
            getWorld().destroyActor(this);
        }

        DamageReactionComponent damageReaction = getComponent(DamageReactionComponent.class);
        if (damageReaction != null && damageReaction.consumeReactionTrigger()) {
            damageFeedbackActive = true;
            damageStateTime = 0f;
        }

        SpriteComponent sprite = getComponent(SpriteComponent.class);
        if (sprite == null) {
            return;
        }

        if (damageFeedbackActive) {
            damageStateTime += delta;
            if (damagedAnimation.isAnimationFinished(damageStateTime)) {
                damageFeedbackActive = false;
                damageStateTime = 0f;
                sprite.setRegion(idleFrame);
            } else {
                sprite.setRegion(damagedAnimation.getKeyFrame(damageStateTime));
            }
        } else {
            sprite.setRegion(idleFrame);
        }
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
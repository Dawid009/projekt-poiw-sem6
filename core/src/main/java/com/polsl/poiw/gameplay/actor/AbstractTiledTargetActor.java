package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TileVisualStateComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.inventory.ItemDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Wspólna baza dla pasywnych, atakowalnych obiektów z mapy Tiled.
 */
public abstract class AbstractTiledTargetActor extends AbstractActor {
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
    public static final String PROP_TILE_GID = "tileGid";

    private transient TiledMap tiledMap;
    private transient int appliedTileGid = Integer.MIN_VALUE;

    public void configure(TiledMap map,
                          int tileGid,
                          TextureRegion region,
                          float sizeW,
                          float sizeH,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          float sortOffsetY,
                          int zOrder,
                          float maxHealth,
                          float currentHealth) {
        this.tiledMap = map;
        addSharedComponents(tileGid, sizeW, sizeH, collHalfW, collHalfH, collOffset,
            sortOffsetY, zOrder, maxHealth, currentHealth);

        if (region != null) {
            addComponent(new SpriteComponent(region, Color.WHITE.cpy()));
            appliedTileGid = tileGid;
        }
    }

    public void configureServer(int tileGid,
                                float sizeW,
                                float sizeH,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                float sortOffsetY,
                                int zOrder,
                                float maxHealth,
                                float currentHealth) {
        addSharedComponents(tileGid, sizeW, sizeH, collHalfW, collHalfH, collOffset,
            sortOffsetY, zOrder, maxHealth, currentHealth);
    }

    public void configureFromReplication(TiledMap map, Map<String, Object> initialProperties) {
        int tileGid = getInt(initialProperties, PROP_TILE_GID, 0);
        float sizeW = getFloat(initialProperties, PROP_SIZE_W, 1f);
        float sizeH = getFloat(initialProperties, PROP_SIZE_H, 1f);
        float collHalfW = getFloat(initialProperties, PROP_COLL_HALF_W, 0.25f);
        float collHalfH = getFloat(initialProperties, PROP_COLL_HALF_H, 0.25f);
        float collOffsetX = getFloat(initialProperties, PROP_COLL_OFFSET_X, 0f);
        float collOffsetY = getFloat(initialProperties, PROP_COLL_OFFSET_Y, 0f);
        float sortOffsetY = getFloat(initialProperties, PROP_SORT_OFFSET_Y, 0f);
        int zOrder = getInt(initialProperties, PROP_Z_ORDER, 1);
        float maxHealth = getFloat(initialProperties, PROP_MAX_HEALTH, 1f);
        float currentHealth = getFloat(initialProperties, PROP_CURRENT_HEALTH, maxHealth);

        configure(
            map,
            tileGid,
            resolveTileRegion(map, tileGid),
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
        TileVisualStateComponent tileVisual = getComponent(TileVisualStateComponent.class);

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
        if (tileVisual != null) {
            properties.put(PROP_TILE_GID, tileVisual.getTileGid());
        }

        return properties;
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null && !health.isAlive()) {
            if (hasAuthority()) {
                onBeforeDestroy();
            }
            if (getWorld() != null) {
                getWorld().destroyActor(this);
            }
            return;
        }

        syncSpriteRegion();
    }

    protected void onBeforeDestroy() {
    }

    protected final TiledMap getTiledMap() {
        return tiledMap;
    }

    protected final void setTileGid(int tileGid) {
        TileVisualStateComponent tileVisual = getComponent(TileVisualStateComponent.class);
        if (tileVisual == null) {
            return;
        }

        tileVisual.setTileGid(tileGid);
        syncSpriteRegion();
    }

    protected final int getTileGid() {
        TileVisualStateComponent tileVisual = getComponent(TileVisualStateComponent.class);
        return tileVisual != null ? tileVisual.getTileGid() : 0;
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

    protected final void spawnVisualDecoration(int tileGid, float width, float height) {
        spawnVisualDecoration(tileGid, width, height, 0f, 0f, null);
    }

    protected final void spawnVisualDecoration(int tileGid,
                                               float width,
                                               float height,
                                               float collHalfW,
                                               float collHalfH,
                                               Vector2 collOffset) {
        if (getWorld() == null || tileGid <= 0) {
            return;
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform == null) {
            return;
        }

        float spawnX = transform.getPosition().x + (transform.getSize().x - width) * 0.5f;
        float spawnY = transform.getPosition().y;

        TiledVisualActor decoration = new TiledVisualActor();
        if (isReplicated()) {
            decoration.configureServer(tileGid, width, height, 0f, transform.getZOrder(),
                collHalfW, collHalfH, collOffset);
            decoration.setReplicated(true);
        } else {
            decoration.configure(getTiledMap(), tileGid, resolveTileRegion(getTiledMap(), tileGid),
                width, height, 0f, transform.getZOrder(), collHalfW, collHalfH, collOffset);
        }
        getWorld().spawnActor(decoration, new Vector2(spawnX, spawnY));
    }

    protected final TextureRegion resolveTileRegion(TiledMap map, int tileGid) {
        if (map == null || tileGid <= 0) {
            return null;
        }

        TiledMapTile tile = map.getTileSets().getTile(tileGid);
        return tile != null ? tile.getTextureRegion() : null;
    }

    private void syncSpriteRegion() {
        SpriteComponent sprite = getComponent(SpriteComponent.class);
        TileVisualStateComponent tileVisual = getComponent(TileVisualStateComponent.class);
        if (sprite == null || tileVisual == null) {
            return;
        }

        int tileGid = tileVisual.getTileGid();
        if (tileGid == appliedTileGid) {
            return;
        }

        TextureRegion region = resolveTileRegion(tiledMap, tileGid);
        if (region == null) {
            return;
        }

        sprite.setRegion(region);
        appliedTileGid = tileGid;
    }

    private void addSharedComponents(int tileGid,
                                     float sizeW,
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
        addComponent(new DamageReactionComponent());
        addComponent(CombatComponent.createPassiveTarget());
        addComponent(new TileVisualStateComponent(tileGid));

        if (collHalfW > 0f && collHalfH > 0f) {
            BoxCollisionComponent collision = new BoxCollisionComponent(
                CollisionProfile.ENEMY,
                collHalfW,
                collHalfH,
                collOffset
            );
            collision.setBodyTypeOverride(BodyDef.BodyType.StaticBody);
            addComponent(collision);
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
package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TileVisualStateComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.save.SaveGameData;

import java.util.HashMap;
import java.util.Map;

public class TiledVisualActor extends AbstractActor {
    public static final String PROP_SIZE_W = "sizeW";
    public static final String PROP_SIZE_H = "sizeH";
    public static final String PROP_SORT_OFFSET_Y = "sortOffsetY";
    public static final String PROP_Z_ORDER = "zOrder";
    public static final String PROP_TILE_GID = "tileGid";
    public static final String PROP_COLL_HALF_W = "collHalfW";
    public static final String PROP_COLL_HALF_H = "collHalfH";
    public static final String PROP_COLL_OFFSET_X = "collOffsetX";
    public static final String PROP_COLL_OFFSET_Y = "collOffsetY";

    private transient TiledMap tiledMap;
    private transient int appliedTileGid = Integer.MIN_VALUE;

    public void configure(TiledMap map,
                          int tileGid,
                          TextureRegion region,
                          float sizeW,
                          float sizeH,
                          float sortOffsetY,
                          int zOrder) {
        configure(map, tileGid, region, sizeW, sizeH, sortOffsetY, zOrder, 0f, 0f, new Vector2());
    }

    public void configure(TiledMap map,
                          int tileGid,
                          TextureRegion region,
                          float sizeW,
                          float sizeH,
                          float sortOffsetY,
                          int zOrder,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset) {
        this.tiledMap = map;
        addSharedComponents(tileGid, sizeW, sizeH, sortOffsetY, zOrder, collHalfW, collHalfH, collOffset);
        if (region != null) {
            addComponent(new SpriteComponent(region, Color.WHITE.cpy()));
            appliedTileGid = tileGid;
        }
    }

    public void configureServer(int tileGid,
                                float sizeW,
                                float sizeH,
                                float sortOffsetY,
                                int zOrder) {
        configureServer(tileGid, sizeW, sizeH, sortOffsetY, zOrder, 0f, 0f, new Vector2());
    }

    public void configureServer(int tileGid,
                                float sizeW,
                                float sizeH,
                                float sortOffsetY,
                                int zOrder,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset) {
        addSharedComponents(tileGid, sizeW, sizeH, sortOffsetY, zOrder, collHalfW, collHalfH, collOffset);
    }

    public void configureFromReplication(TiledMap map, Map<String, Object> initialProperties) {
        int tileGid = getInt(initialProperties, PROP_TILE_GID, 0);
        float sizeW = getFloat(initialProperties, PROP_SIZE_W, 1f);
        float sizeH = getFloat(initialProperties, PROP_SIZE_H, 1f);
        float sortOffsetY = getFloat(initialProperties, PROP_SORT_OFFSET_Y, 0f);
        int zOrder = getInt(initialProperties, PROP_Z_ORDER, 0);
        float collHalfW = getFloat(initialProperties, PROP_COLL_HALF_W, 0f);
        float collHalfH = getFloat(initialProperties, PROP_COLL_HALF_H, 0f);
        float collOffsetX = getFloat(initialProperties, PROP_COLL_OFFSET_X, 0f);
        float collOffsetY = getFloat(initialProperties, PROP_COLL_OFFSET_Y, 0f);

        configure(map, tileGid, resolveTileRegion(map, tileGid), sizeW, sizeH, sortOffsetY, zOrder,
            collHalfW, collHalfH, new Vector2(collOffsetX, collOffsetY));
    }

    public Map<String, Object> buildInitialReplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        TransformComponent transform = getComponent(TransformComponent.class);
        TileVisualStateComponent tileVisual = getComponent(TileVisualStateComponent.class);

        if (transform != null) {
            properties.put(PROP_SIZE_W, transform.getSize().x);
            properties.put(PROP_SIZE_H, transform.getSize().y);
            properties.put(PROP_SORT_OFFSET_Y, transform.getSortOffsetY());
            properties.put(PROP_Z_ORDER, transform.getZOrder());
        }
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        if (collision != null) {
            properties.put(PROP_COLL_HALF_W, collision.getHalfWidth());
            properties.put(PROP_COLL_HALF_H, collision.getHalfHeight());
            properties.put(PROP_COLL_OFFSET_X, collision.getOffset().x);
            properties.put(PROP_COLL_OFFSET_Y, collision.getOffset().y);
        }
        if (tileVisual != null) {
            properties.put(PROP_TILE_GID, tileVisual.getTileGid());
        }

        return properties;
    }

    public SaveGameData.VisualData buildSaveData() {
        SaveGameData.VisualData data = new SaveGameData.VisualData();
        TransformComponent transform = getComponent(TransformComponent.class);
        TileVisualStateComponent tileVisual = getComponent(TileVisualStateComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);

        if (tileVisual != null) {
            data.tileGid = tileVisual.getTileGid();
        }
        if (transform != null) {
            data.x = transform.getPosition().x;
            data.y = transform.getPosition().y;
            data.sizeW = transform.getSize().x;
            data.sizeH = transform.getSize().y;
            data.sortOffsetY = transform.getSortOffsetY();
            data.zOrder = transform.getZOrder();
        }
        if (collision != null) {
            data.collHalfW = collision.getHalfWidth();
            data.collHalfH = collision.getHalfHeight();
            data.collOffsetX = collision.getOffset().x;
            data.collOffsetY = collision.getOffset().y;
        }
        return data;
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
        syncSpriteRegion();
    }

    private void addSharedComponents(int tileGid,
                                     float sizeW,
                                     float sizeH,
                                     float sortOffsetY,
                                     int zOrder,
                                     float collHalfW,
                                     float collHalfH,
                                     Vector2 collOffset) {
        addComponent(new TransformComponent(
            new Vector2(),
            zOrder,
            new Vector2(sizeW, sizeH),
            new Vector2(1f, 1f),
            0f,
            sortOffsetY
        ));
        addComponent(new TileVisualStateComponent(tileGid));
        if (collHalfW > 0f && collHalfH > 0f) {
            BoxCollisionComponent collision = new BoxCollisionComponent(
                CollisionProfile.ENVIRONMENT,
                collHalfW,
                collHalfH,
                collOffset != null ? new Vector2(collOffset) : new Vector2()
            );
            collision.setBodyTypeOverride(BodyDef.BodyType.StaticBody);
            addComponent(collision);
        }
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

    private TextureRegion resolveTileRegion(TiledMap map, int tileGid) {
        if (map == null || tileGid <= 0) {
            return null;
        }

        TiledMapTile tile = map.getTileSets().getTile(tileGid);
        return tile != null ? tile.getTextureRegion() : null;
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
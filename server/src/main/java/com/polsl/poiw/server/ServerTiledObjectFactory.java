package com.polsl.poiw.server;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.tiled.TiledObjectFactory;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.TrainingDummyActor;
import com.polsl.poiw.gameplay.actor.TriggerActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.TransformComponent;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.MapLayer;

import static com.polsl.poiw.engine.tiled.TiledConstants.PPM;

/**
 * fabryka obiektów Tiled po stronie serwera — headless, bez tekstur.
 * tworzy TriggerActor (z kolizją sensor) i statyczne kolizje z PropActor (bez sprite'ów).
 */
public class ServerTiledObjectFactory implements TiledObjectFactory {

    private static final String TAG = "ServerTiledObjectFactory";

    private final GameWorld gameWorld;
    private TiledMapTileLayer waterLayer;
    private HeadlessTmxLoader tmxLoader;

    public ServerTiledObjectFactory(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
    }

    public void setTmxLoader(HeadlessTmxLoader loader) {
        this.tmxLoader = loader;
    }

    /**
     * ustawia referencję do mapy — potrzebne do sprawdzania, czy obiekt stoi na wodzie.
     */
    public void setMap(TiledMap map) {
        MapLayer layer = map.getLayers().get("water");
        if (layer instanceof TiledMapTileLayer tl) {
            this.waterLayer = tl;
        }
    }

    @Override
    public Actor createFromMapObject(String type, MapObject mapObject) {
        // tile object z warstwy "objects" (ma gid → referencja do tile w .tsx)
        if (mapObject instanceof TiledMapTileMapObject tileObj) {
            return createCollisionFromTileObject(type, tileObj);
        }

        if (mapObject instanceof RectangleMapObject rectObj) {
            // headless loader stores gid as property for tile objects
            Integer gid = rectObj.getProperties().get("gid", Integer.class);
            if (gid != null && gid > 0) {
                return createCollisionFromHeadlessObject(type, rectObj, gid);
            }
            // bare rectangle without gid → trigger
            return createTrigger(type, rectObj);
        }

        return null;
    }

    // ===== Trigger =====

    private Actor createTrigger(String name, RectangleMapObject rectObj) {
        Rectangle rect = rectObj.getRectangle();

        float halfW = rect.width / 2f / PPM;
        float halfH = rect.height / 2f / PPM;
        float cx = (rect.x + rect.width / 2f) / PPM;
        float cy = (rect.y + rect.height / 2f) / PPM;

        TriggerActor trigger = new TriggerActor();
        trigger.configure(name, halfW, halfH);

        Float dps = rectObj.getProperties().get("dps", Float.class);
        if (dps != null) {
            trigger.setDamagePerSecond(dps);
        }

        gameWorld.spawnActor(trigger, new Vector2(cx - halfW, cy - halfH));
        Gdx.app.debug(TAG, "Trigger '" + name + "' at (" + cx + ", " + cy + ")"
            + (dps != null ? " [dps=" + dps + "]" : ""));
        return trigger;
    }

    // ===== Tile Object from HeadlessTmxLoader (RectangleMapObject + gid) =====

    private Actor createCollisionFromHeadlessObject(String type, RectangleMapObject rectObj, int gid) {
        String objName = rectObj.getName();
        if ("Player".equals(objName)) {
            return null;
        }

        if (tmxLoader == null) {
            Gdx.app.debug(TAG, "No tmxLoader set — cannot resolve gid=" + gid);
            return null;
        }

        HeadlessTmxLoader.TileData tileData = tmxLoader.getTileData(gid);
        if (tileData == null) {
            Gdx.app.debug(TAG, "No tile data for gid=" + gid + " name=" + objName);
            return null;
        }

        // rozmiar sprite w metrach
        float sizeW = tileData.imageW() / PPM;
        float sizeH = tileData.imageH() / PPM;

        float worldX = rectObj.getRectangle().x / PPM;
        float worldY = rectObj.getRectangle().y / PPM;

        if (!tileData.hasCollision()) {
            return null; // no collision shape → skip
        }

        boolean trainingDummyTile = isTrainingDummyTile(tileData, objName);

        if (!trainingDummyTile && isOnWater(worldX, worldY)) {
            return null; // skip water objects
        }

        // compute collision offset relative to sprite center (same logic as DefaultTiledObjectFactory)
        float spriteCenterXpx = tileData.imageW() / 2f;
        float spriteCenterYpx = tileData.imageH() / 2f;
        float collCenterXpx = tileData.collX() + tileData.collW() / 2f;
        float collCenterYpx = tileData.collY() + tileData.collH() / 2f;
        float offsetX = (collCenterXpx - spriteCenterXpx) / PPM;
        float offsetY = (collCenterYpx - spriteCenterYpx) / PPM;
        float halfW = tileData.collW() / 2f / PPM;
        float halfH = tileData.collH() / 2f / PPM;
        float sortOffsetY = sizeH / 2f + offsetY - halfH;
        int zOrder = getIntProperty(tileData.properties(), "z", 1);
        float maxHealth = getFloatProperty(tileData.properties(), "life", 100f);

        if (halfW <= 0 || halfH <= 0) return null;

        if (trainingDummyTile) {
            TrainingDummyActor trainingDummy = new TrainingDummyActor();
            trainingDummy.configureServer(
                sizeW,
                sizeH,
                halfW,
                halfH,
                new Vector2(offsetX, offsetY),
                sortOffsetY,
                zOrder,
                maxHealth
            );
            trainingDummy.setReplicated(true);

            gameWorld.spawnActor(trainingDummy, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Training dummy '" + (objName != null ? objName : type)
                + "' at (" + worldX + ", " + worldY + ") [replicated gid=" + gid + "]");
            return trainingDummy;
        }

        ServerPropActor prop = new ServerPropActor();
        prop.configure(sizeW, sizeH, halfW, halfH, new Vector2(offsetX, offsetY));

        gameWorld.spawnActor(prop, new Vector2(worldX, worldY));
        Gdx.app.debug(TAG, "ServerProp '" + (objName != null ? objName : type)
            + "' at (" + worldX + ", " + worldY + ") [collision gid=" + gid + "]");
        return prop;
    }

    // ===== Tile Object — only collision, no sprites =====

    private Actor createCollisionFromTileObject(String type, TiledMapTileMapObject tileObj) {
        TiledMapTile tile = tileObj.getTile();
        if (tile == null) return null;

        String objName = tileObj.getName();
        if ("Player".equals(objName)) {
            return null;
        }

        // rozmiar sprite w metrach (potrzebny do obliczenia offsetu kolizji)
        float regionW = tileObj.getTextureRegion() != null ? tileObj.getTextureRegion().getRegionWidth() : 32f;
        float regionH = tileObj.getTextureRegion() != null ? tileObj.getTextureRegion().getRegionHeight() : 32f;
        float sizeW = regionW / PPM;
        float sizeH = regionH / PPM;

        float worldX = tileObj.getX() / PPM;
        float worldY = tileObj.getY() / PPM;

        // odczytaj collision shape z tile objectgroup
        CollisionData collData = extractCollisionFromTile(tile, sizeW, sizeH);

        if (collData != null && isOnWater(worldX, worldY)) {
            collData = null;
        }

        // serwer tworzy actora z kolizją tylko jeśli tile ma collision shape
        if (collData == null || collData.halfW <= 0 || collData.halfH <= 0) {
            return null;
        }

        // headless prop — transform + collision, bez sprite
        ServerPropActor prop = new ServerPropActor();
        prop.configure(sizeW, sizeH, collData.halfW, collData.halfH,
            new Vector2(collData.offsetX, collData.offsetY));

        gameWorld.spawnActor(prop, new Vector2(worldX, worldY));
        Gdx.app.debug(TAG, "ServerProp '" + (objName != null ? objName : type)
            + "' at (" + worldX + ", " + worldY + ") [collision]");
        return prop;
    }

    // ===== Collision Extraction =====

    private CollisionData extractCollisionFromTile(TiledMapTile tile, float spriteW, float spriteH) {
        MapObjects objects = tile.getObjects();
        if (objects == null || objects.getCount() == 0) return null;

        for (MapObject obj : objects) {
            Boolean isSensor = obj.getProperties().get("sensor", false, Boolean.class);
            if (isSensor) continue;

            if (obj instanceof RectangleMapObject rectObj) {
                Rectangle rect = rectObj.getRectangle();
                return collisionFromRect(rect, spriteW, spriteH);
            }

            if (obj instanceof EllipseMapObject ellipseObj) {
                Ellipse ellipse = ellipseObj.getEllipse();
                Rectangle rect = new Rectangle(ellipse.x, ellipse.y, ellipse.width, ellipse.height);
                return collisionFromRect(rect, spriteW, spriteH);
            }

            if (obj instanceof PolygonMapObject polyObj) {
                float[] vertices = polyObj.getPolygon().getTransformedVertices();
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
                float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
                for (int i = 0; i < vertices.length; i += 2) {
                    minX = Math.min(minX, vertices[i]);
                    minY = Math.min(minY, vertices[i + 1]);
                    maxX = Math.max(maxX, vertices[i]);
                    maxY = Math.max(maxY, vertices[i + 1]);
                }
                Rectangle rect = new Rectangle(minX, minY, maxX - minX, maxY - minY);
                return collisionFromRect(rect, spriteW, spriteH);
            }
        }
        return null;
    }

    private boolean isOnWater(float worldX, float worldY) {
        if (waterLayer == null) return false;
        int tileX = (int) worldX;
        int tileY = (int) worldY;
        return waterLayer.getCell(tileX, tileY) != null;
    }

    private CollisionData collisionFromRect(Rectangle rect, float spriteW, float spriteH) {
        float spriteWpx = spriteW * PPM;
        float spriteHpx = spriteH * PPM;

        float collCenterXpx = rect.x + rect.width / 2f;
        float collCenterYpx = rect.y + rect.height / 2f;

        float spriteCenterXpx = spriteWpx / 2f;
        float spriteCenterYpx = spriteHpx / 2f;

        float offsetXpx = collCenterXpx - spriteCenterXpx;
        float offsetYpx = collCenterYpx - spriteCenterYpx;

        return new CollisionData(
            rect.width / 2f / PPM,
            rect.height / 2f / PPM,
            offsetXpx / PPM,
            offsetYpx / PPM
        );
    }

    private record CollisionData(float halfW, float halfH, float offsetX, float offsetY) {}

    private boolean isTrainingDummyTile(HeadlessTmxLoader.TileData tileData, String objName) {
        if (!"Object".equals(tileData.type())) {
            return false;
        }

        Object bodyType = tileData.properties().get("bodyType");
        return bodyType instanceof String body && "StaticBody".equals(body)
            && tileData.properties().containsKey("life")
            && !"Player".equals(objName);
    }

    private int getIntProperty(java.util.Map<String, Object> properties, String key, int defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private float getFloatProperty(java.util.Map<String, Object> properties, String key, float defaultValue) {
        Object value = properties.get(key);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    /**
     * headless prop actor — transform + collision, bez sprite.
     * używany na serwerze do odwzorowania statycznych przeszkód z mapy.
     */
    static class ServerPropActor extends com.polsl.poiw.engine.actor.AbstractActor {
        public void configure(float sizeW, float sizeH,
                              float collHalfW, float collHalfH, Vector2 collOffset) {
            addComponent(new TransformComponent(
                new Vector2(), 1, new Vector2(sizeW, sizeH)
            ));
            addComponent(new BoxCollisionComponent(
                CollisionProfile.ENVIRONMENT, collHalfW, collHalfH, collOffset
            ));
        }
    }
}

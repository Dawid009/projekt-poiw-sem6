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
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.tiled.TiledObjectFactory;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.AbstractCreatureActor;
import com.polsl.poiw.gameplay.actor.CropActor;
import com.polsl.poiw.gameplay.actor.CropKind;
import com.polsl.poiw.gameplay.actor.CreatureKind;
import com.polsl.poiw.gameplay.actor.MineableActor;
import com.polsl.poiw.gameplay.actor.MineableKind;
import com.polsl.poiw.gameplay.actor.TreeActor;
import com.polsl.poiw.gameplay.actor.TreeKind;
import com.polsl.poiw.gameplay.actor.TrainingDummyActor;
import com.polsl.poiw.gameplay.actor.TriggerActor;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.MapLayer;

import static com.polsl.poiw.engine.tiled.TiledConstants.*;

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

        boolean trainingDummyTile = isTrainingDummyTile(tileData, objName);
        CreatureKind creatureKind = getCreatureKind(tileData);
        MineableKind mineableKind = getMineableKind(tileData, type);
        TreeKind treeKind = getTreeKind(tileData, type);
        CropData cropData = getCropData(gid, tileData, type);

        if (!tileData.hasCollision()
            && creatureKind == null
            && !trainingDummyTile
            && mineableKind == null
            && treeKind == null
            && cropData == null) {
            return null;
        }

        if (!trainingDummyTile
            && creatureKind == null
            && mineableKind == null
            && treeKind == null
            && cropData == null
            && isOnWater(worldX, worldY)) {
            return null; // skip water objects
        }

        CollisionData collisionData = tileData.hasCollision()
            ? collisionFromTileData(tileData, sizeW, sizeH)
            : cropData != null ? defaultCropCollision(sizeW, sizeH) : null;

        if (collisionData == null || collisionData.halfW <= 0f || collisionData.halfH <= 0f) {
            return null;
        }

        float offsetX = collisionData.offsetX;
        float offsetY = collisionData.offsetY;
        float halfW = collisionData.halfW;
        float halfH = collisionData.halfH;
        float sortOffsetY = sizeH / 2f + offsetY - halfH;
        int zOrder = getIntProperty(tileData.properties(), "z", 1);
        float maxHealth = getFloatProperty(tileData.properties(), "life", 100f);

        if (creatureKind != null) {
            float creatureHealth = getFloatProperty(tileData.properties(), "life", AbstractCreatureActor.DEFAULT_MAX_HEALTH);
            AbstractCreatureActor creature = creatureKind.createActor();
            creature.configureServer(
                sizeW,
                sizeH,
                halfW,
                halfH,
                new Vector2(offsetX, offsetY),
                sortOffsetY,
                zOrder,
                creatureHealth,
                creatureHealth
            );
            creature.setReplicated(true);

            gameWorld.spawnActor(creature, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Creature '" + creatureKind + "' at (" + worldX + ", " + worldY + ") [replicated gid=" + gid + "]");
            return creature;
        }

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

        if (mineableKind != null) {
            float mineableHealth = getFloatProperty(tileData.properties(), "life", mineableKind.getMaxHealth());
            MineableActor mineable = new MineableActor();
            mineable.configureServer(
                gid,
                sizeW,
                sizeH,
                halfW,
                halfH,
                new Vector2(offsetX, offsetY),
                sortOffsetY,
                zOrder,
                mineableHealth,
                mineableHealth
            );
            mineable.setReplicated(true);

            gameWorld.spawnActor(mineable, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Mineable '" + mineableKind + "' at (" + worldX + ", " + worldY + ") [replicated gid=" + gid + "]");
            return mineable;
        }

        if (treeKind != null) {
            float treeHealth = getFloatProperty(tileData.properties(), "life", treeKind.getMaxHealth());
            TreeActor tree = new TreeActor();
            int stumpTileGid = getIntProperty(tileData.properties(), "stump_tile_gid", 17);
            float stumpWidth = getFloatProperty(tileData.properties(), "stump_width", treeKind == TreeKind.SMALL ? 1.5f : 2f);
            float stumpHeight = getFloatProperty(tileData.properties(), "stump_height", treeKind == TreeKind.SMALL ? 1.5f : 2f);
            tree.setTreeKind(treeKind);
            tree.setStumpTileGid(stumpTileGid);
            tree.setStumpSize(stumpWidth, stumpHeight);
            HeadlessTmxLoader.TileData stumpTileData = tmxLoader != null ? tmxLoader.getTileData(stumpTileGid) : null;
            CollisionData stumpCollision = collisionFromTileData(stumpTileData, stumpWidth, stumpHeight);
            if (stumpCollision != null) {
                tree.setStumpCollision(stumpCollision.halfW, stumpCollision.halfH,
                    new Vector2(stumpCollision.offsetX, stumpCollision.offsetY));
            }
            tree.configureServer(
                gid,
                sizeW,
                sizeH,
                halfW,
                halfH,
                new Vector2(offsetX, offsetY),
                sortOffsetY,
                zOrder,
                treeHealth,
                treeHealth
            );
            tree.setReplicated(true);

            gameWorld.spawnActor(tree, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Tree '" + treeKind + "' at (" + worldX + ", " + worldY + ") [replicated gid=" + gid + "]");
            return tree;
        }

        if (cropData != null) {
            CropActor crop = new CropActor();
            crop.configureServer(
                gid,
                cropData.kind(),
                cropData.growthStage(),
                cropData.globalStageTileGids(),
                sizeW,
                sizeH,
                halfW,
                halfH,
                new Vector2(offsetX, offsetY),
                sortOffsetY,
                zOrder,
                cropData.growthIntervalSeconds(),
                cropData.maxHealth()
            );
            crop.setReplicated(true);

            gameWorld.spawnActor(crop, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Crop '" + cropData.kind() + "' stage=" + cropData.growthStage()
                + " at (" + worldX + ", " + worldY + ") [replicated gid=" + gid + "]");
            return crop;
        }

        ServerPropActor prop = new ServerPropActor();
        prop.configure(sizeW, sizeH, halfW, halfH, new Vector2(offsetX, offsetY), sortOffsetY, zOrder);

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

        // props nie biorą udziału w kolizji gameplay po stronie serwera
        if (collData == null || collData.halfW <= 0 || collData.halfH <= 0) {
            return null;
        }

        ServerPropActor prop = new ServerPropActor();
        float sortOffsetY = sizeH / 2f + collData.offsetY - collData.halfH;
        prop.configure(sizeW, sizeH, collData.halfW, collData.halfH,
            new Vector2(collData.offsetX, collData.offsetY), sortOffsetY, 1);

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

    private CollisionData collisionFromTileData(HeadlessTmxLoader.TileData tileData, float spriteW, float spriteH) {
        if (tileData == null || !tileData.hasCollision()) {
            return null;
        }

        float baseSpriteW = tileData.imageW() / PPM;
        float baseSpriteH = tileData.imageH() / PPM;
        float scaleX = baseSpriteW > 0f ? spriteW / baseSpriteW : 1f;
        float scaleY = baseSpriteH > 0f ? spriteH / baseSpriteH : 1f;

        float spriteCenterXpx = tileData.imageW() / 2f;
        float spriteCenterYpx = tileData.imageH() / 2f;
        float collCenterXpx = tileData.collX() + tileData.collW() / 2f;
        float collCenterYpx = tileData.collY() + tileData.collH() / 2f;
        float offsetX = (collCenterXpx - spriteCenterXpx) / PPM * scaleX;
        float offsetY = (collCenterYpx - spriteCenterYpx) / PPM * scaleY;
        float halfW = tileData.collW() / 2f / PPM * scaleX;
        float halfH = tileData.collH() / 2f / PPM * scaleY;
        return new CollisionData(halfW, halfH, offsetX, offsetY);
    }

    private CollisionData defaultCropCollision(float spriteW, float spriteH) {
        return new CollisionData(
            spriteW * 0.22f,
            spriteH * 0.16f,
            0f,
            -spriteH * 0.18f
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

    private CreatureKind getCreatureKind(HeadlessTmxLoader.TileData tileData) {
        if (!"Creature".equals(tileData.type())) {
            return null;
        }

        Object creatureType = tileData.properties().get("creature_type");
        return creatureType instanceof String value ? CreatureKind.fromMetadata(value) : null;
    }

    private MineableKind getMineableKind(HeadlessTmxLoader.TileData tileData, String layerName) {
        if (!LAYER_MINEABLE.equals(layerName)) {
            return null;
        }

        Object oreType = tileData.properties().get("ore_type");
        return MineableKind.fromMetadata(oreType instanceof String value ? value : null);
    }

    private TreeKind getTreeKind(HeadlessTmxLoader.TileData tileData, String layerName) {
        if (!LAYER_TREES.equals(layerName)) {
            return null;
        }

        Object treeType = tileData.properties().get("tree_type");
        return TreeKind.fromMetadata(treeType instanceof String value ? value : null);
    }

    private CropData getCropData(int gid, HeadlessTmxLoader.TileData tileData, String layerName) {
        if (!LAYER_CROPS.equals(layerName)) {
            return null;
        }

        Object cropType = tileData.properties().get("crop_type");
        CropKind cropKind = CropKind.fromMetadata(cropType instanceof String value ? value : null);
        int growthStage = getIntProperty(tileData.properties(), "growth_stage", 0);
        return new CropData(
            cropKind,
            growthStage,
            cropKind.toGlobalStageTileIds(gid, growthStage),
            getFloatProperty(tileData.properties(), "growth_interval", cropKind.getGrowthIntervalSeconds()),
            getFloatProperty(tileData.properties(), "life", cropKind.getMaxHealth())
        );
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

    static class ServerPropActor extends com.polsl.poiw.engine.actor.AbstractActor {
        public void configure(float sizeW,
                              float sizeH,
                              float collHalfW,
                              float collHalfH,
                              Vector2 collOffset,
                              float sortOffsetY,
                              int zOrder) {
            addComponent(new TransformComponent(
                new Vector2(), zOrder, new Vector2(sizeW, sizeH), new Vector2(1f, 1f), 0f, sortOffsetY
            ));
            addComponent(new BoxCollisionComponent(
                CollisionProfile.ENVIRONMENT, collHalfW, collHalfH, collOffset
            ));
        }
    }

    private record CropData(CropKind kind,
                            int growthStage,
                            int[] globalStageTileGids,
                            float growthIntervalSeconds,
                            float maxHealth) {}
}

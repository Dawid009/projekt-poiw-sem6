package com.polsl.poiw.gameplay.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.tiled.TiledObjectFactory;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.AbstractCreatureActor;
import com.polsl.poiw.gameplay.actor.CropActor;
import com.polsl.poiw.gameplay.actor.CropKind;
import com.polsl.poiw.gameplay.actor.CreatureKind;
import com.polsl.poiw.gameplay.actor.MineableActor;
import com.polsl.poiw.gameplay.actor.MineableKind;
import com.polsl.poiw.gameplay.actor.PropActor;
import com.polsl.poiw.gameplay.actor.TreeActor;
import com.polsl.poiw.gameplay.actor.TreeKind;
import com.polsl.poiw.gameplay.actor.TrainingDummyActor;
import com.polsl.poiw.gameplay.actor.TriggerActor;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.MapLayer;

import static com.polsl.poiw.engine.tiled.TiledConstants.*;

/**
 * Domyślna fabryka tworząca Actorów z obiektów Tiled.
 * <p>
 * Obsługuje dwa typy obiektów zdefiniowane w objects.tsx:
 * <ul>
 *   <li><b>Prop</b> — statyczny obiekt środowiska (dom, skrzynia, drzewo).
 *       Odczytuje collision shape z tile objectgroup w .tsx.</li>
 *   <li><b>Object</b> — obiekt gameplay (trap, training_dummy, Player).
 *       Obiekty z type "Object" bez dodatkowej logiki są ignorowane (Player jest obsługiwany osobno).</li>
 * </ul>
 * <p>
 * Trigger objects (z warstwy "trigger") są tworzone jako {@link TriggerActor}.
 * <p>
 * <b>Organizacja w Tiled:</b>
 * <ul>
 *   <li>Każdy tile w objects.tsx powinien mieć ustawiony <b>Type</b> ("Prop" lub "Object")</li>
 *   <li>Kształt kolizji definiowany jest jako objectgroup wewnątrz tile (w Tiled: tile → Collision Editor)</li>
 *   <li>Triggery to prostokąty na warstwie "trigger" z property "sensor=true"</li>
 *   <li>Nazwy obiektów w Tiled odpowiadają logice gameplay (np. "trap_trigger", "Player")</li>
 * </ul>
 */
public class DefaultTiledObjectFactory implements TiledObjectFactory {

    private static final String TAG = "TiledObjectFactory";

    private final GameWorld gameWorld;
    private final TextureAtlas objectsAtlas;
    private final TextureAtlas creaturesAtlas;
    private TiledMap currentMap;
    private TiledMapTileLayer waterLayer;
    private boolean skipReplicatedDamageableObjects;

    public DefaultTiledObjectFactory(GameWorld gameWorld, TextureAtlas objectsAtlas, TextureAtlas creaturesAtlas) {
        this.gameWorld = gameWorld;
        this.objectsAtlas = objectsAtlas;
        this.creaturesAtlas = creaturesAtlas;
    }

    /**
     * Ustawia referencję do mapy — potrzebne do sprawdzania, czy obiekt stoi na wodzie.
     */
    public void setMap(TiledMap map) {
        this.currentMap = map;
        MapLayer layer = map.getLayers().get(LAYER_WATER);
        if (layer instanceof TiledMapTileLayer tl) {
            this.waterLayer = tl;
        }
    }

    public void setSkipReplicatedDamageableObjects(boolean skipReplicatedDamageableObjects) {
        this.skipReplicatedDamageableObjects = skipReplicatedDamageableObjects;
    }

    @Override
    public Actor createFromMapObject(String type, MapObject mapObject) {
        // === Trigger z warstwy "trigger" (prostokąt bez gid) ===
        if (mapObject instanceof RectangleMapObject rectObj) {
            return createTrigger(type, rectObj);
        }

        // === Tile object z warstwy "objects" (ma gid → referencja do tile w .tsx) ===
        if (mapObject instanceof TiledMapTileMapObject tileObj) {
            return createFromTileObject(type, tileObj);
        }

        Gdx.app.debug(TAG, "Nieobsługiwany typ MapObject: " + mapObject.getClass().getSimpleName());
        return null;
    }

    // ===== Trigger (warstwa "trigger") =====

    private Actor createTrigger(String name, RectangleMapObject rectObj) {
        Rectangle rect = rectObj.getRectangle();

        // Pozycja i rozmiar — przelicz piksele → metry
        // Tiled: lewy dolny róg, ale musimy ustawić centrum
        float halfW = rect.width / 2f / PPM;
        float halfH = rect.height / 2f / PPM;
        float cx = (rect.x + rect.width / 2f) / PPM;
        float cy = (rect.y + rect.height / 2f) / PPM;

        TriggerActor trigger = new TriggerActor();
        trigger.configure(name, halfW, halfH);

        // Odczytaj opcjonalną właściwość "dps" (damage per second) z Tiled
        Float dps = rectObj.getProperties().get("dps", Float.class);
        if (dps != null) {
            trigger.setDamagePerSecond(dps);
        }

        gameWorld.spawnActor(trigger, new Vector2(cx - halfW, cy - halfH));
        Gdx.app.debug(TAG, "Trigger '" + name + "' at (" + cx + ", " + cy + ")"
            + (dps != null ? " [dps=" + dps + "]" : ""));
        return trigger;
    }

    // ===== Tile Object (warstwa "objects") =====

    private Actor createFromTileObject(String type, TiledMapTileMapObject tileObj) {
        TiledMapTile tile = tileObj.getTile();
        if (tile == null) return null;

        // Typ z tile properties (Prop / Object) — zdefiniowany w objects.tsx
        String tileType = tile.getProperties().get("type", String.class);
        if (tileType == null) {
            // Fallback: sprawdź type przekazany z parsera
            tileType = type;
        }

        // Pomijaj obiekty typu Player — gracze są spawnowani osobno w travel callback
        String objName = tileObj.getName();
        if ("Player".equals(objName)) {
            return null;
        }

        // Region z atlasu — pobierz z tile (TextureRegion)
        TextureRegion region = tileObj.getTextureRegion();
        if (region == null) {
            Gdx.app.debug(TAG, "Brak TextureRegion dla tile object: " + objName);
            return null;
        }

        // Rozmiar sprite'a w metrach
        float sizeW = region.getRegionWidth() / PPM;
        float sizeH = region.getRegionHeight() / PPM;

        // Pozycja — Tiled tile objects mają origin na dole-lewo
        float worldX = tileObj.getX() / PPM;
        float worldY = tileObj.getY() / PPM;

        // Odczytaj collision shape z tile objectgroup (jeśli istnieje)
        CollisionData collData = extractCollisionFromTile(tile, sizeW, sizeH);
        CreatureKind creatureKind = getCreatureKind(tile, tileType);
        boolean trainingDummyTile = isTrainingDummyTile(tile, tileType);
        MineableKind mineableKind = getMineableKind(tile, type);
        TreeKind treeKind = getTreeKind(tile, type);
        CropData cropData = getCropData(tile, type);

        if (cropData != null && collData == null) {
            collData = defaultCropCollision(sizeW, sizeH);
        }

        // Sprawdź czy obiekt stoi na wodzie — jeśli tak, pomijamy kolizję
        // (woda i tak blokuje gracza, dekoracja na wodzie nie powinna mieć hitboxa)
        if (!trainingDummyTile
            && creatureKind == null
            && mineableKind == null
            && treeKind == null
            && cropData == null
            && collData != null
            && isOnWater(worldX, worldY)) {
            collData = null;
        }

        // sortOffsetY — punkt Y-sort to dolna krawędź kolizji (stopy pnia / podstawa domu)
        // position.y = dół sprite'a, collData.offsetY = offset od CENTRUM sprite do centrum kolizji
        // Dolna krawędź kolizji w przestrzeni sprite'a = sizeH/2 + offsetY - halfH
        // sortOffsetY dodawane jest do position.y, więc sortOffsetY = sizeH/2 + offsetY - halfH
        float sortOffsetY = 0f;
        if (collData != null) {
            sortOffsetY = sizeH / 2f + collData.offsetY - collData.halfH;
        }

        // zOrder z tile property "z" (np. trap ma z=0 → rysuje się pod graczem)
        int zOrder = tile.getProperties().get("z", 1, Integer.class);

        if (creatureKind != null) {
            if (skipReplicatedDamageableObjects) {
                Gdx.app.debug(TAG, "Skipping local creature spawn in multiplayer: " + creatureKind);
                return null;
            }

            float maxHealth = getFloatProperty(tile, "life", AbstractCreatureActor.DEFAULT_MAX_HEALTH);

            AbstractCreatureActor creature = creatureKind.createActor();
            creature.configure(
                creaturesAtlas,
                sizeW,
                sizeH,
                collData != null ? collData.halfW : 0f,
                collData != null ? collData.halfH : 0f,
                collData != null ? new Vector2(collData.offsetX, collData.offsetY) : Vector2.Zero,
                sortOffsetY,
                zOrder,
                maxHealth,
                maxHealth
            );

            gameWorld.spawnActor(creature, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Creature '" + creatureKind + "' at (" + worldX + ", " + worldY + ")");
            return creature;
        }

        if (trainingDummyTile) {
            if (skipReplicatedDamageableObjects) {
                Gdx.app.debug(TAG, "Skipping local training dummy spawn in multiplayer");
                return null;
            }

            Integer life = tile.getProperties().get("life", Integer.class);
            TrainingDummyActor trainingDummy = new TrainingDummyActor();
            trainingDummy.configure(
                objectsAtlas,
                sizeW,
                sizeH,
                collData != null ? collData.halfW : 0f,
                collData != null ? collData.halfH : 0f,
                collData != null ? new Vector2(collData.offsetX, collData.offsetY) : Vector2.Zero,
                sortOffsetY,
                zOrder,
                life != null ? life.floatValue() : 100f
            );

            gameWorld.spawnActor(trainingDummy, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Training dummy at (" + worldX + ", " + worldY + ") [combat target]");
            return trainingDummy;
        }

        if (mineableKind != null) {
            if (skipReplicatedDamageableObjects) {
                Gdx.app.debug(TAG, "Skipping local mineable spawn in multiplayer: " + mineableKind);
                return null;
            }

            float maxHealth = getFloatProperty(tile, "life", mineableKind.getMaxHealth());
            MineableActor mineable = new MineableActor();
            mineable.configure(
                currentMap,
                tile.getId(),
                region,
                sizeW,
                sizeH,
                collData != null ? collData.halfW : 0f,
                collData != null ? collData.halfH : 0f,
                collData != null ? new Vector2(collData.offsetX, collData.offsetY) : Vector2.Zero,
                sortOffsetY,
                zOrder,
                maxHealth,
                maxHealth
            );

            gameWorld.spawnActor(mineable, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Mineable '" + mineableKind + "' at (" + worldX + ", " + worldY + ")");
            return mineable;
        }

        if (treeKind != null) {
            if (skipReplicatedDamageableObjects) {
                Gdx.app.debug(TAG, "Skipping local tree spawn in multiplayer: " + treeKind);
                return null;
            }

            float maxHealth = getFloatProperty(tile, "life", treeKind.getMaxHealth());
            TreeActor tree = new TreeActor();
            int stumpTileGid = getIntProperty(tile, "stump_tile_gid", 17);
            float stumpWidth = getFloatProperty(tile, "stump_width", treeKind == TreeKind.SMALL ? 1.5f : 2f);
            float stumpHeight = getFloatProperty(tile, "stump_height", treeKind == TreeKind.SMALL ? 1.5f : 2f);
            tree.setTreeKind(treeKind);
            tree.setStumpTileGid(stumpTileGid);
            tree.setStumpSize(stumpWidth, stumpHeight);
            CollisionData stumpCollision = extractCollisionFromTileByGid(stumpTileGid, stumpWidth, stumpHeight);
            if (stumpCollision != null) {
                tree.setStumpCollision(stumpCollision.halfW, stumpCollision.halfH,
                    new Vector2(stumpCollision.offsetX, stumpCollision.offsetY));
            }
            tree.configure(
                currentMap,
                tile.getId(),
                region,
                sizeW,
                sizeH,
                collData != null ? collData.halfW : 0f,
                collData != null ? collData.halfH : 0f,
                collData != null ? new Vector2(collData.offsetX, collData.offsetY) : Vector2.Zero,
                sortOffsetY,
                zOrder,
                maxHealth,
                maxHealth
            );

            gameWorld.spawnActor(tree, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Tree '" + treeKind + "' at (" + worldX + ", " + worldY + ")");
            return tree;
        }

        if (cropData != null) {
            if (skipReplicatedDamageableObjects) {
                Gdx.app.debug(TAG, "Skipping local crop spawn in multiplayer: " + cropData.kind());
                return null;
            }

            CropActor crop = new CropActor();
            crop.configure(
                currentMap,
                tile.getId(),
                region,
                cropData.kind(),
                cropData.growthStage(),
                cropData.globalStageTileGids(),
                sizeW,
                sizeH,
                collData != null ? collData.halfW : 0f,
                collData != null ? collData.halfH : 0f,
                collData != null ? new Vector2(collData.offsetX, collData.offsetY) : Vector2.Zero,
                sortOffsetY,
                zOrder,
                cropData.growthIntervalSeconds(),
                cropData.maxHealth()
            );

            gameWorld.spawnActor(crop, new Vector2(worldX, worldY));
            Gdx.app.debug(TAG, "Crop '" + cropData.kind() + "' stage=" + cropData.growthStage()
                + " at (" + worldX + ", " + worldY + ")");
            return crop;
        }

        // Twórz PropActor
        PropActor prop = new PropActor();
        prop.configure(
            region, sizeW, sizeH,
            collData != null ? collData.halfW : 0f,
            collData != null ? collData.halfH : 0f,
            collData != null ? new Vector2(collData.offsetX, collData.offsetY) : Vector2.Zero,
            sortOffsetY,
            zOrder
        );

        gameWorld.spawnActor(prop, new Vector2(worldX, worldY));
        Gdx.app.debug(TAG, "Prop '" + (objName != null ? objName : tileType)
            + "' at (" + worldX + ", " + worldY + ")"
            + (collData != null ? " [collision]" : " [no collision]"));
        return prop;
    }

    private CollisionData extractCollisionFromTileByGid(int tileGid, float spriteW, float spriteH) {
        if (currentMap == null || tileGid <= 0) {
            return null;
        }

        TiledMapTile tile = currentMap.getTileSets().getTile(tileGid);
        if (tile == null) {
            return null;
        }

        return extractCollisionFromTile(tile, spriteW, spriteH);
    }

    // ===== Collision Extraction from Tile =====

    /**
     * Odczytuje kształt kolizji z objectgroup wewnątrz tile (.tsx).
     * Bierze PIERWSZY prostokąt/elipsę (bez property "sensor") jako kształt blokujący.
     *
     * @param tile tile z mapy
     * @param spriteW szerokość sprite'a w metrach
     * @param spriteH wysokość sprite'a w metrach
     * @return dane kolizji lub null jeśli tile nie ma collision objectgroup
     */
    private CollisionData extractCollisionFromTile(TiledMapTile tile, float spriteW, float spriteH) {
        MapObjects objects = tile.getObjects();
        if (objects == null || objects.getCount() == 0) return null;

        for (MapObject obj : objects) {
            // Pomijaj sensory (np. attack_sensor_down)
            Boolean isSensor = obj.getProperties().get("sensor", false, Boolean.class);
            if (isSensor) continue;

            if (obj instanceof RectangleMapObject rectObj) {
                Rectangle rect = rectObj.getRectangle();
                return collisionFromRect(rect, spriteW, spriteH);
            }

            if (obj instanceof EllipseMapObject ellipseObj) {
                // Przybliżamy elipsę prostokątem
                Ellipse ellipse = ellipseObj.getEllipse();
                Rectangle rect = new Rectangle(ellipse.x, ellipse.y, ellipse.width, ellipse.height);
                return collisionFromRect(rect, spriteW, spriteH);
            }

            if (obj instanceof PolygonMapObject polyObj) {
                // Przybliżamy polygon jego bounding box
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

    /**
     * Sprawdza czy obiekt o podanej pozycji (w metrach) stoi na kafelku wody.
     */
    private boolean isOnWater(float worldX, float worldY) {
        if (waterLayer == null) return false;
        int tileX = (int) worldX;
        int tileY = (int) worldY;
        return waterLayer.getCell(tileX, tileY) != null;
    }

    /**
     * Konwertuje prostokąt kolizji z pikseli tile'a na metry z offsetem od centrum sprite'a.
     * <p>
     * LibGDX TMX loader już konwertuje obiekty z tile objectgroup do Y-up (flipY=true),
     * więc rect jest już w przestrzeni Y-up (Y=0 na dole sprite'a).
     * Sprite rysowany od lewego dolnego rogu.
     * Body position = centrum sprite'a.
     * Offset kolizji = przesunięcie od centrum sprite'a do centrum prostokąta kolizji.
     */
    private CollisionData collisionFromRect(Rectangle rect, float spriteW, float spriteH) {
        float spriteWpx = spriteW * PPM;
        float spriteHpx = spriteH * PPM;

        // Centrum kolizji w pikselach (już Y-up, bo LibGDX TMX loader flipuje za nas)
        float collCenterXpx = rect.x + rect.width / 2f;
        float collCenterYpx = rect.y + rect.height / 2f;

        // Centrum sprite'a w pikselach
        float spriteCenterXpx = spriteWpx / 2f;
        float spriteCenterYpx = spriteHpx / 2f;

        // Offset od centrum sprite do centrum kolizji (oba w Y-up — bez dodatkowego flipu)
        float offsetXpx = collCenterXpx - spriteCenterXpx;
        float offsetYpx = collCenterYpx - spriteCenterYpx;

        return new CollisionData(
            rect.width / 2f / PPM,
            rect.height / 2f / PPM,
            offsetXpx / PPM,
            offsetYpx / PPM
        );
    }

    private boolean isTrainingDummyTile(TiledMapTile tile, String tileType) {
        if (!"Object".equals(tileType)) {
            return false;
        }

        String bodyType = tile.getProperties().get("bodyType", String.class);
        Integer life = tile.getProperties().get("life", Integer.class);
        return bodyType != null && bodyType.equals("StaticBody") && life != null;
    }

    private CreatureKind getCreatureKind(TiledMapTile tile, String tileType) {
        if (!"Creature".equals(tileType) || tile == null) {
            return null;
        }

        String creatureType = tile.getProperties().get("creature_type", String.class);
        return CreatureKind.fromMetadata(creatureType);
    }

    private MineableKind getMineableKind(TiledMapTile tile, String layerName) {
        if (!LAYER_MINEABLE.equals(layerName) || tile == null) {
            return null;
        }

        String oreType = tile.getProperties().get("ore_type", String.class);
        return MineableKind.fromMetadata(oreType);
    }

    private TreeKind getTreeKind(TiledMapTile tile, String layerName) {
        if (!LAYER_TREES.equals(layerName) || tile == null) {
            return null;
        }

        String treeType = tile.getProperties().get("tree_type", String.class);
        return TreeKind.fromMetadata(treeType);
    }

    private CropData getCropData(TiledMapTile tile, String layerName) {
        if (!LAYER_CROPS.equals(layerName) || tile == null) {
            return null;
        }

        String cropType = tile.getProperties().get("crop_type", String.class);
        CropKind cropKind = CropKind.fromMetadata(cropType);
        int growthStage = tile.getProperties().get("growth_stage", 0, Integer.class);
        float growthIntervalSeconds = getFloatProperty(tile, "growth_interval", cropKind.getGrowthIntervalSeconds());
        float maxHealth = getFloatProperty(tile, "life", cropKind.getMaxHealth());

        return new CropData(
            cropKind,
            growthStage,
            cropKind.toGlobalStageTileIds(tile.getId(), growthStage),
            growthIntervalSeconds,
            maxHealth
        );
    }

    private CollisionData defaultCropCollision(float spriteW, float spriteH) {
        return new CollisionData(
            spriteW * 0.22f,
            spriteH * 0.16f,
            0f,
            -spriteH * 0.18f
        );
    }

    private float getFloatProperty(TiledMapTile tile, String propertyName, float defaultValue) {
        if (tile == null) {
            return defaultValue;
        }

        Object value = tile.getProperties().get(propertyName);
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    private int getIntProperty(TiledMapTile tile, String propertyName, int defaultValue) {
        if (tile == null) {
            return defaultValue;
        }

        Object value = tile.getProperties().get(propertyName);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    /** Dane kolizji — halfW, halfH, offset od centrum body */
    private record CollisionData(float halfW, float halfH, float offsetX, float offsetY) {}

    private record CropData(CropKind kind,
                            int growthStage,
                            int[] globalStageTileGids,
                            float growthIntervalSeconds,
                            float maxHealth) {}
}

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
import com.polsl.poiw.gameplay.actor.PropActor;
import com.polsl.poiw.gameplay.actor.TriggerActor;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.MapLayer;

import static com.polsl.poiw.engine.tiled.TiledConstants.PPM;

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
    private final TextureAtlas atlas;
    private TiledMapTileLayer waterLayer;

    public DefaultTiledObjectFactory(GameWorld gameWorld, TextureAtlas atlas) {
        this.gameWorld = gameWorld;
        this.atlas = atlas;
    }

    /**
     * Ustawia referencję do mapy — potrzebne do sprawdzania, czy obiekt stoi na wodzie.
     */
    public void setMap(TiledMap map) {
        MapLayer layer = map.getLayers().get("water");
        if (layer instanceof TiledMapTileLayer tl) {
            this.waterLayer = tl;
        }
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

        // Pomijaj obiekty typu Player — gracze są spawnowani osobno w GameScreen
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

        // Sprawdź czy obiekt stoi na wodzie — jeśli tak, pomijamy kolizję
        // (woda i tak blokuje gracza, dekoracja na wodzie nie powinna mieć hitboxa)
        if (collData != null && isOnWater(worldX, worldY)) {
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

    /** Dane kolizji — halfW, halfH, offset od centrum body */
    private record CollisionData(float halfW, float halfH, float offsetX, float offsetY) {}
}

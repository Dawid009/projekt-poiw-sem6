package com.polsl.poiw.engine.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.engine.asset.MapAsset;
import com.polsl.poiw.engine.collision.CollisionChannel;
import com.polsl.poiw.engine.world.GameWorld;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.polsl.poiw.engine.tiled.TiledConstants.*;

/**
 * Parsuje mapę Tiled (.tmx) i tworzy obiekty w GameWorld.
 *
 * Odpowiada za:
 * 1. Tworzenie Box2D static bodies z warstwy "collision" (ściany)
 * 2. Odczytanie pozycji startowych gracza z warstwy "player"
 * 3. Przekazanie obiektów gameplay (spawny, skrzynie, pułapki) do TiledObjectFactory
*/
public class TiledMapParser {

    private final GameWorld gameWorld;
    private final AssetService assetService;
    private TiledObjectFactory objectFactory;
    private TiledMap currentMap;

    // Pozycje startowe graczy (odczytane z warstwy "player")
    private final List<Vector2> playerStartPositions = new ArrayList<>();

    public TiledMapParser(GameWorld gameWorld, AssetService assetService) {
        this.gameWorld = gameWorld;
        this.assetService = assetService;
    }

    /**
     * Ustawia fabrykę obiektów — tłumaczy typ obiektu Tiled na Actora.
     * Opcjonalna — jeśli null, obiekty z warstw obiektowych nie będą tworzone.
     */
    public void setObjectFactory(TiledObjectFactory factory) {
        this.objectFactory = factory;
    }

    /**
     * Ładuje mapę synchronicznie przez AssetService.
     * @param mapAsset enum mapy do załadowania
     * @return załadowana TiledMap
     */
    public TiledMap loadMap(MapAsset mapAsset) {
        return assetService.load(mapAsset);
    }

    /** Zwraca aktualnie sparsowaną mapę. */
    public TiledMap getCurrentMap() {
        return currentMap;
    }

    /**
     * Parsuje całą mapę Tiled.
     *
     * Wywoływane RAZ przy ładowaniu poziomu
     * @param map załadowana mapa TiledMap (z AssetManager)
     */
    public void parse(TiledMap map) {
        this.currentMap = map;
        parseCollisionLayer(map);
        parseWaterCollision(map);
        parseTileLayerCollision(map);
        parseStaticBackgroundObjectCollision(map);
        parseMapBoundaries(map);
        parsePlayerStartLayer(map);
        parsePlayerFromObjectsLayer(map);
        parseObjectLayers(map);
    }

    /**
     * Warstwa "collision" — tworzy Box2D static bodies.
     * Każde body oznaczone jest userData = CollisionChannel.ENVIRONMENT
     * aby CollisionSystem wiedział, że to statyczna przeszkoda.
     */
    private void parseCollisionLayer(TiledMap map) {
        MapLayer layer = map.getLayers().get(LAYER_COLLISION);
        if (layer == null) return;

        int count = 0;
        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof RectangleMapObject rectObj) {
                Rectangle rect = rectObj.getRectangle();

                // Przelicza piksele → metry Box2D
                float x = (rect.x + rect.width / 2f) / PPM;
                float y = (rect.y + rect.height / 2f) / PPM;
                float halfW = rect.width / 2f / PPM;
                float halfH = rect.height / 2f / PPM;

                // Tworzy Box2D static body (ściana)
                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;
                bodyDef.position.set(x, y);

                Body body = gameWorld.getBox2dWorld().createBody(bodyDef);
                // Oznaczamy jako ENVIRONMENT — CollisionSystem wie jak obsłużyć
                body.setUserData(CollisionChannel.ENVIRONMENT);

                PolygonShape shape = new PolygonShape();
                shape.setAsBox(halfW, halfH);

                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = shape;

                body.createFixture(fixtureDef);
                shape.dispose();
                count++;
            }
        }
        Gdx.app.debug("TiledMapParser", "Collision layer: " + count + " static bodies");
    }

    /**
     * Warstwa "water" — tworzy Box2D static bodies z kafelków wody.
     */
    private void parseWaterCollision(TiledMap map) {
        MapLayer layer = map.getLayers().get(LAYER_WATER);
        if (layer == null || !(layer instanceof TiledMapTileLayer tileLayer)) return;

        int count = 0;
        int width = tileLayer.getWidth();
        int height = tileLayer.getHeight();
        float tileW = tileLayer.getTileWidth();
        float tileH = tileLayer.getTileHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell == null) continue;

                // Centrum kafelka w metrach Box2D
                float cx = (x + 0.5f) * tileW / PPM;
                float cy = (y + 0.5f) * tileH / PPM;
                float halfW = tileW / 2f / PPM;
                float halfH = tileH / 2f / PPM;

                BodyDef bodyDef = new BodyDef();
                bodyDef.type = BodyDef.BodyType.StaticBody;
                bodyDef.position.set(cx, cy);

                Body body = gameWorld.getBox2dWorld().createBody(bodyDef);
                body.setUserData(CollisionChannel.ENVIRONMENT);

                PolygonShape shape = new PolygonShape();
                shape.setAsBox(halfW, halfH);

                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = shape;

                body.createFixture(fixtureDef);
                shape.dispose();
                count++;
            }
        }
        Gdx.app.debug("TiledMapParser", "Water collision: " + count + " static bodies");
    }

    /**
     * Szuka kolizji zapisanej w objectgroup poszczególnych kafelków.
     * To obsługuje np. płot albo klif, gdy blokowanie jest wpisane w sam tile, a nie w osobną warstwę.
     */
    private void parseTileLayerCollision(TiledMap map) {
        int count = 0;

        for (MapLayer layer : map.getLayers()) {
            if (!(layer instanceof TiledMapTileLayer tileLayer)) {
                continue;
            }
            if (LAYER_WATER.equals(layer.getName())) {
                continue;
            }

            for (int y = 0; y < tileLayer.getHeight(); y++) {
                for (int x = 0; x < tileLayer.getWidth(); x++) {
                    TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                    if (cell == null || cell.getTile() == null) {
                        continue;
                    }

                    count += createTileCollisionBodies(
                        cell.getTile(),
                        x * tileLayer.getTileWidth(),
                        y * tileLayer.getTileHeight(),
                        tileLayer.getTileWidth(),
                        tileLayer.getTileHeight()
                    );
                }
            }
        }

        Gdx.app.debug("TiledMapParser", "Tile collision: " + count + " static bodies");
    }

    /**
     * Buduje kolizję dla dekoracji z warstw tła, jeśli ich tile ma blokujący objectgroup.
     */
    private void parseStaticBackgroundObjectCollision(TiledMap map) {
        int count = 0;

        for (MapLayer layer : map.getLayers()) {
            if (!isStaticBackgroundObjectLayer(layer.getName())) {
                continue;
            }

            for (MapObject object : layer.getObjects()) {
                if (object instanceof TiledMapTileMapObject tileObject) {
                    TiledMapTile tile = tileObject.getTile();
                    if (!shouldCreateStaticObjectCollision(layer.getName(), tile)) {
                        continue;
                    }

                    float spriteW = tileObject.getTextureRegion() != null
                        ? tileObject.getTextureRegion().getRegionWidth()
                        : map.getProperties().get("tilewidth", Integer.class);
                    float spriteH = tileObject.getTextureRegion() != null
                        ? tileObject.getTextureRegion().getRegionHeight()
                        : map.getProperties().get("tileheight", Integer.class);

                    count += createTileCollisionBodies(tile, tileObject.getX(), tileObject.getY(), spriteW, spriteH);
                } else if (object instanceof RectangleMapObject rectObject) {
                    Integer gid = rectObject.getProperties().get("gid", Integer.class);
                    if (gid == null) {
                        continue;
                    }

                    TiledMapTile tile = map.getTileSets().getTile(gid);
                    if (!shouldCreateStaticObjectCollision(layer.getName(), tile)) {
                        continue;
                    }

                    Rectangle rect = rectObject.getRectangle();
                    count += createTileCollisionBodies(tile, rect.x, rect.y, rect.width, rect.height);
                }
            }
        }

        Gdx.app.debug("TiledMapParser", "Static object collision: " + count + " static bodies");
    }

    /**
     * Tworzy Box2D static bodies na krawędziach mapy (4 ściany).
     * Zapobiega wyjściu gracza poza mapę.
     */
    private void parseMapBoundaries(TiledMap map) {
        int mapW = map.getProperties().get("width", Integer.class);
        int mapH = map.getProperties().get("height", Integer.class);

        // Rozmiar mapy w metrach (1 tile = 1 metr)
        float w = mapW;
        float h = mapH;
        float thickness = 0.5f; // grubość ściany

        // Dolna krawędź
        createBoundaryBody(-thickness, h / 2f, thickness, h / 2f);
        // Górna krawędź
        createBoundaryBody(w + thickness, h / 2f, thickness, h / 2f);
        // Lewa krawędź
        createBoundaryBody(w / 2f, -thickness, w / 2f, thickness);
        // Prawa krawędź
        createBoundaryBody(w / 2f, h + thickness, w / 2f, thickness);

        Gdx.app.debug("TiledMapParser", "Map boundaries: 4 static bodies (" + w + "x" + h + " meters)");
    }

    private void createBoundaryBody(float cx, float cy, float halfW, float halfH) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(cx, cy);

        Body body = gameWorld.getBox2dWorld().createBody(bodyDef);
        body.setUserData(CollisionChannel.ENVIRONMENT);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfW, halfH);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;

        body.createFixture(fixtureDef);
        shape.dispose();
    }

    /**
     * Zamienia collision objectgroup z jednego tile'a na statyczne body w świecie.
     */
    private int createTileCollisionBodies(TiledMapTile tile,
                                          float worldXpx,
                                          float worldYpx,
                                          float spriteWpx,
                                          float spriteHpx) {
        if (tile == null) {
            return 0;
        }

        MapObjects objects = tile.getObjects();
        if (objects == null || objects.getCount() == 0) {
            return 0;
        }

        int count = 0;
        for (MapObject object : objects) {
            Boolean sensor = object.getProperties().get("sensor", false, Boolean.class);
            if (sensor) {
                continue;
            }

            Rectangle bounds = toCollisionBounds(object);
            if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
                continue;
            }

            float centerX = (worldXpx + bounds.x + bounds.width * 0.5f) / PPM;
            float centerY = (worldYpx + bounds.y + bounds.height * 0.5f) / PPM;
            createStaticCollisionBody(centerX, centerY, bounds.width * 0.5f / PPM, bounds.height * 0.5f / PPM);
            count++;
        }

        return count;
    }

    private Rectangle toCollisionBounds(MapObject object) {
        if (object instanceof RectangleMapObject rectObject) {
            return rectObject.getRectangle();
        }

        if (object instanceof EllipseMapObject ellipseObject) {
            Ellipse ellipse = ellipseObject.getEllipse();
            return new Rectangle(ellipse.x, ellipse.y, ellipse.width, ellipse.height);
        }

        if (object instanceof PolygonMapObject polygonObject) {
            float[] vertices = polygonObject.getPolygon().getTransformedVertices();
            float minX = Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            for (int index = 0; index < vertices.length; index += 2) {
                minX = Math.min(minX, vertices[index]);
                minY = Math.min(minY, vertices[index + 1]);
                maxX = Math.max(maxX, vertices[index]);
                maxY = Math.max(maxY, vertices[index + 1]);
            }

            if (minX == Float.MAX_VALUE || minY == Float.MAX_VALUE) {
                return null;
            }
            return new Rectangle(minX, minY, maxX - minX, maxY - minY);
        }

        return null;
    }

    private void createStaticCollisionBody(float cx, float cy, float halfW, float halfH) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(cx, cy);

        Body body = gameWorld.getBox2dWorld().createBody(bodyDef);
        body.setUserData(CollisionChannel.ENVIRONMENT);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfW, halfH);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        body.createFixture(fixtureDef);
        shape.dispose();
    }

    private boolean shouldCreateStaticObjectCollision(String layerName, TiledMapTile tile) {
        if (!hasBlockingCollision(tile)) {
            return false;
        }
        return isStaticBackgroundObjectLayer(layerName);
    }

    private boolean hasBlockingCollision(TiledMapTile tile) {
        if (tile == null) {
            return false;
        }

        MapObjects objects = tile.getObjects();
        if (objects == null || objects.getCount() == 0) {
            return false;
        }

        for (MapObject object : objects) {
            Boolean sensor = object.getProperties().get("sensor", false, Boolean.class);
            if (!sensor && toCollisionBounds(object) != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * Warstwa "player" — odczytuje pozycje startowe graczy.
     *
     * W Tiled: punkty (obiekty) z property "index" (0, 1, 2, 3...).
     * W multiplayer: gracz 0 spawnuje się na punkcie index=0, gracz 1 na index=1, itd.
     */
    private void parsePlayerStartLayer(TiledMap map) {
        MapLayer layer = map.getLayers().get(LAYER_PLAYER_START);
        if (layer == null) return;

        playerStartPositions.clear();
        for (MapObject obj : layer.getObjects()) {
            if (obj instanceof RectangleMapObject rectObj) {
                Rectangle rect = rectObj.getRectangle();
                float x = rect.x / PPM;
                float y = rect.y / PPM;
                playerStartPositions.add(new Vector2(x, y));
            }
        }

        // Sortuje po "index" property (jeśli istnieje)
        // Domyślna kolejność dodawania w Tiled
    }

    /**
     * Szuka obiektu o nazwie "Player" na dowolnej warstwie obiektowej (tile object z gid).
     */
    private void parsePlayerFromObjectsLayer(TiledMap map) {
        if (!playerStartPositions.isEmpty()) return; // Już znaleziono w warstwie "player"

        for (MapLayer layer : map.getLayers()) {
            if (!isParsableObjectLayer(layer)) continue;

            for (MapObject obj : layer.getObjects()) {
                if (!"Player".equals(obj.getName())) continue;

                if (obj instanceof TiledMapTileMapObject tileObj) {
                    float x = tileObj.getX() / PPM;
                    float y = tileObj.getY() / PPM;
                    playerStartPositions.add(new Vector2(x, y));
                    Gdx.app.debug("TiledMapParser", "Player start position from layer '" + layer.getName() + "': (" + x + ", " + y + ")");
                    return;
                } else if (obj instanceof RectangleMapObject rectObj) {
                    // headless loader — tile objects stored as RectangleMapObject with gid property
                    Rectangle rect = rectObj.getRectangle();
                    float x = rect.x / PPM;
                    float y = rect.y / PPM;
                    playerStartPositions.add(new Vector2(x, y));
                    Gdx.app.debug("TiledMapParser", "Player start position (headless) from layer '" + layer.getName() + "': (" + x + ", " + y + ")");
                    return;
                }
            }
        }
    }

    /**
     * Wszystkie warstwy obiektowe poza technicznymi — deleguje do TiledObjectFactory.
     */
    private void parseObjectLayers(TiledMap map) {
        if (objectFactory == null) {
            Gdx.app.debug("TiledMapParser", "Brak TiledObjectFactory — pomijam warstwy obiektowe");
            return;
        }

        for (MapLayer layer : map.getLayers()) {
            if (!isParsableObjectLayer(layer)) continue;

            String layerName = layer.getName();

            int count = 0;
            for (MapObject obj : layer.getObjects()) {
                // Typ z custom property
                String type = obj.getProperties().get(PROP_TYPE, String.class);

                // Fallback: nazwa obiektu
                if (type == null || type.isEmpty()) {
                    type = obj.getName();
                }

                // Fallback: nazwa warstwy (np. "trigger" dla triggerów)
                if (type == null || type.isEmpty()) {
                    type = layerName;
                }

                Actor actor = objectFactory.createFromMapObject(type, obj);
                if (actor != null) count++;
            }
            Gdx.app.debug("TiledMapParser", "Layer '" + layerName + "': " + count + " actors created");
        }
    }

    /**
     * Odfiltrowuje warstwy techniczne, żeby do fabryki trafiały tylko obiekty gameplay.
     */
    private boolean isParsableObjectLayer(MapLayer layer) {
        if (layer == null || layer instanceof TiledMapTileLayer) return false;
        if (layer.getObjects().getCount() == 0) return false;
        return isGameplayObjectLayer(layer.getName());
    }

    /** Pozycja startowa gracza (dla multiplayer: gracz o danym indeksie) */
    public Vector2 getPlayerStartPosition(int playerIndex) {
        if (playerStartPositions.isEmpty()) return new Vector2(2, 2); // fallback
        return playerStartPositions.get(playerIndex % playerStartPositions.size());
    }

    /** Zwraca wszystkie znalezione pozycje startowe bez możliwości ich modyfikacji. */
    public List<Vector2> getAllPlayerStartPositions() {
        return Collections.unmodifiableList(playerStartPositions);
    }
}

package com.polsl.poiw.engine.tiled;

import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.engine.asset.MapAsset;
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
     * Opcjonalna — jeśli null, obiekty z warstw spawns/objects/triggers nie będą tworzone.
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
        parsePlayerStartLayer(map);
        parseObjectLayers(map);
    }

    /**
     * Warstwa "collision" — tworzy Box2D static bodies.
     */
    private void parseCollisionLayer(TiledMap map) {
        MapLayer layer = map.getLayers().get(LAYER_COLLISION);
        if (layer == null) return;

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

                PolygonShape shape = new PolygonShape();
                shape.setAsBox(halfW, halfH);

                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.shape = shape;

                body.createFixture(fixtureDef);
                shape.dispose();
            }
        }
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
     * Warstwy "spawns", "objects", "triggers" — deleguje do TiledObjectFactory.
     * Engine tylko odczytuje "type" i przekazuje do fabryki.
     */
    private void parseObjectLayers(TiledMap map) {
        String[] layerNames = { LAYER_SPAWNS, LAYER_OBJECTS, LAYER_TRIGGERS };

        for (String layerName : layerNames) {
            MapLayer layer = map.getLayers().get(layerName);
            if (layer == null) continue;

            for (MapObject obj : layer.getObjects()) {
                String type = obj.getProperties().get(PROP_TYPE, String.class);
                if (type == null || type.isEmpty()) {
                    type = obj.getName(); // fallback: nazwa obiektu
                }

                if (type != null && !type.isEmpty() && objectFactory != null) {
                    objectFactory.createFromMapObject(type, obj);
                    // Actor jest już dodany do GameWorld przez fabrykę (lub null jeśli pomijany)
                }
            }
        }
    }

    /** Pozycja startowa gracza (dla multiplayer: gracz o danym indeksie) */
    public Vector2 getPlayerStartPosition(int playerIndex) {
        if (playerStartPositions.isEmpty()) return new Vector2(2, 2); // fallback
        return playerStartPositions.get(playerIndex % playerStartPositions.size());
    }

    public List<Vector2> getAllPlayerStartPositions() {
        return Collections.unmodifiableList(playerStartPositions);
    }
}

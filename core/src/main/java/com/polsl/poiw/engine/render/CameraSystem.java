package com.polsl.poiw.engine.render;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

/**
 * System odpowiedzialny za śledzenie kamery za wyznaczonym Actorem.
 * Kamera jest ograniczona do granic mapy (nie wychodzi poza krawędzie).
 */
public class CameraSystem extends EntitySystem {

    private final OrthographicCamera camera;
    private float mapWidthInMeters;
    private float mapHeightInMeters;
    private boolean hasBounds = false;

    public CameraSystem(OrthographicCamera camera) {
        // Priorytet tuż przed renderowaniem
        super(99);
        this.camera = camera;
    }

    /**
     * Ustawia mapę — potrzebną do obliczenia granic kamery.
     */
    public void setMap(TiledMap map) {
        if (map == null) {
            hasBounds = false;
            return;
        }

        // Oblicza wymiary mapy w metrach
        // (tiles * tileSize / PPM = tiles, bo UNIT_SCALE = 1/16 i tileSize = 16)
        TiledMapTileLayer firstLayer = null;
        for (int i = 0; i < map.getLayers().getCount(); i++) {
            if (map.getLayers().get(i) instanceof TiledMapTileLayer layer) {
                firstLayer = layer;
                break;
            }
        }

        if (firstLayer != null) {
            float tileWidth = firstLayer.getTileWidth();
            float tileHeight = firstLayer.getTileHeight();
            int mapWidth = firstLayer.getWidth();
            int mapHeight = firstLayer.getHeight();

            // Konwersja na metry: (tiles * tileSize) * UNIT_SCALE = tiles * (tileSize / PPM)
            // Ale UNIT_SCALE = 1/16 i tileSize = 16, więc wynik = ilość tile'ów
            this.mapWidthInMeters = mapWidth * tileWidth * com.polsl.poiw.Main.UNIT_SCALE;
            this.mapHeightInMeters = mapHeight * tileHeight * com.polsl.poiw.Main.UNIT_SCALE;
            this.hasBounds = true;
        }
    }

    @Override
    public void update(float deltaTime) {
        if (hasBounds) {
            clampCameraToBounds();
        }
        camera.update();
    }

    /**
     * Ogranicza kamerę tak, aby nie wychodziła poza granice mapy.
     */
    private void clampCameraToBounds() {
        float cameraHalfWidth = camera.viewportWidth * camera.zoom / 2f;
        float cameraHalfHeight = camera.viewportHeight * camera.zoom / 2f;

        // Jeśli mapa jest mniejsza niż viewport — wycentruj
        if (mapWidthInMeters <= camera.viewportWidth * camera.zoom) {
            camera.position.x = mapWidthInMeters / 2f;
        } else {
            camera.position.x = Math.max(cameraHalfWidth,
                Math.min(mapWidthInMeters - cameraHalfWidth, camera.position.x));
        }

        if (mapHeightInMeters <= camera.viewportHeight * camera.zoom) {
            camera.position.y = mapHeightInMeters / 2f;
        } else {
            camera.position.y = Math.max(cameraHalfHeight,
                Math.min(mapHeightInMeters - cameraHalfHeight, camera.position.y));
        }
    }

    public OrthographicCamera getCamera() { return camera; }
}

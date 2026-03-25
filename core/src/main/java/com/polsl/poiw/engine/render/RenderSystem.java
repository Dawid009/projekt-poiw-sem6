package com.polsl.poiw.engine.render;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.polsl.poiw.Main;

/**
 * System renderujący mapę Tiled i entity.
 */
public class RenderSystem extends EntitySystem {

    private final SpriteBatch batch;
    private final Viewport viewport;
    private final OrthographicCamera camera;

    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMap currentMap;

    public RenderSystem(SpriteBatch batch, Viewport viewport, OrthographicCamera camera) {
        // Niski priorytet = renderowany na końcu (po fizyce, logice)
        super(100);
        this.batch = batch;
        this.viewport = viewport;
        this.camera = camera;
    }

    /**
     * Ustawia aktualną mapę do renderowania.
     * Tworzy nowy OrthogonalTiledMapRenderer z odpowiednim UNIT_SCALE.
     */
    public void setMap(TiledMap map) {
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        this.currentMap = map;
        if (map != null) {
            this.mapRenderer = new OrthogonalTiledMapRenderer(map, Main.UNIT_SCALE, batch);
        }
    }

    @Override
    public void update(float deltaTime) {
        // Aplikuj viewport i kamerę
        viewport.apply();
        camera.update();

        // Renderuj mapę Tiled
        if (mapRenderer != null) {
            mapRenderer.setView(camera);
            mapRenderer.render();
        }
    }

    /**
     * Zwraca aktualnie ustawioną mapę.
     */
    public TiledMap getCurrentMap() {
        return currentMap;
    }

    /**
     * Zwalnia zasoby renderera mapy.
     */
    public void dispose() {
        if (mapRenderer != null) {
            mapRenderer.dispose();
            mapRenderer = null;
        }
    }
}

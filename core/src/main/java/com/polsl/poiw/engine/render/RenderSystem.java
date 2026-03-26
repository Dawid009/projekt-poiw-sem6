package com.polsl.poiw.engine.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * System renderujący mapę Tiled i entity z SpriteComponent + TransformComponent.
 * <p>
 * Kolejność renderowania:
 * <ol>
 *   <li>Warstwy tła (wszystkie tile layers z mapy)</li>
 *   <li>Entity posortowane po zOrder → Y-sort (gracz, drzewa, domy, skrzynie, wrogowie)</li>
 * </ol>
 * <p>
 * Obiekty z Tiled (drzewa, domy) są teraz Actorami z SpriteComponent + TransformComponent,
 * więc renderują się automatycznie z poprawnym Y-sort (gracz chodzi ZA drzewem / PRZED drzewem).
 */
public class RenderSystem extends SortedIteratingSystem implements Disposable {

    private final Batch batch;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final BatchTiledMapRenderer tiledRenderer;
    private final List<MapLayer> bgdLayers;

    public RenderSystem(Batch batch, Viewport viewport, OrthographicCamera camera) {
        super(
            Family.all(TransformComponent.class, SpriteComponent.class).get(),
            Comparator.comparing(TransformComponent.MAPPER::get),
            100
        );
        this.batch = batch;
        this.viewport = viewport;
        this.camera = camera;
        this.tiledRenderer = new OrthogonalTiledMapRenderer(null, Main.UNIT_SCALE, batch);
        this.bgdLayers = new ArrayList<>();
    }

    /**
     * Renderuje scenę: warstwy tła → entity (Y-sorted).
     */
    @Override
    public void update(float deltaTime) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
        viewport.apply();

        batch.begin();
        batch.setColor(Color.WHITE);
        tiledRenderer.setView(camera);

        // Warstwy tła (tile layers)
        bgdLayers.forEach(tiledRenderer::renderMapLayer);

        // Entity posortowane po zOrder / Y-sort
        // Obejmuje: gracza, PropActor (drzewa, domy, skrzynie), wrogów, etc.
        forceSort();
        super.update(deltaTime);

        batch.end();
    }

    /**
     * Rysuje pojedynczy entity na podstawie TransformComponent i SpriteComponent.
     */
    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent scene = TransformComponent.MAPPER.get(entity);
        SpriteComponent sprite = SpriteComponent.MAPPER.get(entity);
        TextureRegion region = sprite.getRegion();
        if (region == null) return;

        Vector2 position = scene.getPosition();
        Vector2 scaling = scene.getScaling();
        Vector2 size = scene.getSize();
        batch.setColor(sprite.getColor());
        batch.draw(
            region,
            position.x - (1f - scaling.x) * size.x * 0.5f,
            position.y - (1f - scaling.y) * size.y * 0.5f,
            size.x * 0.5f, size.y * 0.5f,
            size.x, size.y,
            scaling.x, scaling.y,
            scene.getRotationDeg()
        );
    }

    /**
     * Ustawia mapę — zbiera warstwy kafelkowe jako tło.
     * Warstwy obiektowe (objects, triggers) nie są renderowane tutaj,
     * bo obiekty z Tiled są Actorami z SpriteComponent.
     */
    public void setMap(TiledMap tiledMap) {
        tiledRenderer.setMap(tiledMap);
        bgdLayers.clear();

        for (MapLayer layer : tiledMap.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                bgdLayers.add(layer);
            }
        }
    }

    @Override
    public void dispose() {
        tiledRenderer.dispose();
    }
}

package com.polsl.poiw.engine.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.SortedIteratingSystem;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
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

import static com.polsl.poiw.engine.tiled.TiledConstants.LAYER_OBJECTS;

/**
 * System renderujący mapę Tiled i entity z SpriteComponent + TransformComponent.
 *
 * Kolejność renderowania:
 * 1. Warstwy tła (wszystkie warstwy kafelkowe PRZED warstwą "objects")
 * 2. Entity posortowane po zOrder → Y-sort
 * 3. Warstwy pierwszego planu (warstwy kafelkowe PO "objects")
 */
public class RenderSystem extends SortedIteratingSystem implements Disposable {

    private final Batch batch;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final BatchTiledMapRenderer tiledRenderer;
    private final List<MapLayer> bgdLayers;
    private MapLayer objectsLayer;

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
     * Renderuje scenę: tło → entity → pierwszy plan.
     */
    @Override
    public void update(float deltaTime) {
        AnimatedTiledMapTile.updateAnimationBaseTime();
        viewport.apply();

        batch.begin();
        batch.setColor(Color.WHITE);
        tiledRenderer.setView(camera);

        // Warstwy tła
        bgdLayers.forEach(tiledRenderer::renderMapLayer);

        // Entity posortowane po zOrder / Y-sort
        forceSort();
        super.update(deltaTime);

        // Renderuj obiekty z warstwy "objects" (drzewa, domy, itp.)
        if (objectsLayer != null) {
            renderObjectsLayer();
        }else{
            System.console().printf("No objects layer found!");
        }

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
     * Ustawia mapę — rozdziela warstwy na tło i pierwszy plan.
     * Warstwa "objects" zawiera obiekty kafelkowe (drzewa, domy) i jest renderowana osobno.
     */
    public void setMap(TiledMap tiledMap) {
        tiledRenderer.setMap(tiledMap);

        bgdLayers.clear();
        objectsLayer = null;

        for (MapLayer layer : tiledMap.getLayers()) {
            if (LAYER_OBJECTS.equals(layer.getName())) {
                objectsLayer = layer;
                continue;
            }
            // Pomijaj warstwy obiektowe bez kafelków (czyste MapLayer)
            if (layer.getClass().equals(MapLayer.class)) continue;

            // Warstwy kafelków
            if (layer instanceof TiledMapTileLayer) {
                bgdLayers.add(layer);
            }
        }
    }

    /**
     * Renderuje obiekty kafelkowe z warstwy "objects".
     */
    private void renderObjectsLayer() {
        if (objectsLayer == null) return;

        for (MapObject obj : objectsLayer.getObjects()) {
            if (obj instanceof TiledMapTileMapObject tileObj) {
                renderTileObject(tileObj);
            }
        }
    }

    /**
     * Renderuje pojedynczy obiekt kafelkowy z mapy.
     */
    private void renderTileObject(TiledMapTileMapObject tileObj) {
        TextureRegion region = tileObj.getTextureRegion();
        if (region == null) return;

        float unit = Main.UNIT_SCALE;

        float x = tileObj.getX() * unit;
        float y = tileObj.getY() * unit;

        float scaleX = tileObj.getScaleX();
        float scaleY = tileObj.getScaleY();
        float rotation = tileObj.getRotation();

        float width = region.getRegionWidth() * unit;
        float height = region.getRegionHeight() * unit;

        // UWAGA: Tiled ma origin na dole-lewo dla tile objects
        float originX = width * 0.5f;
        float originY = height * 0.5f;

        batch.setColor(Color.WHITE);
        batch.draw(
            region,
            x, y,
            originX, originY,
            width, height,
            scaleX, scaleY,
            rotation
        );
    }

    @Override
    public void dispose() {
        tiledRenderer.dispose();
    }
}

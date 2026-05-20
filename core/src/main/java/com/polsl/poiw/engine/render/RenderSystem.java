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
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.EllipseMapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.objects.TiledMapTileMapObject;
import com.badlogic.gdx.maps.tiled.renderers.BatchTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.AnimatedTiledMapTile;
import com.badlogic.gdx.math.Ellipse;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.polsl.poiw.engine.tiled.TiledConstants.isStaticBackgroundObjectLayer;

/**
 * System renderujący mapę Tiled i entity z SpriteComponent + TransformComponent.
 * <p>
 * Kolejność renderowania:
 * <ol>
 *   <li>Warstwy tła (tile layers + statyczne object layers renderowane bezpośrednio z TMX)</li>
 *   <li>Entity posortowane po zOrder → Y-sort (gracz, drzewa, mineable, cropy, wrogowie)</li>
 * </ol>
 * <p>
 * Statyczne warstwy środowiskowe (houses, small_flora, bridges) renderowane są bezpośrednio z TMX,
 * a obiekty gameplay renderują się jako osobne Actory z poprawnym Y-sort.
 */
public class RenderSystem extends SortedIteratingSystem implements Disposable {

    private final Batch batch;
    private final OrthographicCamera camera;
    private final Viewport viewport;

    private final BatchTiledMapRenderer tiledRenderer;
    private final List<MapLayer> bgdLayers;
    private final List<StaticObjectRenderable> backgroundObjects;
    private final List<StaticObjectRenderable> foregroundObjects;
    private int foregroundObjectIndex;

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
        this.backgroundObjects = new ArrayList<>();
        this.foregroundObjects = new ArrayList<>();
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

        // Warstwy tła (tile layers + wybrane object layers)
        for (MapLayer layer : bgdLayers) {
            tiledRenderer.renderMapLayer(layer);
        }
        backgroundObjects.forEach(this::renderStaticObject);

        // Entity posortowane po zOrder / Y-sort
        // Obejmuje: gracza, PropActor (drzewa, domy, skrzynie), wrogów, etc.
        foregroundObjectIndex = 0;
        forceSort();
        super.update(deltaTime);
        while (foregroundObjectIndex < foregroundObjects.size()) {
            renderStaticObject(foregroundObjects.get(foregroundObjectIndex++));
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

        while (foregroundObjectIndex < foregroundObjects.size()
            && compareStaticToTransform(foregroundObjects.get(foregroundObjectIndex), scene) <= 0) {
            renderStaticObject(foregroundObjects.get(foregroundObjectIndex++));
        }

        Vector2 position = scene.getPosition();
        Vector2 renderOffset = scene.getRenderOffset();
        Vector2 scaling = scene.getScaling();
        Vector2 size = scene.getSize();
        Vector2 origin = scene.getRotationOriginNormalized();
        batch.setColor(sprite.getColor());
        batch.draw(
            region,
            position.x + renderOffset.x,
            position.y + renderOffset.y,
            size.x * origin.x, size.y * origin.y,
            size.x, size.y,
            scaling.x, scaling.y,
            scene.getRotationDeg()
        );
    }

    /**
     * Ustawia mapę — zbiera warstwy kafelkowe i statyczne warstwy obiektowe jako tło.
     */
    public void setMap(TiledMap tiledMap) {
        tiledRenderer.setMap(tiledMap);
        bgdLayers.clear();
        backgroundObjects.clear();
        foregroundObjects.clear();

        for (MapLayer layer : tiledMap.getLayers()) {
            if (!layer.isVisible()) {
                continue;
            }

            if (layer instanceof TiledMapTileLayer) {
                bgdLayers.add(layer);
            } else if (isStaticBackgroundObjectLayer(layer.getName())) {
                collectStaticObjectLayer(layer);
            }
        }

        foregroundObjects.sort((left, right) -> compareSortKeys(
            left.zOrder(), left.getSortY(), left.x(),
            right.zOrder(), right.getSortY(), right.x()
        ));
    }

    private void collectStaticObjectLayer(MapLayer layer) {
        for (MapObject object : layer.getObjects()) {
            if (!(object instanceof TiledMapTileMapObject tileObject)) {
                continue;
            }

            TextureRegion region = tileObject.getTextureRegion();
            if (region == null) {
                continue;
            }

            StaticObjectRenderable renderable = buildRenderable(layer.getName(), tileObject, region);
            if (renderable == null) {
                continue;
            }

            if (renderable.foreground()) {
                foregroundObjects.add(renderable);
            } else {
                backgroundObjects.add(renderable);
            }
        }
    }

    private StaticObjectRenderable buildRenderable(String layerName,
                                                   TiledMapTileMapObject tileObject,
                                                   TextureRegion region) {
        float x = tileObject.getX() * Main.UNIT_SCALE;
        float y = tileObject.getY() * Main.UNIT_SCALE;
        float width = region.getRegionWidth() * Main.UNIT_SCALE;
        float height = region.getRegionHeight() * Main.UNIT_SCALE;
        CollisionBounds bounds = extractCollisionBounds(tileObject.getTile(), width, height);
        int zOrder = tileObject.getTile() != null
            ? tileObject.getTile().getProperties().get("z", 1, Integer.class)
            : 1;
        boolean foreground = shouldRenderInForeground(layerName, tileObject.getTile(), bounds);
        float sortOffsetY = bounds != null ? bounds.sortOffsetY() : 0f;

        return new StaticObjectRenderable(
            region,
            x,
            y,
            width,
            height,
            tileObject.getScaleX(),
            tileObject.getScaleY(),
            tileObject.getRotation(),
            zOrder,
            sortOffsetY,
            foreground
        );
    }

    private boolean shouldRenderInForeground(String layerName, TiledMapTile tile, CollisionBounds bounds) {
        if (bounds == null || tile == null) {
            return false;
        }
        if ("houses".equals(layerName)) {
            return true;
        }

        String bodyType = tile.getProperties().get("bodyType", String.class);
        return "StaticBody".equals(bodyType);
    }

    private CollisionBounds extractCollisionBounds(TiledMapTile tile, float spriteW, float spriteH) {
        if (tile == null) {
            return null;
        }

        MapObjects objects = tile.getObjects();
        if (objects == null || objects.getCount() == 0) {
            return null;
        }

        for (MapObject object : objects) {
            Boolean sensor = object.getProperties().get("sensor", false, Boolean.class);
            if (sensor) {
                continue;
            }

            Rectangle bounds = toCollisionBounds(object);
            if (bounds == null || bounds.width <= 0f || bounds.height <= 0f) {
                continue;
            }

            float spriteWpx = spriteW / Main.UNIT_SCALE;
            float spriteHpx = spriteH / Main.UNIT_SCALE;
            float collCenterYpx = bounds.y + bounds.height * 0.5f;
            float spriteCenterYpx = spriteHpx * 0.5f;
            float offsetY = (collCenterYpx - spriteCenterYpx) * Main.UNIT_SCALE;
            float halfH = bounds.height * 0.5f * Main.UNIT_SCALE;
            return new CollisionBounds(halfH, spriteH * 0.5f + offsetY - halfH);
        }

        return null;
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

    private int compareStaticToTransform(StaticObjectRenderable renderable, TransformComponent transform) {
        return compareSortKeys(
            renderable.zOrder(),
            renderable.getSortY(),
            renderable.x(),
            transform.getZOrder(),
            transform.getPosition().y + transform.getSortOffsetY(),
            transform.getPosition().x
        );
    }

    private int compareSortKeys(int leftZ, float leftSortY, float leftX,
                                int rightZ, float rightSortY, float rightX) {
        if (leftZ != rightZ) {
            return Integer.compare(leftZ, rightZ);
        }
        if (leftSortY != rightSortY) {
            return Float.compare(rightSortY, leftSortY);
        }
        return Float.compare(leftX, rightX);
    }

    private void renderStaticObject(StaticObjectRenderable renderable) {
        batch.setColor(Color.WHITE);
        batch.draw(
            renderable.region(),
            renderable.x(),
            renderable.y(),
            0f,
            0f,
            renderable.width(),
            renderable.height(),
            renderable.scaleX(),
            renderable.scaleY(),
            renderable.rotationDeg()
        );
    }

    private record CollisionBounds(float halfHeight, float sortOffsetY) {
    }

    private record StaticObjectRenderable(TextureRegion region,
                                          float x,
                                          float y,
                                          float width,
                                          float height,
                                          float scaleX,
                                          float scaleY,
                                          float rotationDeg,
                                          int zOrder,
                                          float sortOffsetY,
                                          boolean foreground) {
        private float getSortY() {
            return y + sortOffsetY;
        }
    }

    @Override
    public void dispose() {
        tiledRenderer.dispose();
    }
}

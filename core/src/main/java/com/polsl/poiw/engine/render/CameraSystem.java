package com.polsl.poiw.engine.render;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.component.CameraFollowComponent;
import com.polsl.poiw.engine.component.TransformComponent;

/**
 * System śledzący kamerą entity z CameraFollowComponent + TransformComponent.
 * Kamera płynnie podąża za entity (LERP)
 */
public class CameraSystem extends IteratingSystem {

    private static final float CAMERA_EPSILON = 0.001f;

    private final OrthographicCamera camera;
    private final float smoothingFactor;
    private final Vector2 targetPosition;
    private float mapW;
    private float mapH;

    /** Flaga pierwszej klatki — kamera od razu skacze na cel zamiast lerpować */
    private boolean firstFrame = true;

    public CameraSystem(OrthographicCamera camera) {
        super(Family.all(CameraFollowComponent.class, TransformComponent.class).get(), 99);
        this.camera = camera;
        this.smoothingFactor = 10f;
        this.targetPosition = new Vector2();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent scene = TransformComponent.MAPPER.get(entity);
        calcTargetPosition(scene.getPosition());

        if (firstFrame) {
            // Pierwsza klatka — natychmiast ustaw kamerę na graczu
            camera.position.set(targetPosition.x, targetPosition.y, camera.position.z);
            firstFrame = false;
        } else {
            // Płynne śledzenie niezależne od FPS.
            float progress = 1f - (float) Math.exp(-smoothingFactor * deltaTime);
            float smoothedX = MathUtils.lerp(camera.position.x, targetPosition.x, progress);
            float smoothedY = MathUtils.lerp(camera.position.y, targetPosition.y, progress);

            if (Math.abs(targetPosition.x - smoothedX) < CAMERA_EPSILON) {
                smoothedX = targetPosition.x;
            }
            if (Math.abs(targetPosition.y - smoothedY) < CAMERA_EPSILON) {
                smoothedY = targetPosition.y;
            }

            camera.position.set(smoothedX, smoothedY, camera.position.z);
        }
        camera.update();
    }

    /**
     * Resetuje flagę pierwszej klatki — kamera snapnie do celu przy następnym update.
     * Wywoływane np. przy zmianie poziomu.
     */
    public void resetFirstFrame() {
        this.firstFrame = true;
    }

    /**
     * Oblicza docelową pozycję kamery z ograniczeniem do granic mapy.
     * Uwzględnia aktualny rozmiar viewportu (ważne dla ExtendViewport).
     */
    private void calcTargetPosition(Vector2 entityPosition) {
        float targetX = entityPosition.x;
        float camHalfW = camera.viewportWidth * camera.zoom * 0.5f;
        if (mapW > camHalfW * 2f) {
            float min = camHalfW;
            float max = mapW - camHalfW;
            targetX = MathUtils.clamp(targetX, min, max);
        } else {
            targetX = mapW * 0.5f;
        }

        float targetY = entityPosition.y;
        float camHalfH = camera.viewportHeight * camera.zoom * 0.5f;
        if (mapH > camHalfH * 2f) {
            float min = camHalfH;
            float max = mapH - camHalfH;
            targetY = MathUtils.clamp(targetY, min, max);
        } else {
            targetY = mapH * 0.5f;
        }

        this.targetPosition.set(targetX, targetY);
    }

    /**
     * Ustawia mapę — oblicza granice kamery z properties mapy.
     */
    public void setMap(TiledMap tiledMap) {
        if (tiledMap == null) return;

        int width = tiledMap.getProperties().get("width", 0, Integer.class);
        int tileW = tiledMap.getProperties().get("tilewidth", 0, Integer.class);
        int height = tiledMap.getProperties().get("height", 0, Integer.class);
        int tileH = tiledMap.getProperties().get("tileheight", 0, Integer.class);
        mapW = width * tileW * Main.UNIT_SCALE;
        mapH = height * tileH * Main.UNIT_SCALE;
    }

    public OrthographicCamera getCamera() { return camera; }
}

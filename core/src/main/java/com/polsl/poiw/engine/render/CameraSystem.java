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

    private final OrthographicCamera camera;
    private final float smoothingFactor;
    private final Vector2 targetPosition;
    private float mapW;
    private float mapH;

    public CameraSystem(OrthographicCamera camera) {
        super(Family.all(CameraFollowComponent.class, TransformComponent.class).get(), 99);
        this.camera = camera;
        this.smoothingFactor = 4f;
        this.targetPosition = new Vector2();
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        TransformComponent scene = TransformComponent.MAPPER.get(entity);
        calcTargetPosition(scene.getPosition());

        // Płynne śledzenie (LERP)
        float progress = smoothingFactor * deltaTime;
        float smoothedX = MathUtils.lerp(camera.position.x, targetPosition.x, progress);
        float smoothedY = MathUtils.lerp(camera.position.y, targetPosition.y, progress);
        camera.position.set(smoothedX, smoothedY, camera.position.z);
        camera.update();
    }

    /**
     * Oblicza docelową pozycję kamery z ograniczeniem do granic mapy.
     */
    private void calcTargetPosition(Vector2 entityPosition) {
        float targetX = entityPosition.x;
        float camHalfW = camera.viewportWidth * 0.5f;
        if (mapW > camHalfW) {
            float min = Math.min(camHalfW, mapW - camHalfW);
            float max = Math.max(camHalfW, mapW - camHalfW);
            targetX = MathUtils.clamp(targetX, min, max);
        }

        float targetY = entityPosition.y;
        float camHalfH = camera.viewportHeight * 0.5f;
        if (mapH > camHalfH) {
            float min = Math.min(camHalfH, mapH - camHalfH);
            float max = Math.max(camHalfH, mapH - camHalfH);
            targetY = MathUtils.clamp(targetY, min, max);
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

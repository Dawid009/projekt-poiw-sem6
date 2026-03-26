package com.polsl.poiw.engine.render;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

/**
 * System debugowy rysujący kształty kolizji Box2D (czerwone prostokąty/krawędzie).
 * Domyślnie wyłączony. Przełączany klawiszem F3 z GameScreen.
 */
public class DebugRenderSystem extends EntitySystem implements Disposable {

    private final Box2DDebugRenderer debugRenderer;
    private final World box2dWorld;
    private final OrthographicCamera camera;
    private boolean debugEnabled = false;

    public DebugRenderSystem(World box2dWorld, OrthographicCamera camera) {
        // Priorytet 101 — rysuje PO RenderSystem (100)
        super(101);
        this.box2dWorld = box2dWorld;
        this.camera = camera;
        this.debugRenderer = new Box2DDebugRenderer(
            true,   // drawBodies
            false,  // drawJoints
            false,  // drawAABBs
            false,  // drawInactiveBodies
            false,  // drawVelocities
            true    // drawContacts
        );
    }

    @Override
    public void update(float deltaTime) {
        if (!debugEnabled) return;
        debugRenderer.render(box2dWorld, camera.combined);
    }

    /** Włącza/wyłącza debug rendering */
    public void setEnabled(boolean enabled) {
        this.debugEnabled = enabled;
    }

    /** Przełącza debug rendering */
    public void toggle() {
        this.debugEnabled = !debugEnabled;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public void dispose() {
        debugRenderer.dispose();
    }
}

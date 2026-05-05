package com.polsl.poiw.engine.net.prediction;

/**
 * network clock — client-side estimated server time.
 * increments by delta each frame, snaps/lerps when server
 * time arrives via movement snapshots.
 */
public class NetworkClock {

    private static final float SNAP_THRESHOLD = 1.0f; // 1s difference → snap immediately
    private static final float LERP_SPEED = 0.1f; // lerp toward server time (slow correction)

    private float renderTime = 0f;
    private float targetServerTime = 0f;
    private boolean initialized = false;

    /** called every frame */
    public void update(float delta) {
        renderTime += delta;

        // slowly lerp toward target to correct drift
        if (initialized) {
            float diff = targetServerTime - renderTime;
            if (Math.abs(diff) > SNAP_THRESHOLD) {
                // snap — too far off
                renderTime = targetServerTime;
            } else if (Math.abs(diff) > 0.001f) {
                renderTime += diff * LERP_SPEED;
            }
        }
    }

    /** called when server time arrives (from BatchMovementSnapshot) */
    public void onServerTimeReceived(float serverTime) {
        if (!initialized) {
            renderTime = serverTime;
            initialized = true;
        }
        targetServerTime = serverTime;
    }

    public float getRenderTime() { return renderTime; }
    public boolean isInitialized() { return initialized; }
}

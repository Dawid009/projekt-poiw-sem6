package com.polsl.poiw.engine.net.prediction;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * entity interpolation for SIMULATED_PROXY (remote players).
 * renders position with configurable delay behind server time,
 * interpolating between server snapshots.
 * supports short extrapolation when no new snapshot arrives.
 */
public class EntityInterpolation {

    private static final float INTERP_DELAY = 0.1f; // 100ms
    private static final float MAX_EXTRAPOLATION = 0.1f; // max 100ms extrapolation
    private static final float SNAP_THRESHOLD = 5f; // 5m — teleport threshold
    private static final int BUFFER_SIZE = 20;

    private final PositionSnapshot[] buffer = new PositionSnapshot[BUFFER_SIZE];
    private int head = 0;
    private int count = 0;

    /** reusable Vector2 — nie alokujemy co frame */
    private final Vector2 resultPos = new Vector2();

    // add a position + velocity snapshot from the server
    public void addSnapshot(float serverTime, float x, float y, float velX, float velY) {
        buffer[head] = new PositionSnapshot(serverTime, x, y, velX, velY);
        head = (head + 1) % BUFFER_SIZE;
        if (count < BUFFER_SIZE) count++;
    }

    // backward-compatible overload (no velocity)
    public void addSnapshot(float serverTime, float x, float y) {
        addSnapshot(serverTime, x, y, 0f, 0f);
    }

    /**
     * calculates interpolated position (INTERP_DELAY behind render time).
     * returns reusable Vector2 — do NOT store reference, copy if needed.
     */
    public Vector2 interpolate(float renderTime) {
        return interpolate(renderTime, true);
    }

    public Vector2 interpolate(float renderTime, boolean allowExtrapolation) {
        float targetTime = renderTime - INTERP_DELAY;

        if (count < 2) return null;

        // find two snapshots surrounding targetTime
        PositionSnapshot before = null;
        PositionSnapshot after = null;

        int start = (head - count + BUFFER_SIZE) % BUFFER_SIZE;
        for (int i = 0; i < count - 1; i++) {
            int idx = (start + i) % BUFFER_SIZE;
            int nextIdx = (start + i + 1) % BUFFER_SIZE;

            PositionSnapshot s1 = buffer[idx];
            PositionSnapshot s2 = buffer[nextIdx];

            if (s1 != null && s2 != null && s1.time <= targetTime && s2.time >= targetTime) {
                before = s1;
                after = s2;
                break;
            }
        }

        if (before != null && after != null) {
            // snap threshold — if distance between before and after > SNAP_THRESHOLD, teleport
            float dx = after.x - before.x;
            float dy = after.y - before.y;
            if (dx * dx + dy * dy > SNAP_THRESHOLD * SNAP_THRESHOLD) {
                resultPos.set(after.x, after.y);
                return resultPos;
            }

            // linear interpolation
            float span = after.time - before.time;
            float t = span > 0 ? MathUtils.clamp((targetTime - before.time) / span, 0f, 1f) : 0f;

            resultPos.set(
                MathUtils.lerp(before.x, after.x, t),
                MathUtils.lerp(before.y, after.y, t)
            );
            return resultPos;
        }

        // no bracketing snapshots — try short extrapolation from latest
        int lastIdx = (head - 1 + BUFFER_SIZE) % BUFFER_SIZE;
        PositionSnapshot last = buffer[lastIdx];
        if (last == null) return null;

        float timeSinceLast = targetTime - last.time;
        if (allowExtrapolation && timeSinceLast > 0 && timeSinceLast <= MAX_EXTRAPOLATION) {
            // extrapolate using velocity
            resultPos.set(
                last.x + last.velX * timeSinceLast,
                last.y + last.velY * timeSinceLast
            );
        } else {
            // beyond extrapolation limit — hold last known position
            resultPos.set(last.x, last.y);
        }
        return resultPos;
    }

    // clear buffer
    public void clear() {
        head = 0;
        count = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) buffer[i] = null;
    }

    public int getSnapshotCount() { return count; }

    private record PositionSnapshot(float time, float x, float y, float velX, float velY) {}
}

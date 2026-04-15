package com.polsl.poiw.engine.net.prediction;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * entity interpolation for SIMULATED_PROXY (remote players)
 * client renders position with 100ms delay, interpolating between server snapshots
 * TODO: this is a very simple implementation, update this later
 */
public class EntityInterpolation {

    private static final float INTERP_DELAY = 0.1f; // 100ms
    private static final int BUFFER_SIZE = 20;

    private final PositionSnapshot[] buffer = new PositionSnapshot[BUFFER_SIZE];
    private int head = 0;
    private int count = 0;
    private float currentTime = 0f;

    // add a position snapshot from the server
    public void addSnapshot(float serverTime, float x, float y) {
        buffer[head] = new PositionSnapshot(serverTime, x, y);
        head = (head + 1) % BUFFER_SIZE;
        if (count < BUFFER_SIZE) count++;
    }

    // calculate interpolated position (100ms in the past of server time)
    public Vector2 interpolate(float currentTime) {
        this.currentTime = currentTime;
        float renderTime = currentTime - INTERP_DELAY;

        if (count < 2) return null;

        // find two snapshots surrounding renderTime
        PositionSnapshot before = null;
        PositionSnapshot after = null;

        int start = (head - count + BUFFER_SIZE) % BUFFER_SIZE;
        for (int i = 0; i < count - 1; i++) {
            int idx = (start + i) % BUFFER_SIZE;
            int nextIdx = (start + i + 1) % BUFFER_SIZE;

            PositionSnapshot s1 = buffer[idx];
            PositionSnapshot s2 = buffer[nextIdx];

            if (s1 != null && s2 != null && s1.time <= renderTime && s2.time >= renderTime) {
                before = s1;
                after = s2;
                break;
            }
        }

        if (before == null || after == null) {
            // extrapolate from last known snapshot
            int lastIdx = (head - 1 + BUFFER_SIZE) % BUFFER_SIZE;
            PositionSnapshot last = buffer[lastIdx];
            return last != null ? new Vector2(last.x, last.y) : null;
        }

        // linear interpolation
        float span = after.time - before.time;
        float t = span > 0 ? MathUtils.clamp((renderTime - before.time) / span, 0f, 1f) : 0f;

        return new Vector2(
            MathUtils.lerp(before.x, after.x, t),
            MathUtils.lerp(before.y, after.y, t)
        );
    }

    // clear buffer
    public void clear() {
        head = 0;
        count = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) buffer[i] = null;
    }

    public int getSnapshotCount() { return count; }

    private record PositionSnapshot(float time, float x, float y) {}
}

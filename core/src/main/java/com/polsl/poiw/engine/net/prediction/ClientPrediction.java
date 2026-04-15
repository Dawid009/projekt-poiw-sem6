package com.polsl.poiw.engine.net.prediction;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.component.MovementComponent;

/**
 * client-side prediction - stores unacknowledged moves
 * and replays them after server correction
 */
public class ClientPrediction {

    private static final int MAX_SAVED = 128;
    private static final float RECONCILIATION_THRESHOLD = 0.1f; // 10cm

    private final SavedMove[] moves = new SavedMove[MAX_SAVED];
    private int head = 0;
    private int count = 0;

    // save input (after sending to server and applying locally)
    public void saveMove(int seq, float dirX, float dirY, float posX, float posY) {
        moves[head] = new SavedMove(seq, dirX, dirY, posX, posY);
        head = (head + 1) % MAX_SAVED;
        if (count < MAX_SAVED)
            count++;
    }

    /**
     * Reconciliation: server sent a position correction
     * compare predicted vs server - if discrepancy > threshold,
     * snap to server pos and replay unacknowledged moves.
     * 
     * @param serverX
     * @param serverY
     * @param lastProcessedSeq last input sequence processed by server
     * @param move             MovementComponent to reset direction for replaying
     *                         moves
     * @param body             Box2D body to correct position of
     */
    public void reconcile(float serverX, float serverY, int lastProcessedSeq,
            MovementComponent move, Body body) {
        if (body == null)
            return;

        // find predicted position for lastProcessedSeq
        int startIdx = findMoveIndex(lastProcessedSeq);
        if (startIdx < 0) {
            // not found - snap absolutely
            body.setTransform(serverX, serverY, 0);
            return;
        }

        // check discrepancy
        SavedMove savedAtSeq = getMoveAtIndex(startIdx);
        if (savedAtSeq == null) {
            body.setTransform(serverX, serverY, 0);
            return;
        }

        float dx = serverX - savedAtSeq.posX;
        float dy = serverY - savedAtSeq.posY;
        float distSq = dx * dx + dy * dy;

        if (distSq < RECONCILIATION_THRESHOLD * RECONCILIATION_THRESHOLD) {
            // prediction is accurate enough — do not correct
            return;
        }

        // snap to server position
        body.setTransform(serverX, serverY, 0);

        // replay unacknowledged moves (after lastProcessedSeq)
        replayMoves(startIdx, move, body);
    }

    private void replayMoves(int fromIdx, MovementComponent move, Body body) {
        float dt = 1f / 60f; // fixed timestep
        int idx = (fromIdx + 1) % MAX_SAVED;
        int remaining = countMovesAfter(fromIdx);

        for (int i = 0; i < remaining; i++) {
            SavedMove m = moves[idx];
            if (m == null)
                break;

            // apply movement
            float speed = move.getMaxSpeed();
            float len = (float) Math.sqrt(m.dirX * m.dirX + m.dirY * m.dirY);
            if (len > 0) {
                float nx = m.dirX / len;
                float ny = m.dirY / len;
                body.setLinearVelocity(nx * speed, ny * speed);
            } else {
                body.setLinearVelocity(0, 0);
            }

            // mini physics step (approximation)
            Vector2 pos = body.getPosition();
            Vector2 vel = body.getLinearVelocity();
            body.setTransform(pos.x + vel.x * dt, pos.y + vel.y * dt, 0);

            idx = (idx + 1) % MAX_SAVED;
        }
    }

    private int findMoveIndex(int seq) {
        int start = (head - count + MAX_SAVED) % MAX_SAVED;
        for (int i = 0; i < count; i++) {
            int idx = (start + i) % MAX_SAVED;
            if (moves[idx] != null && moves[idx].seq == seq) {
                return idx;
            }
        }
        return -1;
    }

    private SavedMove getMoveAtIndex(int idx) {
        return (idx >= 0 && idx < MAX_SAVED) ? moves[idx] : null;
    }

    private int countMovesAfter(int fromIdx) {
        if (count == 0)
            return 0;
        int end = (head - 1 + MAX_SAVED) % MAX_SAVED;
        if (fromIdx == end)
            return 0;
        return ((end - fromIdx) + MAX_SAVED) % MAX_SAVED;
    }

    // clear buffer (e.g. on level change)
    public void clear() {
        head = 0;
        count = 0;
        for (int i = 0; i < MAX_SAVED; i++)
            moves[i] = null;
    }

    private record SavedMove(int seq, float dirX, float dirY, float posX, float posY) {
    }
}

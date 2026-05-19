package com.polsl.poiw.engine.net.prediction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.component.MovementComponent;

/**
 * client-side prediction — stores unacknowledged moves
 * and compares predicted positions with server corrections.
 * snap-only reconciliation (no Box2D replay).
 */
public class ClientPrediction {

    private static final String TAG = "ClientPrediction";
    private static final int MAX_SAVED = 256;
    private static final float RECONCILIATION_THRESHOLD = 0.25f; // 25cm

    private final SavedMove[] moves = new SavedMove[MAX_SAVED];
    private int head = 0;
    private int count = 0;

    // save input (after sending to server and applying locally)
    // posX/posY = body position at the moment input was sent
    public void saveMove(int seq, float dirX, float dirY, float posX, float posY) {
        moves[head] = new SavedMove(seq, dirX, dirY, posX, posY);
        head = (head + 1) % MAX_SAVED;
        if (count < MAX_SAVED)
            count++;
    }

    /**
     * server sent authoritative position for lastProcessedSeq.
     * compare with predicted position at that seq:
     * - if close → accept prediction, discard acknowledged moves
     * - if far → snap body to server position + velocity, discard acknowledged moves
     */
    public void reconcile(float serverX, float serverY,
                          float serverVelX, float serverVelY,
                          int lastProcessedSeq, Body body) {
        if (body == null) return;

        // find predicted position for lastProcessedSeq
        int idx = findMoveIndex(lastProcessedSeq);
        if (idx >= 0) {
            SavedMove saved = moves[idx];
            float dx = serverX - saved.posX;
            float dy = serverY - saved.posY;
            float distSq = dx * dx + dy * dy;

            if (distSq < RECONCILIATION_THRESHOLD * RECONCILIATION_THRESHOLD) {
                // prediction accurate — no correction needed
                discardMovesUpTo(lastProcessedSeq);
                return;
            }
        }

        // snap to server position and velocity
        body.setTransform(serverX, serverY, 0);
        body.setLinearVelocity(serverVelX, serverVelY);
        discardMovesUpTo(lastProcessedSeq);
    }

    private void discardMovesUpTo(int seq) {
        // remove all saved moves with seq <= lastProcessedSeq
        int start = (head - count + MAX_SAVED) % MAX_SAVED;
        int discarded = 0;
        for (int i = 0; i < count; i++) {
            int idx = (start + i) % MAX_SAVED;
            if (moves[idx] != null && moves[idx].seq <= seq) {
                moves[idx] = null;
                discarded++;
            } else {
                break; // sequences are ordered
            }
        }
        count -= discarded;
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

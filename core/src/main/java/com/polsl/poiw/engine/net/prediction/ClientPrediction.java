package com.polsl.poiw.engine.net.prediction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Przechowuje lokalnie wysłane inputy i porównuje je z korektami z serwera.
 */
public class ClientPrediction {

    private static final String TAG = "ClientPrediction";
    private static final int MAX_SAVED = 256;
    private static final int SEQUENCE_SPACE = 1 << 30;
    private static final int HALF_SEQUENCE_SPACE = SEQUENCE_SPACE / 2;
    private static final float SMALL_CORRECTION_THRESHOLD = 0.12f;
    private static final float SOFT_CORRECTION_THRESHOLD = 0.65f;

    private final SavedMove[] moves = new SavedMove[MAX_SAVED];
    private int head = 0;
    private int count = 0;
    private int softCorrectionCount = 0;
    private int snapCorrectionCount = 0;

    public void saveMove(int seq,
                         float dirX,
                         float dirY,
                         boolean sprinting,
                         float posX,
                         float posY,
                         float velX,
                         float velY) {
        moves[head] = new SavedMove(seq, dirX, dirY, sprinting, posX, posY, velX, velY);
        head = (head + 1) % MAX_SAVED;
        if (count < MAX_SAVED) {
            count++;
        }
    }

    public void reconcile(float serverX, float serverY,
                          float serverVelX, float serverVelY,
                          int lastProcessedSeq, Body body) {
        if (body == null) return;

        int idx = findMoveIndex(lastProcessedSeq);
        if (idx >= 0) {
            SavedMove saved = moves[idx];
            float dx = serverX - saved.predictedX;
            float dy = serverY - saved.predictedY;
            float distSq = dx * dx + dy * dy;
            float smallThresholdSq = SMALL_CORRECTION_THRESHOLD * SMALL_CORRECTION_THRESHOLD;
            float softThresholdSq = SOFT_CORRECTION_THRESHOLD * SOFT_CORRECTION_THRESHOLD;

            if (distSq <= smallThresholdSq) {
                discardMovesUpTo(lastProcessedSeq);
                return;
            }

            if (distSq <= softThresholdSq) {
                Vector2 currentPos = body.getPosition();
                body.setTransform(currentPos.x + dx, currentPos.y + dy, body.getAngle());
                body.setLinearVelocity(serverVelX, serverVelY);
                discardMovesUpTo(lastProcessedSeq);
                logSoftCorrection(lastProcessedSeq, (float) Math.sqrt(distSq));
                return;
            }
        }

        float snapDistance = idx >= 0 ? distanceToSaved(idx, serverX, serverY) : -1f;
        body.setTransform(serverX, serverY, body.getAngle());
        body.setLinearVelocity(serverVelX, serverVelY);
        discardMovesUpTo(lastProcessedSeq);
        logSnapCorrection(lastProcessedSeq, snapDistance);
    }

    private void discardMovesUpTo(int seq) {
        int start = (head - count + MAX_SAVED) % MAX_SAVED;
        int discarded = 0;
        for (int i = 0; i < count; i++) {
            int idx = (start + i) % MAX_SAVED;
            if (moves[idx] != null && isSequenceAtOrBefore(moves[idx].seq, seq)) {
                moves[idx] = null;
                discarded++;
            } else {
                break;
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

    public void clear() {
        head = 0;
        count = 0;
        softCorrectionCount = 0;
        snapCorrectionCount = 0;
        for (int i = 0; i < MAX_SAVED; i++) {
            moves[i] = null;
        }
    }

    private boolean isSequenceAtOrBefore(int seq, int reference) {
        if (seq == reference) {
            return true;
        }

        int diff = (reference - seq + SEQUENCE_SPACE) % SEQUENCE_SPACE;
        return diff > 0 && diff < HALF_SEQUENCE_SPACE;
    }

    private float distanceToSaved(int idx, float serverX, float serverY) {
        SavedMove saved = moves[idx];
        if (saved == null) {
            return -1f;
        }
        float dx = serverX - saved.predictedX;
        float dy = serverY - saved.predictedY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void logSoftCorrection(int seq, float distance) {
        softCorrectionCount++;
        if (softCorrectionCount <= 5 || softCorrectionCount % 25 == 0) {
            Gdx.app.debug(TAG, "Soft correction for seq=" + seq + " dist=" + distance
                + " count=" + softCorrectionCount);
        }
    }

    private void logSnapCorrection(int seq, float distance) {
        snapCorrectionCount++;
        if (snapCorrectionCount <= 5 || snapCorrectionCount % 10 == 0) {
            Gdx.app.debug(TAG, "Snap correction for seq=" + seq + " dist=" + distance
                + " count=" + snapCorrectionCount);
        }
    }

    private record SavedMove(
        int seq,
        float dirX,
        float dirY,
        boolean sprinting,
        float predictedX,
        float predictedY,
        float predictedVelX,
        float predictedVelY
    ) {
    }
}

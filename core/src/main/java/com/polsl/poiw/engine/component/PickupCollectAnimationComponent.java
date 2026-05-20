package com.polsl.poiw.engine.component;

import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;

public class PickupCollectAnimationComponent extends AbstractActorComponent {

    @Replicated
    private boolean collecting;

    @Replicated
    private float targetX;

    @Replicated
    private float targetY;

    @Replicated
    private float durationSeconds;

    @Replicated
    @RepNotify("onCollectSequenceChanged")
    private int collectSequence;

    private transient boolean startTriggered;

    public PickupCollectAnimationComponent() {
        setReplicated(true);
    }

    public void startCollection(float targetX, float targetY, float durationSeconds) {
        if (collecting) {
            return;
        }

        this.collecting = true;
        this.targetX = targetX;
        this.targetY = targetY;
        this.durationSeconds = durationSeconds;
        this.collectSequence += 1;
        this.startTriggered = true;

        markDirty("collecting");
        markDirty("targetX");
        markDirty("targetY");
        markDirty("durationSeconds");
        markDirty("collectSequence");
    }

    public boolean isCollecting() {
        return collecting;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public boolean consumeStartTrigger() {
        boolean triggered = startTriggered;
        startTriggered = false;
        return triggered;
    }

    @SuppressWarnings("unused")
    private void onCollectSequenceChanged() {
        startTriggered = true;
    }
}
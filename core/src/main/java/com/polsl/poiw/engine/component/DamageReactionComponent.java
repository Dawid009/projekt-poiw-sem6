package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;

public class DamageReactionComponent extends AbstractActorComponent {
    public static final ComponentMapper<DamageReactionComponent> MAPPER =
        ComponentMapper.getFor(DamageReactionComponent.class);

    @Replicated
    @RepNotify("onReactionCounterChanged")
    private int reactionCounter;

    private transient int consumedReactionCounter;

    public DamageReactionComponent() {
        setReplicated(true);
    }

    public void triggerReaction() {
        if (getOwner() != null && !getOwner().hasAuthority()) {
            return;
        }

        reactionCounter += 1;
        markDirty("reactionCounter");
    }

    public boolean consumeReactionTrigger() {
        if (reactionCounter <= consumedReactionCounter) {
            return false;
        }

        consumedReactionCounter = reactionCounter;
        return true;
    }

    @SuppressWarnings("unused")
    private void onReactionCounterChanged() {
    }
}
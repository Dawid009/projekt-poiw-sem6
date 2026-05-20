package com.polsl.poiw.gameplay.actor;

import com.polsl.poiw.engine.auth.GameplayStatsBridge;
import com.polsl.poiw.gameplay.item.GameplayItems;

public class PigActor extends AbstractCreatureActor {
    @Override
    protected String getIdleRegionName() {
        return "pig/pig_idle_left";
    }

    @Override
    protected String getWalkRegionName() {
        return "pig/pig_walk_left";
    }

    @Override
    protected float getMoveSpeed() {
        return 1.05f;
    }

    @Override
    protected void onBeforeDestroy() {
        spawnItemDrops(GameplayItems.STEAK_RAW, 1, 2);
    }

    @Override
    protected void onDeathObserved() {
        GameplayStatsBridge.recordAnimalKill(this);
    }
}
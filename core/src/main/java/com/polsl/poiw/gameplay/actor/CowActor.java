package com.polsl.poiw.gameplay.actor;

import com.polsl.poiw.engine.auth.GameplayStatsBridge;
import com.polsl.poiw.gameplay.item.GameplayItems;

public class CowActor extends AbstractCreatureActor {
    @Override
    protected String getIdleRegionName() {
        return "cow/cow_idle_left";
    }

    @Override
    protected String getWalkRegionName() {
        return "cow/cow_walk_left";
    }

    @Override
    protected float getMoveSpeed() {
        return 0.95f;
    }

    @Override
    protected float getWanderRadius() {
        return 3.4f;
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
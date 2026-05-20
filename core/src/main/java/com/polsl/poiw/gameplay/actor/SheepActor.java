package com.polsl.poiw.gameplay.actor;

import com.polsl.poiw.engine.auth.GameplayStatsBridge;
import com.polsl.poiw.gameplay.item.GameplayItems;

public class SheepActor extends AbstractCreatureActor {
    @Override
    protected String getIdleRegionName() {
        return "sheep/sheep_idle_left";
    }

    @Override
    protected String getWalkRegionName() {
        return "sheep/sheep_walk_left";
    }

    @Override
    protected float getMoveSpeed() {
        return 1.0f;
    }

    @Override
    protected float getWanderRadius() {
        return 3.2f;
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
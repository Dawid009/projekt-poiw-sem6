package com.polsl.poiw.gameplay.actor;

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
}
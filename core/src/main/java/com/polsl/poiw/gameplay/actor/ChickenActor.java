package com.polsl.poiw.gameplay.actor;

public class ChickenActor extends AbstractCreatureActor {
    @Override
    protected String getIdleRegionName() {
        return "chicken/chicken_idle_left";
    }

    @Override
    protected String getWalkRegionName() {
        return "chicken/chicken_walk_left";
    }

    @Override
    protected float getMoveSpeed() {
        return 1.2f;
    }

    @Override
    protected float getWanderRadius() {
        return 2.3f;
    }
}
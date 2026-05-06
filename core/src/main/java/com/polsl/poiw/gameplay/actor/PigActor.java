package com.polsl.poiw.gameplay.actor;

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
}
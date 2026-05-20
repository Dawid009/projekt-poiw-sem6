package com.polsl.poiw.gameplay.actor;

import com.polsl.poiw.gameplay.item.GameplayItems;

public class SlimeActor extends AbstractCreatureActor {
    @Override
    protected String getIdleRegionName() {
        return "slime_green/slime_green_idle";
    }

    @Override
    protected String getWalkRegionName() {
        return "slime_green/slime_green_jump";
    }

    @Override
    protected float getMoveSpeed() {
        return 0.85f;
    }

    @Override
    protected float getWanderRadius() {
        return 2.8f;
    }

    @Override
    protected void onBeforeDestroy() {
        spawnItemDrops(GameplayItems.COIN_GOLD, 1, 1);
        spawnItemDrops(GameplayItems.COIN_SILVER, 1, 3);
        spawnItemDrops(GameplayItems.COIN_BRONZE, 1, 5);
    }
}
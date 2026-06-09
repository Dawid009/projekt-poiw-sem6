package com.polsl.poiw.gameplay.actor;

import com.polsl.poiw.engine.auth.GameplayStatsBridge;
import com.polsl.poiw.gameplay.item.GameplayItems;

public class SkeletonActor extends AbstractCreatureActor {
    @Override
    protected String getIdleRegionName() {
        return "skeleton/skeleton_idle_right";
    }

    @Override
    protected String getWalkRegionName() {
        return "skeleton/skeleton_walk_right";
    }

    @Override
    protected boolean usesRightFacingSourceFrames() {
        return true;
    }

    @Override
    protected float getMoveSpeed() {
        return 1.15f;
    }

    @Override
    protected float getWanderRadius() {
        return 4.2f;
    }

    @Override
    protected boolean isAggressiveToPlayers() {
        return true;
    }

    @Override
    protected float getChaseRadius() {
        return 7.5f;
    }

    @Override
    protected float getContactDamageAmount() {
        return 12f;
    }

    @Override
    protected void onBeforeDestroy() {
        spawnItemDrops(GameplayItems.COIN_SILVER, 1, 3);
        spawnItemDrops(GameplayItems.COIN_BRONZE, 1, 5);
        spawnItemDrops(GameplayItems.HEAL_POTION, 0, 2);
    }

    @Override
    protected void onDeathObserved() {
        GameplayStatsBridge.recordEnemyKill(this);
    }
}

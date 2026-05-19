package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.gameplay.item.GameplayItems;

import java.util.Map;

public class CropActor extends AbstractTiledTargetActor {

    private CropKind cropKind;
    private int growthStage;
    private int[] growthTileGids = new int[0];
    private float growthIntervalSeconds = CropKind.CARROT.getGrowthIntervalSeconds();
    private float growthTimer;

    public void configure(TiledMap map,
                          int tileGid,
                          TextureRegion region,
                          CropKind cropKind,
                          int growthStage,
                          int[] growthTileGids,
                          float sizeW,
                          float sizeH,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          float sortOffsetY,
                          int zOrder,
                          float growthIntervalSeconds,
                          float maxHealth) {
        this.cropKind = cropKind;
        this.growthStage = growthStage;
        this.growthTileGids = growthTileGids.clone();
        this.growthIntervalSeconds = growthIntervalSeconds;
        this.growthTimer = 0f;

        super.configure(map, tileGid, region, sizeW, sizeH, collHalfW, collHalfH,
            collOffset, sortOffsetY, zOrder, maxHealth, maxHealth);
        configureCropCollision();
    }

    public void configureServer(int tileGid,
                                CropKind cropKind,
                                int growthStage,
                                int[] growthTileGids,
                                float sizeW,
                                float sizeH,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                float sortOffsetY,
                                int zOrder,
                                float growthIntervalSeconds,
                                float maxHealth) {
        this.cropKind = cropKind;
        this.growthStage = growthStage;
        this.growthTileGids = growthTileGids.clone();
        this.growthIntervalSeconds = growthIntervalSeconds;
        this.growthTimer = 0f;

        super.configureServer(tileGid, sizeW, sizeH, collHalfW, collHalfH,
            collOffset, sortOffsetY, zOrder, maxHealth, maxHealth);
        configureCropCollision();
    }

    @Override
    public void configureFromReplication(TiledMap map, Map<String, Object> initialProperties) {
        super.configureFromReplication(map, initialProperties);
        configureCropCollision();
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null && !health.isAlive()) {
            return;
        }
        if (!hasAuthority() || growthTileGids.length == 0 || growthStage >= growthTileGids.length - 1) {
            return;
        }

        growthTimer += delta;
        if (growthTimer < growthIntervalSeconds) {
            return;
        }

        growthTimer = 0f;
        growthStage = Math.min(growthStage + 1, growthTileGids.length - 1);
        setTileGid(growthTileGids[growthStage]);
    }

    public CropKind getCropKind() {
        return cropKind;
    }

    @Override
    protected void onBeforeDestroy() {
        if (cropKind == null || growthTileGids.length == 0 || growthStage < growthTileGids.length - 1) {
            return;
        }

        switch (cropKind) {
            case CARROT -> {
                spawnItemDrops(GameplayItems.ITEM_CARROT, 1, 2);
                spawnItemDrops(GameplayItems.SEEDS_CARROT, 1, 3);
            }
            case WHEAT -> {
                spawnItemDrops(GameplayItems.ITEM_WHEAT, 1, 2);
                spawnItemDrops(GameplayItems.SEEDS_WHEAT, 1, 3);
            }
        }
    }

    private void configureCropCollision() {
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        if (collision != null) {
            collision.setSensorOverride(true);
        }
    }
}
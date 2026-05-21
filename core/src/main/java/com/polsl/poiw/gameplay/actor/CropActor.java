package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.save.SaveGameData;
import com.polsl.poiw.gameplay.item.GameplayItems;

import java.util.Map;

public class CropActor extends AbstractTiledTargetActor {
    public static final String PROP_CROP_KIND = "cropKind";
    public static final String PROP_GROWTH_STAGE = "growthStage";
    public static final String PROP_GROWTH_INTERVAL_SECONDS = "growthIntervalSeconds";

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
        normalizeGrowthState(tileGid);
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
        normalizeGrowthState(tileGid);
        configureCropCollision();
    }

    @Override
    public Map<String, Object> buildInitialReplicationProperties() {
        Map<String, Object> properties = super.buildInitialReplicationProperties();
        if (cropKind != null) {
            properties.put(PROP_CROP_KIND, cropKind.name());
        }
        properties.put(PROP_GROWTH_STAGE, growthStage);
        properties.put(PROP_GROWTH_INTERVAL_SECONDS, growthIntervalSeconds);
        return properties;
    }

    @Override
    public void configureFromReplication(TiledMap map, Map<String, Object> initialProperties) {
        super.configureFromReplication(map, initialProperties);
        String cropKindValue = initialProperties != null ? (String) initialProperties.get(PROP_CROP_KIND) : null;
        cropKind = parseCropKind(cropKindValue);
        growthStage = getInt(initialProperties, PROP_GROWTH_STAGE, 0);
        growthIntervalSeconds = getFloat(initialProperties, PROP_GROWTH_INTERVAL_SECONDS,
            cropKind != null ? cropKind.getGrowthIntervalSeconds() : CropKind.CARROT.getGrowthIntervalSeconds());
        growthTimer = 0f;
        growthTileGids = cropKind != null
            ? cropKind.toGlobalStageTileIds(getTileGid(), growthStage)
            : new int[0];
        normalizeGrowthState(getTileGid());
        configureCropCollision();
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        refreshGrowthStageFromTile();

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
        setTileGid(growthTileGids[Math.min(growthStage + 1, growthTileGids.length - 1)]);
        refreshGrowthStageFromTile();
    }

    public CropKind getCropKind() {
        return cropKind;
    }

    public int getGrowthStage() {
        refreshGrowthStageFromTile();
        return growthStage;
    }

    public float getGrowthIntervalSeconds() {
        return growthIntervalSeconds;
    }

    public float getGrowthTimer() {
        return growthTimer;
    }

    public void setGrowthTimer(float growthTimer) {
        this.growthTimer = Math.max(0f, growthTimer);
    }

    public SaveGameData.CropData buildSaveData() {
        SaveGameData.CropData data = new SaveGameData.CropData();
        TransformComponent transform = getComponent(TransformComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        HealthComponent health = getComponent(HealthComponent.class);

        refreshGrowthStageFromTile();
        data.cropKind = cropKind != null ? cropKind.name() : "";
        data.tileGid = getTileGid();
        data.growthStage = growthStage;
        data.growthIntervalSeconds = growthIntervalSeconds;
        data.growthTimer = growthTimer;
        if (transform != null) {
            data.x = transform.getPosition().x;
            data.y = transform.getPosition().y;
            data.sizeW = transform.getSize().x;
            data.sizeH = transform.getSize().y;
            data.sortOffsetY = transform.getSortOffsetY();
            data.zOrder = transform.getZOrder();
        }
        if (collision != null) {
            data.collHalfW = collision.getHalfWidth();
            data.collHalfH = collision.getHalfHeight();
            data.collOffsetX = collision.getOffset().x;
            data.collOffsetY = collision.getOffset().y;
        }
        if (health != null) {
            data.maxHealth = health.getMaxHealth();
            data.currentHealth = health.getCurrentHealth();
        }
        return data;
    }

    @Override
    protected void onBeforeDestroy() {
        refreshGrowthStageFromTile();
        if (cropKind == null || growthTileGids.length == 0) {
            return;
        }

        int currentStage = resolveStageFromCurrentTile();
        boolean mature = currentStage >= growthTileGids.length - 1;

        switch (cropKind) {
            case CARROT -> {
                if (mature) {
                    spawnItemDrops(GameplayItems.ITEM_CARROT, 1, 2);
                    spawnItemDrops(GameplayItems.SEEDS_CARROT, 1, 3);
                } else {
                    spawnItemDrops(GameplayItems.SEEDS_CARROT, 1, 2);
                }
            }
            case WHEAT -> {
                if (mature) {
                    spawnItemDrops(GameplayItems.ITEM_WHEAT, 1, 2);
                    spawnItemDrops(GameplayItems.SEEDS_WHEAT, 1, 3);
                } else {
                    spawnItemDrops(GameplayItems.SEEDS_WHEAT, 1, 2);
                }
            }
        }
    }

    private void configureCropCollision() {
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        if (collision != null) {
            collision.setSensorOverride(true);
        }
    }

    private void normalizeGrowthState(int tileGid) {
        if (cropKind == null) {
            cropKind = CropKind.CARROT;
        }

        if (growthTileGids == null || growthTileGids.length == 0) {
            growthTileGids = cropKind.toGlobalStageTileIds(tileGid, growthStage);
        }

        refreshGrowthStageFromTile();
    }

    private void refreshGrowthStageFromTile() {
        growthStage = resolveStageFromCurrentTile();
    }

    private int resolveStageFromCurrentTile() {
        if (growthTileGids == null || growthTileGids.length == 0) {
            return 0;
        }

        int tileGid = getTileGid();
        for (int stageIndex = 0; stageIndex < growthTileGids.length; stageIndex++) {
            if (growthTileGids[stageIndex] == tileGid) {
                return stageIndex;
            }
        }

        return Math.max(0, Math.min(growthStage, growthTileGids.length - 1));
    }

    private CropKind parseCropKind(String value) {
        if (value == null || value.isBlank()) {
            return CropKind.CARROT;
        }

        try {
            return CropKind.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return CropKind.fromMetadata(value);
        }
    }

    private float getFloat(Map<String, Object> properties, String key, float defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return defaultValue;
    }

    private int getInt(Map<String, Object> properties, String key, int defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}
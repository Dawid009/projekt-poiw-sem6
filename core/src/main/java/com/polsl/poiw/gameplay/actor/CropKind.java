package com.polsl.poiw.gameplay.actor;

import java.util.Arrays;

public enum CropKind {
    CARROT("carrot", new int[]{10, 11, 17, 18}, 1f, 18f),
    WHEAT("wheat", new int[]{12, 13, 19, 20}, 1f, 22f);

    private final String metadataValue;
    private final int[] stageLocalTileIds;
    private final float maxHealth;
    private final float growthIntervalSeconds;

    CropKind(String metadataValue, int[] stageLocalTileIds, float maxHealth, float growthIntervalSeconds) {
        this.metadataValue = metadataValue;
        this.stageLocalTileIds = stageLocalTileIds;
        this.maxHealth = maxHealth;
        this.growthIntervalSeconds = growthIntervalSeconds;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public float getGrowthIntervalSeconds() {
        return growthIntervalSeconds;
    }

    public int getStageCount() {
        return stageLocalTileIds.length;
    }

    public int getLocalTileIdForStage(int stage) {
        int clampedStage = Math.max(0, Math.min(stage, stageLocalTileIds.length - 1));
        return stageLocalTileIds[clampedStage];
    }

    public int[] toGlobalStageTileIds(int currentGlobalTileGid, int currentStage) {
        int clampedStage = Math.max(0, Math.min(currentStage, stageLocalTileIds.length - 1));
        int firstGid = currentGlobalTileGid - stageLocalTileIds[clampedStage];
        int[] result = new int[stageLocalTileIds.length];
        for (int i = 0; i < stageLocalTileIds.length; i++) {
            result[i] = firstGid + stageLocalTileIds[i];
        }
        return result;
    }

    public static CropKind fromMetadata(String metadataValue) {
        if (metadataValue != null) {
            for (CropKind kind : values()) {
                if (kind.metadataValue.equalsIgnoreCase(metadataValue)) {
                    return kind;
                }
            }
        }

        return CARROT;
    }

    public int[] getStageLocalTileIds() {
        return Arrays.copyOf(stageLocalTileIds, stageLocalTileIds.length);
    }
}
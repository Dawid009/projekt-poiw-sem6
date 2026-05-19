package com.polsl.poiw.gameplay.actor;

public enum MineableKind {
    IRON("iron", 45f),
    GOLD("gold", 70f);

    private final String metadataValue;
    private final float maxHealth;

    MineableKind(String metadataValue, float maxHealth) {
        this.metadataValue = metadataValue;
        this.maxHealth = maxHealth;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public static MineableKind fromMetadata(String metadataValue) {
        if (metadataValue != null) {
            for (MineableKind kind : values()) {
                if (kind.metadataValue.equalsIgnoreCase(metadataValue)) {
                    return kind;
                }
            }
        }

        return IRON;
    }
}
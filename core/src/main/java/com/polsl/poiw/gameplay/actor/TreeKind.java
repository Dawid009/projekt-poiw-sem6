package com.polsl.poiw.gameplay.actor;

public enum TreeKind {
    OAK("oak", 55f, 4, 5),
    SMALL("small", 30f, 3, 4);

    private final String metadataValue;
    private final float maxHealth;
    private final int minLogs;
    private final int maxLogs;

    TreeKind(String metadataValue, float maxHealth, int minLogs, int maxLogs) {
        this.metadataValue = metadataValue;
        this.maxHealth = maxHealth;
        this.minLogs = minLogs;
        this.maxLogs = maxLogs;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public int getMinLogs() {
        return minLogs;
    }

    public int getMaxLogs() {
        return maxLogs;
    }

    public static TreeKind fromProperties(String treeTypeValue, String treeSizeValue) {
        if (treeTypeValue != null && !treeTypeValue.isBlank()) {
            return fromMetadata(treeTypeValue);
        }
        if (treeSizeValue != null && treeSizeValue.equalsIgnoreCase("small")) {
            return SMALL;
        }
        return OAK;
    }

    public static TreeKind fromMetadata(String metadataValue) {
        if (metadataValue != null) {
            for (TreeKind kind : values()) {
                if (kind.metadataValue.equalsIgnoreCase(metadataValue)) {
                    return kind;
                }
            }
        }

        return OAK;
    }
}
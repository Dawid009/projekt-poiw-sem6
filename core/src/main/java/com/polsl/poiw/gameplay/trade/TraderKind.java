package com.polsl.poiw.gameplay.trade;

public enum TraderKind {
    FARMER("farmer", "Farmer", "farmer/idle/idle_down"),
    MINER("miner", "Miner", "miner/idle/idle_down");

    private final String id;
    private final String displayName;
    private final String idleAnimationRegion;

    TraderKind(String id, String displayName, String idleAnimationRegion) {
        this.id = id;
        this.displayName = displayName;
        this.idleAnimationRegion = idleAnimationRegion;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdleAnimationRegion() {
        return idleAnimationRegion;
    }

    public static TraderKind fromId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        for (TraderKind kind : values()) {
            if (kind.id.equalsIgnoreCase(rawValue.trim())) {
                return kind;
            }
        }
        return null;
    }
}

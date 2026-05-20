package com.polsl.poiw.gameplay.tool;

public enum PlayerToolType {
    SWORD("tools/sword"),
    AXE("tools/axe"),
    PICKAXE("tools/pickaxe"),
    HOE("tools/hoe"),
    WATERING_CAN("tools/watering_can");

    private final String iconRegionName;

    PlayerToolType(String iconRegionName) {
        this.iconRegionName = iconRegionName;
    }

    public String getIconRegionName() {
        return iconRegionName;
    }

    public PlayerToolType next() {
        PlayerToolType[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public PlayerToolType previous() {
        PlayerToolType[] values = values();
        return values[(ordinal() - 1 + values.length) % values.length];
    }

    public static PlayerToolType fromOrdinal(int ordinal) {
        PlayerToolType[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return SWORD;
        }
        return values[ordinal];
    }
}
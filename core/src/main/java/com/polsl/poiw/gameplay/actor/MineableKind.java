package com.polsl.poiw.gameplay.actor;

import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.item.GameplayItems;

public enum MineableKind {
    IRON("iron", 45f, GameplayItems.IRON_ORE),
    GOLD("gold", 70f, GameplayItems.GOLD_ORE);

    private final String metadataValue;
    private final float maxHealth;
    private final ItemDefinition dropItem;

    MineableKind(String metadataValue, float maxHealth, ItemDefinition dropItem) {
        this.metadataValue = metadataValue;
        this.maxHealth = maxHealth;
        this.dropItem = dropItem;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public ItemDefinition getDropItem() {
        return dropItem;
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
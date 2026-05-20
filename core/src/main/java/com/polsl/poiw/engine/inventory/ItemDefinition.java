package com.polsl.poiw.engine.inventory;

import com.badlogic.gdx.graphics.Color;

import java.util.Objects;

public final class ItemDefinition {

    private final String itemId;
    private final ItemType type;
    private final String displayName;
    private final String description;
    private final String textureRegionName;
    private final Color displayColor;
    private final ItemQuality quality;
    private final int maxStack;
    private final boolean consumable;
    private final float healthRestoreAmount;

    private ItemDefinition(Builder builder) {
        this.itemId = builder.itemId;
        this.type = builder.type;
        this.displayName = builder.displayName;
        this.description = builder.description;
        this.textureRegionName = builder.textureRegionName;
        this.displayColor = new Color(builder.displayColor);
        this.quality = builder.quality;
        this.maxStack = builder.maxStack;
        this.consumable = builder.consumable;
        this.healthRestoreAmount = builder.healthRestoreAmount;
    }

    public static Builder builder(String itemId, ItemType type, String displayName) {
        return new Builder(itemId, type, displayName);
    }

    public String getItemId() {
        return itemId;
    }

    public ItemType getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public String getTextureRegionName() {
        return textureRegionName;
    }

    public Color getDisplayColor() {
        return new Color(displayColor);
    }

    public ItemQuality getQuality() {
        return quality;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public boolean isConsumable() {
        return consumable;
    }

    public float getHealthRestoreAmount() {
        return healthRestoreAmount;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ItemDefinition other)) {
            return false;
        }
        return Objects.equals(itemId, other.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    public static final class Builder {

        private final String itemId;
        private final ItemType type;
        private final String displayName;
        private String description = "";
        private String textureRegionName;
        private Color displayColor = Color.WHITE.cpy();
        private ItemQuality quality = ItemQuality.COMMON;
        private int maxStack = 99;
        private boolean consumable;
        private float healthRestoreAmount;

        private Builder(String itemId, ItemType type, String displayName) {
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("itemId cannot be blank");
            }
            if (displayName == null || displayName.isBlank()) {
                throw new IllegalArgumentException("displayName cannot be blank");
            }
            this.itemId = itemId;
            this.type = Objects.requireNonNull(type, "type cannot be null");
            this.displayName = displayName;
        }

        public Builder description(String description) {
            this.description = description != null ? description : "";
            return this;
        }

        public Builder textureRegionName(String textureRegionName) {
            this.textureRegionName = textureRegionName;
            return this;
        }

        public Builder displayColor(Color displayColor) {
            this.displayColor = displayColor != null ? displayColor.cpy() : Color.WHITE.cpy();
            return this;
        }

        public Builder quality(ItemQuality quality) {
            this.quality = quality != null ? quality : ItemQuality.COMMON;
            return this;
        }

        public Builder maxStack(int maxStack) {
            if (maxStack <= 0) {
                throw new IllegalArgumentException("maxStack must be > 0");
            }
            this.maxStack = maxStack;
            return this;
        }

        public Builder consumable(boolean consumable) {
            this.consumable = consumable;
            return this;
        }

        public Builder healthRestoreAmount(float healthRestoreAmount) {
            this.healthRestoreAmount = Math.max(0f, healthRestoreAmount);
            return this;
        }

        public ItemDefinition build() {
            return new ItemDefinition(this);
        }
    }
}
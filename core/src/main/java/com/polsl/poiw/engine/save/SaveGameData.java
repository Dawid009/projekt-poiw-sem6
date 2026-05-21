package com.polsl.poiw.engine.save;

import java.util.ArrayList;
import java.util.List;

public class SaveGameData {
    public static final int CURRENT_VERSION = 1;

    public int version = CURRENT_VERSION;
    public String levelId = "game";
    public long lastPlayedEpochMillis;
    public float totalPlayTimeSeconds;
    public PlayerData player = new PlayerData();
    public List<TreeData> trees = new ArrayList<>();
    public List<MineableData> mineables = new ArrayList<>();
    public List<CreatureData> creatures = new ArrayList<>();
    public List<CropData> crops = new ArrayList<>();
    public List<VisualData> visuals = new ArrayList<>();
    public List<ItemPickupData> itemPickups = new ArrayList<>();

    public static class PlayerData {
        public float x;
        public float y;
        public float maxHealth;
        public float currentHealth;
        public int activeToolOrdinal;
        public String assignedItemId = "";
        public List<InventoryEntryData> inventory = new ArrayList<>();
    }

    public static class InventoryEntryData {
        public String itemId = "";
        public int quantity;
    }

    public static class TreeData {
        public String treeKind = "";
        public int tileGid;
        public float x;
        public float y;
        public float sizeW;
        public float sizeH;
        public float collHalfW;
        public float collHalfH;
        public float collOffsetX;
        public float collOffsetY;
        public float sortOffsetY;
        public int zOrder;
        public float maxHealth;
        public float currentHealth;
        public int stumpTileGid;
        public float stumpWidth;
        public float stumpHeight;
        public float stumpCollHalfW;
        public float stumpCollHalfH;
        public float stumpCollOffsetX;
        public float stumpCollOffsetY;
    }

    public static class MineableData {
        public String mineableKind = "";
        public int tileGid;
        public float x;
        public float y;
        public float sizeW;
        public float sizeH;
        public float collHalfW;
        public float collHalfH;
        public float collOffsetX;
        public float collOffsetY;
        public float sortOffsetY;
        public int zOrder;
        public float maxHealth;
        public float currentHealth;
        public int minDropCount;
        public int maxDropCount;
    }

    public static class CreatureData {
        public String creatureKind = "";
        public float x;
        public float y;
        public float sizeW;
        public float sizeH;
        public float collHalfW;
        public float collHalfH;
        public float collOffsetX;
        public float collOffsetY;
        public float sortOffsetY;
        public int zOrder;
        public float maxHealth;
        public float currentHealth;
    }

    public static class CropData {
        public String cropKind = "";
        public int tileGid;
        public int growthStage;
        public float growthIntervalSeconds;
        public float growthTimer;
        public float x;
        public float y;
        public float sizeW;
        public float sizeH;
        public float collHalfW;
        public float collHalfH;
        public float collOffsetX;
        public float collOffsetY;
        public float sortOffsetY;
        public int zOrder;
        public float maxHealth;
        public float currentHealth;
    }

    public static class VisualData {
        public int tileGid;
        public float x;
        public float y;
        public float sizeW;
        public float sizeH;
        public float sortOffsetY;
        public int zOrder;
        public float collHalfW;
        public float collHalfH;
        public float collOffsetX;
        public float collOffsetY;
    }

    public static class ItemPickupData {
        public String itemId = "";
        public int quantity;
        public float x;
        public float y;
    }
}
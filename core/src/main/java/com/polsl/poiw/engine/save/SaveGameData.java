package com.polsl.poiw.engine.save;

import java.util.ArrayList;
import java.util.List;

/**
 * Surowe dane jednego savegame.
 * Trzyma tylko stan potrzebny do odtworzenia świata w singleplayerze.
 */
public class SaveGameData {
    public static final int CURRENT_VERSION = 2;

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
    public List<ChestData> chests = new ArrayList<>();
    public List<ItemPickupData> itemPickups = new ArrayList<>();

    /** Stan gracza zapisywany razem ze światem. */
    public static class PlayerData {
        public float x;
        public float y;
        public float maxHealth;
        public float currentHealth;
        public int activeToolOrdinal;
        public String assignedItemId = "";
        public List<InventoryEntryData> inventory = new ArrayList<>();
    }

    /** Jeden wpis inventory bez dodatkowej logiki UI. */
    public static class InventoryEntryData {
        public String itemId = "";
        public int quantity;
    }

    /** Zapis pojedynczego drzewa, także po ścięciu do pniaka. */
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

    /** Zapis kopalnego obiektu razem z parametrami dropu. */
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

    /** Lekki zapis żywej istoty wystarczający do jej odtworzenia. */
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

    /** Stan zasianej rośliny razem z etapem wzrostu. */
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

    /** Zapis prostego obiektu wizualnego postawionego w świecie. */
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

    /** Zapis skrzyni razem z jej zawartością. */
    public static class ChestData {
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
        public String title = "Skrzynia";
        public int slotCount = 16;
        public List<InventoryEntryData> inventory = new ArrayList<>();
    }

    /** Dane przedmiotu leżącego na ziemi. */
    public static class ItemPickupData {
        public String itemId = "";
        public int quantity;
        public float x;
        public float y;
    }
}

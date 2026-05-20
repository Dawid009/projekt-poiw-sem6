package com.polsl.poiw.gameplay.item;

import com.badlogic.gdx.graphics.Color;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.engine.inventory.ItemQuality;
import com.polsl.poiw.engine.inventory.ItemType;

import java.util.List;

public final class GameplayItems {

    public static final ItemDefinition POTION_RED = consumableItem(
        "potion_red",
        "Czerwona mikstura",
        "Szybko leczy.",
        "potions/potion_red",
        new Color(0.82f, 0.2f, 0.2f, 1f),
        ItemQuality.COMMON,
        20,
        10f
    );

    public static final ItemDefinition POTION_BLUE = consumableItem(
        "potion_blue",
        "Blekitna mikstura",
        "Mikstura na drobne obrazenia.",
        "potions/potion_blue",
        new Color(0.2f, 0.55f, 0.92f, 1f),
        ItemQuality.COMMON,
        20,
        8f
    );

    public static final ItemDefinition POTION_GREEN = consumableItem(
        "potion_green",
        "Zielona mikstura",
        "Polna mikstura.",
        "potions/potion_green",
        new Color(0.2f, 0.74f, 0.35f, 1f),
        ItemQuality.COMMON,
        20,
        12f
    );

    public static final ItemDefinition POTION_PURPLE = consumableItem(
        "potion_purple",
        "Fioletowa mikstura",
        "Brak opisu",
        "potions/potion_purple",
        new Color(0.62f, 0.3f, 0.86f, 1f),
        ItemQuality.RARE,
        20,
        14f
    );

    public static final ItemDefinition POTION_YELLOW = consumableItem(
        "potion_yellow",
        "Zolta mikstura",
        "Jasna mikstura wzmacniajaca.",
        "potions/potion_yellow",
        new Color(0.9f, 0.78f, 0.18f, 1f),
        ItemQuality.RARE,
        20,
        16f
    );

    public static final ItemDefinition COIN_BRONZE = simpleItem(
        "coin_bronze",
        ItemType.VALUABLE,
        "Brazowa moneta",
        "Drobna moneta",
        "coins/coin_bronze",
        new Color(0.69f, 0.42f, 0.2f, 1f),
        ItemQuality.COMMON,
        99
    );

    public static final ItemDefinition COIN_SILVER = simpleItem(
        "coin_silver",
        ItemType.VALUABLE,
        "Srebrna moneta",
        "Moneta sredniej wartosci.",
        "coins/coin_silver",
        new Color(0.75f, 0.78f, 0.82f, 1f),
        ItemQuality.COMMON,
        99
    );

    public static final ItemDefinition COIN_GOLD = simpleItem(
        "coin_gold",
        ItemType.VALUABLE,
        "Zlota moneta",
        "Cenna moneta o wysokiej wartosci.",
        "coins/coin_gold",
        new Color(0.93f, 0.8f, 0.2f, 1f),
        ItemQuality.LEGENDARY,
        99
    );

    public static final ItemDefinition SHARD_BLUE = simpleItem(
        "shard_blue",
        ItemType.MISC,
        "Blekitny odlam",
        "Maly odlam",
        "misc/shard_blue",
        new Color(0.2f, 0.7f, 0.95f, 1f),
        ItemQuality.RARE,
        30
    );

    public static final ItemDefinition IRON_CLUSTER = simpleItem(
        "iron_cluster",
        ItemType.MATERIAL,
        "Bryla zelaza",
        "Zwarta bryla surowego zelaza.",
        "iron/item_iron_cluster",
        new Color(0.55f, 0.58f, 0.62f, 1f),
        ItemQuality.COMMON,
        40
    );

    public static final ItemDefinition IRON_BAR = simpleItem(
        "iron_bar",
        ItemType.MATERIAL,
        "Sztabka zelaza",
        "Przetopiona sztabka gotowa do obrobki.",
        "iron/item_iron_bar",
        new Color(0.63f, 0.66f, 0.7f, 1f),
        ItemQuality.COMMON,
        40
    );

    public static final ItemDefinition IRON_NUGGETS = simpleItem(
        "iron_nuggets",
        ItemType.MATERIAL,
        "Zelazne drobiny",
        "Garstka drobnych kawalkow zelaza.",
        "iron/item_iron_nuggets",
        new Color(0.5f, 0.53f, 0.56f, 1f),
        ItemQuality.COMMON,
        40
    );

    public static final ItemDefinition GOLD_CLUSTER = simpleItem(
        "gold_cluster",
        ItemType.MATERIAL,
        "Bryla zlota",
        "Surowe zloto wydobyte z zyly.",
        "gold/item_gold_cluster",
        new Color(0.9f, 0.77f, 0.2f, 1f),
        ItemQuality.RARE,
        40
    );

    public static final ItemDefinition GOLD_BAR = simpleItem(
        "gold_bar",
        ItemType.VALUABLE,
        "Sztabka zlota",
        "Ciezka sztabka o wysokiej wartosci.",
        "gold/item_gold_bar",
        new Color(0.92f, 0.78f, 0.28f, 1f),
        ItemQuality.LEGENDARY,
        20
    );

    public static final ItemDefinition GOLD_NUGGETS = simpleItem(
        "gold_nuggets",
        ItemType.MATERIAL,
        "Zlote drobiny",
        "Drobne kawalki zlota do dalszej obrobki.",
        "gold/item_gold_nuggets",
        new Color(0.86f, 0.72f, 0.16f, 1f),
        ItemQuality.RARE,
        40
    );

    public static final ItemDefinition ITEM_CARROT = simpleItem(
        "item_carrot",
        ItemType.MATERIAL,
        "Marchew",
        "marchew prosto z pola.",
        "food/carrot",
        new Color(0.93f, 0.48f, 0.18f, 1f),
        ItemQuality.COMMON,
        50
    );

    public static final ItemDefinition ITEM_WHEAT = simpleItem(
        "item_wheat",
        ItemType.MATERIAL,
        "Pszenica",
        "Wiazka dojrzalej pszenicy.",
        "food/wheat",
        new Color(0.88f, 0.76f, 0.32f, 1f),
        ItemQuality.COMMON,
        50
    );

    public static final ItemDefinition CHICKEN_RAW = simpleItem(
        "chicken_raw",
        ItemType.MATERIAL,
        "Surowy kurczak",
        "Swieze mieso drobiowe do dalszej obrobki.",
        "food/chicken_raw",
        new Color(0.92f, 0.72f, 0.62f, 1f),
        ItemQuality.COMMON,
        25
    );

    public static final ItemDefinition STEAK_RAW = simpleItem(
        "steak_raw",
        ItemType.MATERIAL,
        "Surowy steak",
        "Surowy kawalek czerwonego miesa.",
        "food/steak_raw",
        new Color(0.78f, 0.26f, 0.22f, 1f),
        ItemQuality.COMMON,
        25
    );

    public static final ItemDefinition WOOD_LOG = simpleItem(
        "wood_log",
        ItemType.MATERIAL,
        "Drewno",
        "Surowy kloc drewna.",
        "misc/tree_log",
        new Color(0.56f, 0.36f, 0.18f, 1f),
        ItemQuality.COMMON,
        50
    );

    public static final ItemDefinition SEEDS_CARROT = simpleItem(
        "seeds_carrot",
        ItemType.MISC,
        "Nasiona marchwi",
        "Mala paczka nasion marchwi.",
        "seeds/seeds_carrot",
        new Color(0.94f, 0.56f, 0.24f, 1f),
        ItemQuality.COMMON,
        50
    );

    public static final ItemDefinition SEEDS_WHEAT = simpleItem(
        "seeds_wheat",
        ItemType.MISC,
        "Nasiona pszenicy",
        "Proste ziarna gotowe do wysiewu.",
        "seeds/seeds_wheat",
        new Color(0.78f, 0.68f, 0.26f, 1f),
        ItemQuality.COMMON,
        50
    );

    public static final ItemDefinition HEAL_POTION = POTION_RED;
    public static final ItemDefinition MANA_SHARD = SHARD_BLUE;
    public static final ItemDefinition IRON_ORE = IRON_CLUSTER;
    public static final ItemDefinition GOLD_ORE = GOLD_CLUSTER;
    public static final ItemDefinition GOLD_COIN = COIN_GOLD;

    private static final List<ItemDefinition> ALL_ITEMS = List.of(
        COIN_BRONZE,
        COIN_SILVER,
        COIN_GOLD,
        CHICKEN_RAW,
        ITEM_CARROT,
        ITEM_WHEAT,
        STEAK_RAW,
        WOOD_LOG,
        GOLD_BAR,
        GOLD_CLUSTER,
        GOLD_NUGGETS,
        IRON_BAR,
        IRON_CLUSTER,
        IRON_NUGGETS,
        SHARD_BLUE,
        POTION_BLUE,
        POTION_GREEN,
        POTION_PURPLE,
        POTION_RED,
        POTION_YELLOW,
        SEEDS_CARROT,
        SEEDS_WHEAT
    );

    private static final List<ItemDefinition> DEBUG_ITEMS = List.of(
        POTION_RED,
        POTION_BLUE,
        POTION_GREEN,
        POTION_PURPLE,
        POTION_YELLOW,
        SHARD_BLUE,
        IRON_CLUSTER,
        COIN_SILVER,
        COIN_GOLD
    );

    private GameplayItems() {
    }

    public static ItemDefinition getDebugItem(int slot) {
        if (slot < 0 || slot >= DEBUG_ITEMS.size()) {
            return null;
        }
        return DEBUG_ITEMS.get(slot);
    }

    public static List<ItemDefinition> getDebugItems() {
        return DEBUG_ITEMS;
    }

    public static List<ItemDefinition> getAllItems() {
        return ALL_ITEMS;
    }

    public static ItemDefinition findById(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        for (ItemDefinition itemDefinition : ALL_ITEMS) {
            if (itemId.equals(itemDefinition.getItemId())) {
                return itemDefinition;
            }
        }

        return null;
    }

    private static ItemDefinition simpleItem(String itemId,
                                             ItemType itemType,
                                             String displayName,
                                             String description,
                                             String textureRegionName,
                                             Color displayColor,
                                             ItemQuality quality,
                                             int maxStack) {
        return ItemDefinition.builder(itemId, itemType, displayName)
            .description(description)
            .textureRegionName(textureRegionName)
            .displayColor(displayColor)
            .quality(quality)
            .maxStack(maxStack)
            .build();
    }

    private static ItemDefinition consumableItem(String itemId,
                                                 String displayName,
                                                 String description,
                                                 String textureRegionName,
                                                 Color displayColor,
                                                 ItemQuality quality,
                                                 int maxStack,
                                                 float healthRestoreAmount) {
        return ItemDefinition.builder(itemId, ItemType.CONSUMABLE, displayName)
            .description(description)
            .textureRegionName(textureRegionName)
            .displayColor(displayColor)
            .quality(quality)
            .maxStack(maxStack)
            .consumable(true)
            .healthRestoreAmount(healthRestoreAmount)
            .build();
    }
}
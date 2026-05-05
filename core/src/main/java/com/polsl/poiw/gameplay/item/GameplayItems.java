package com.polsl.poiw.gameplay.item;

import com.badlogic.gdx.graphics.Color;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.engine.inventory.ItemQuality;
import com.polsl.poiw.engine.inventory.ItemType;

import java.util.List;

public final class GameplayItems {

    public static final ItemDefinition HEAL_POTION = ItemDefinition.builder(
            "heal_potion",
            ItemType.CONSUMABLE,
            "Mikstura leczenia"
        )
        .description("Przywraca 10 punktow zdrowia.")
        .displayColor(new Color(0.82f, 0.2f, 0.2f, 1f))
        .quality(ItemQuality.COMMON)
        .maxStack(20)
        .consumable(true)
        .healthRestoreAmount(10f)
        .build();

    public static final ItemDefinition MANA_SHARD = ItemDefinition.builder(
            "mana_shard",
            ItemType.MATERIAL,
            "Odlamk many"
        )
        .description("Skondensowany kawalek energii. Na razie sluzy jako material testowy.")
        .displayColor(new Color(0.2f, 0.7f, 0.95f, 1f))
        .quality(ItemQuality.RARE)
        .maxStack(30)
        .build();

    public static final ItemDefinition IRON_ORE = ItemDefinition.builder(
            "iron_ore",
            ItemType.MATERIAL,
            "Ruda zelaza"
        )
        .description("Surowiec rzemieslniczy do dalszego przetwarzania.")
        .displayColor(new Color(0.55f, 0.58f, 0.62f, 1f))
        .quality(ItemQuality.COMMON)
        .maxStack(40)
        .build();

    public static final ItemDefinition GOLD_COIN = ItemDefinition.builder(
            "gold_coin",
            ItemType.VALUABLE,
            "Zlota moneta"
        )
        .description("Uniwersalna waluta kupcow i handlarzy.")
        .displayColor(new Color(0.93f, 0.8f, 0.2f, 1f))
        .quality(ItemQuality.LEGENDARY)
        .maxStack(99)
        .build();

    private static final List<ItemDefinition> DEBUG_ITEMS = List.of(
        HEAL_POTION,
        MANA_SHARD,
        IRON_ORE,
        GOLD_COIN
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
}
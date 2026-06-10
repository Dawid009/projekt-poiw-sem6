package com.polsl.poiw.gameplay.trade;

import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.item.GameplayItems;

public final class CurrencyHelper {
    private CurrencyHelper() {
    }

    public static int getTotalBronzeValue(InventoryComponent inventory) {
        if (inventory == null) {
            return 0;
        }

        int total = 0;
        for (InventoryStack stack : inventory.getItemsSnapshot()) {
            if (stack == null || stack.getDefinition() == null) {
                continue;
            }

            String itemId = stack.getDefinition().getItemId();
            if (GameplayItems.COIN_GOLD.getItemId().equals(itemId)) {
                total += stack.getQuantity() * TradePrice.GOLD_TO_BRONZE;
            } else if (GameplayItems.COIN_SILVER.getItemId().equals(itemId)) {
                total += stack.getQuantity() * TradePrice.SILVER_TO_BRONZE;
            } else if (GameplayItems.COIN_BRONZE.getItemId().equals(itemId)) {
                total += stack.getQuantity();
            }
        }
        return total;
    }

    public static boolean canAfford(InventoryComponent inventory, TradePrice price) {
        return getTotalBronzeValue(inventory) >= toBronzeValue(price);
    }

    public static boolean canReceive(InventoryComponent inventory, TradePrice price) {
        return canReceiveBronzeValue(inventory, toBronzeValue(price));
    }

    public static boolean canSpendAndReceive(InventoryComponent inventory,
                                             TradePrice price,
                                             ItemDefinition definition,
                                             int quantity) {
        if (inventory == null || definition == null || quantity <= 0) {
            return false;
        }

        InventoryComponent simulation = cloneInventory(inventory);
        return simulation != null
            && trySpend(simulation, price)
            && simulation.addItem(definition, quantity) == quantity;
    }

    public static boolean canReceiveBronzeValue(InventoryComponent inventory, int bronzeValue) {
        if (inventory == null) {
            return false;
        }
        if (bronzeValue <= 0) {
            return true;
        }

        InventoryComponent simulation = cloneInventory(inventory);
        return tryAddBronzeValueWithoutValidation(simulation, bronzeValue);
    }

    public static boolean trySpend(InventoryComponent inventory, TradePrice price) {
        if (inventory == null) {
            return false;
        }

        int cost = toBronzeValue(price);
        int total = getTotalBronzeValue(inventory);
        if (cost <= 0) {
            return true;
        }
        if (total < cost) {
            return false;
        }

        rewriteCoinStacks(inventory, total - cost);
        return true;
    }

    public static boolean tryAddPrice(InventoryComponent inventory, TradePrice price) {
        return tryAddBronzeValue(inventory, toBronzeValue(price));
    }

    public static boolean tryAddBronzeValue(InventoryComponent inventory, int bronzeValue) {
        if (inventory == null) {
            return false;
        }
        if (bronzeValue <= 0) {
            return true;
        }
        if (!canAddCurrencyBundle(inventory, bronzeValue)) {
            return false;
        }

        TradePrice normalized = TradePrice.fromBronzeValue(bronzeValue);
        if (normalized.goldCoins() > 0 && inventory.addItem(GameplayItems.COIN_GOLD, normalized.goldCoins()) != normalized.goldCoins()) {
            return false;
        }
        if (normalized.silverCoins() > 0 && inventory.addItem(GameplayItems.COIN_SILVER, normalized.silverCoins()) != normalized.silverCoins()) {
            return false;
        }
        if (normalized.bronzeCoins() > 0 && inventory.addItem(GameplayItems.COIN_BRONZE, normalized.bronzeCoins()) != normalized.bronzeCoins()) {
            return false;
        }
        return true;
    }

    private static int toBronzeValue(TradePrice price) {
        return price != null ? price.toBronzeValue() : 0;
    }

    private static void rewriteCoinStacks(InventoryComponent inventory, int remainingBronzeValue) {
        inventory.removeItem(GameplayItems.COIN_GOLD.getItemId(), Integer.MAX_VALUE);
        inventory.removeItem(GameplayItems.COIN_SILVER.getItemId(), Integer.MAX_VALUE);
        inventory.removeItem(GameplayItems.COIN_BRONZE.getItemId(), Integer.MAX_VALUE);
        tryAddBronzeValueWithoutValidation(inventory, remainingBronzeValue);
    }

    private static boolean canAddCurrencyBundle(InventoryComponent inventory, int bronzeValue) {
        InventoryComponent simulation = cloneInventory(inventory);
        return simulation != null && tryAddBronzeValueWithoutValidation(simulation, bronzeValue);
    }

    private static boolean tryAddBronzeValueWithoutValidation(InventoryComponent inventory, int bronzeValue) {
        if (inventory == null || bronzeValue < 0) {
            return false;
        }

        TradePrice normalized = TradePrice.fromBronzeValue(bronzeValue);
        return inventory.addItem(GameplayItems.COIN_GOLD, normalized.goldCoins()) == normalized.goldCoins()
            && inventory.addItem(GameplayItems.COIN_SILVER, normalized.silverCoins()) == normalized.silverCoins()
            && inventory.addItem(GameplayItems.COIN_BRONZE, normalized.bronzeCoins()) == normalized.bronzeCoins();
    }

    private static InventoryComponent cloneInventory(InventoryComponent inventory) {
        if (inventory == null) {
            return null;
        }

        InventoryComponent clone = new InventoryComponent();
        clone.setMaxSlots(inventory.getMaxSlots());
        clone.restoreSaveEntries(inventory.buildSaveEntries());
        return clone;
    }
}

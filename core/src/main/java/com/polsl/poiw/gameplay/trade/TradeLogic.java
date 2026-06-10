package com.polsl.poiw.gameplay.trade;

import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.TradeBasketComponent;
import com.polsl.poiw.engine.inventory.InventoryStack;

import java.util.List;
import java.util.function.Function;

public final class TradeLogic {
    private TradeLogic() {
    }

    public static boolean transferStack(InventoryComponent source,
                                        InventoryComponent target,
                                        int slotIndex,
                                        boolean wholeStack) {
        if (source == null || target == null || slotIndex < 0) {
            return false;
        }

        InventoryStack stack = source.getStackAt(slotIndex);
        if (stack == null || stack.getDefinition() == null || stack.getQuantity() <= 0) {
            return false;
        }

        int transferQuantity = wholeStack ? stack.getQuantity() : 1;
        if (!target.canAddItem(stack.getDefinition(), transferQuantity)) {
            return false;
        }

        int removed = source.removeItemAt(slotIndex, transferQuantity);
        if (removed <= 0) {
            return false;
        }

        int added = target.addItem(stack.getDefinition(), removed);
        if (added == removed) {
            return true;
        }

        source.addItem(stack.getDefinition(), removed - Math.max(0, added));
        return false;
    }

    public static boolean buyTraderItem(InventoryComponent playerInventory,
                                        InventoryComponent traderInventory,
                                        TradeOfferDefinition offer,
                                        int traderSlotIndex,
                                        String itemId) {
        if (playerInventory == null
            || traderInventory == null
            || offer == null
            || itemId == null
            || itemId.isBlank()
            || offer.buyPrice().isZero()) {
            return false;
        }

        InventoryStack traderStack = findTraderStackForPurchase(traderInventory, traderSlotIndex, itemId);
        if (traderStack == null || traderStack.getDefinition() == null || traderStack.getQuantity() <= 0) {
            return false;
        }

        if (!CurrencyHelper.canSpendAndReceive(playerInventory, offer.buyPrice(), traderStack.getDefinition(), 1)) {
            return false;
        }

        if (!CurrencyHelper.trySpend(playerInventory, offer.buyPrice())) {
            return false;
        }
        if (traderInventory.removeItemAt(traderStack.getSlotIndex(), 1) <= 0) {
            CurrencyHelper.tryAddPrice(playerInventory, offer.buyPrice());
            return false;
        }
        if (playerInventory.addItem(traderStack.getDefinition(), 1) != 1) {
            traderInventory.addItem(traderStack.getDefinition(), 1);
            CurrencyHelper.tryAddPrice(playerInventory, offer.buyPrice());
            return false;
        }
        return true;
    }

    public static boolean sellTradeBasket(TradeBasketComponent tradeBasket,
                                          InventoryComponent playerInventory,
                                          Function<String, TradeOfferDefinition> offerResolver) {
        if (tradeBasket == null
            || playerInventory == null
            || offerResolver == null
            || tradeBasket.getOccupiedSlotCount() <= 0) {
            return false;
        }

        int totalBronzeValue = 0;
        for (InventoryStack stack : tradeBasket.getItemsSnapshot()) {
            if (stack == null || stack.getDefinition() == null) {
                continue;
            }

            TradeOfferDefinition offer = offerResolver.apply(stack.getDefinition().getItemId());
            if (offer == null || offer.sellPrice().isZero()) {
                return false;
            }
            totalBronzeValue += offer.sellPrice().toBronzeValue() * stack.getQuantity();
        }

        if (totalBronzeValue <= 0 || !CurrencyHelper.canReceiveBronzeValue(playerInventory, totalBronzeValue)) {
            return false;
        }

        tradeBasket.restoreSaveEntries(List.of());
        return CurrencyHelper.tryAddBronzeValue(playerInventory, totalBronzeValue);
    }

    public static InventoryStack findTraderStackForPurchase(InventoryComponent traderInventory,
                                                            int traderSlotIndex,
                                                            String itemId) {
        if (traderInventory == null || itemId == null || itemId.isBlank()) {
            return null;
        }

        InventoryStack requestedStack = traderInventory.getStackAt(traderSlotIndex);
        if (requestedStack != null
            && requestedStack.getDefinition() != null
            && itemId.equals(requestedStack.getDefinition().getItemId())
            && requestedStack.getQuantity() > 0) {
            return requestedStack;
        }

        for (InventoryStack stack : traderInventory.getItemsSnapshot()) {
            if (stack == null || stack.getDefinition() == null || stack.getQuantity() <= 0) {
                continue;
            }
            if (itemId.equals(stack.getDefinition().getItemId())) {
                return stack;
            }
        }
        return null;
    }
}

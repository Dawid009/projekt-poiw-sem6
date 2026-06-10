package com.polsl.poiw.gameplay.trade;

import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.item.GameplayItems;

import java.util.List;

public final class TraderCatalog {
    private static final List<TradeOfferDefinition> FARMER_OFFERS = List.of(
        offer(GameplayItems.SEEDS_CARROT, 30, TradePrice.of(0, 0, 4), TradePrice.of(0, 0, 2)),
        offer(GameplayItems.SEEDS_WHEAT, 30, TradePrice.of(0, 0, 4), TradePrice.of(0, 0, 2)),
        offer(GameplayItems.ITEM_CARROT, 18, TradePrice.of(0, 0, 8), TradePrice.of(0, 0, 4)),
        offer(GameplayItems.ITEM_WHEAT, 18, TradePrice.of(0, 0, 8), TradePrice.of(0, 0, 4)),
        offer(GameplayItems.CHICKEN_RAW, 12, TradePrice.of(0, 1, 2), TradePrice.of(0, 0, 6)),
        offer(GameplayItems.STEAK_RAW, 10, TradePrice.of(0, 1, 5), TradePrice.of(0, 0, 8)),
        offer(GameplayItems.WOOD_LOG, 24, TradePrice.of(0, 0, 9), TradePrice.of(0, 0, 4)),
        offer(GameplayItems.POTION_RED, 8, TradePrice.of(0, 2, 5), TradePrice.of(0, 1, 0))
    );

    private static final List<TradeOfferDefinition> MINER_OFFERS = List.of(
        offer(GameplayItems.IRON_CLUSTER, 18, TradePrice.of(0, 1, 4), TradePrice.of(0, 0, 7)),
        offer(GameplayItems.IRON_NUGGETS, 24, TradePrice.of(0, 0, 9), TradePrice.of(0, 0, 4)),
        offer(GameplayItems.IRON_BAR, 10, TradePrice.of(0, 2, 0), TradePrice.of(0, 1, 0)),
        offer(GameplayItems.GOLD_CLUSTER, 10, TradePrice.of(0, 3, 5), TradePrice.of(0, 1, 8)),
        offer(GameplayItems.GOLD_NUGGETS, 16, TradePrice.of(0, 2, 2), TradePrice.of(0, 1, 1)),
        offer(GameplayItems.GOLD_BAR, 4, TradePrice.of(1, 0, 0), TradePrice.of(0, 5, 0)),
        offer(GameplayItems.WOOD_LOG, 16, TradePrice.of(0, 0, 9), TradePrice.of(0, 0, 4))
    );

    private TraderCatalog() {
    }

    public static List<TradeOfferDefinition> getOffers(TraderKind traderKind) {
        if (traderKind == null) {
            return List.of();
        }

        return switch (traderKind) {
            case FARMER -> FARMER_OFFERS;
            case MINER -> MINER_OFFERS;
        };
    }

    public static TradeOfferDefinition findOffer(TraderKind traderKind, String itemId) {
        if (traderKind == null || itemId == null || itemId.isBlank()) {
            return null;
        }

        for (TradeOfferDefinition offer : getOffers(traderKind)) {
            if (itemId.equals(offer.itemDefinition().getItemId())) {
                return offer;
            }
        }
        return null;
    }

    private static TradeOfferDefinition offer(ItemDefinition itemDefinition,
                                              int initialStock,
                                              TradePrice buyPrice,
                                              TradePrice sellPrice) {
        return new TradeOfferDefinition(itemDefinition, initialStock, buyPrice, sellPrice);
    }
}

package com.polsl.poiw.gameplay.trade;

import com.polsl.poiw.engine.inventory.ItemDefinition;

public record TradeOfferDefinition(
    ItemDefinition itemDefinition,
    int initialStock,
    TradePrice buyPrice,
    TradePrice sellPrice
) {
    public TradeOfferDefinition {
        if (itemDefinition == null) {
            throw new IllegalArgumentException("itemDefinition cannot be null");
        }
        initialStock = Math.max(0, initialStock);
        buyPrice = buyPrice != null ? buyPrice : TradePrice.ZERO;
        sellPrice = sellPrice != null ? sellPrice : TradePrice.ZERO;
    }
}

package com.polsl.poiw.engine.component;

public class TradeBasketComponent extends InventoryComponent {
    public static final int DEFAULT_SLOT_COUNT = 4;

    public TradeBasketComponent() {
        setMaxSlots(DEFAULT_SLOT_COUNT);
    }
}

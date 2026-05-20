package com.polsl.poiw.engine.inventory;

public final class InventoryStack {

    private final ItemDefinition definition;
    private final int quantity;

    public InventoryStack(ItemDefinition definition, int quantity) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        this.definition = definition;
        this.quantity = quantity;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public int getQuantity() {
        return quantity;
    }
}
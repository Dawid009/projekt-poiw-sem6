package com.polsl.poiw.engine.inventory;

public final class InventoryStack {

    private final ItemDefinition definition;
    private final int quantity;
    private final int slotIndex;

    public InventoryStack(ItemDefinition definition, int quantity) {
        this(definition, quantity, -1);
    }

    public InventoryStack(ItemDefinition definition, int quantity, int slotIndex) {
        if (definition == null) {
            throw new IllegalArgumentException("definition cannot be null");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        this.definition = definition;
        this.quantity = quantity;
        this.slotIndex = slotIndex;
    }

    public ItemDefinition getDefinition() {
        return definition;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getSlotIndex() {
        return slotIndex;
    }
}

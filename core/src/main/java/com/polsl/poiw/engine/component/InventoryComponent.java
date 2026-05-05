package com.polsl.poiw.engine.component;

import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryComponent extends AbstractActorComponent {

    private final Map<String, InventoryRecord> items = new LinkedHashMap<>();
    // Revision sluzy tylko do odswiezania UI po zmianie zawartosci.
    private final transient PropertyBinding<Integer> revisionBinding = new PropertyBinding<>(0);
    private int revision = 0;

    public int addItem(ItemDefinition definition, int quantity) {
        if (definition == null || quantity <= 0) {
            return 0;
        }

        InventoryRecord record = items.get(definition.getItemId());
        int currentQuantity = record != null ? record.quantity : 0;
        int maxQuantity = definition.getMaxStack();
        int remainingSpace = Math.max(0, maxQuantity - currentQuantity);
        int added = Math.min(quantity, remainingSpace);
        if (added <= 0) {
            return 0;
        }

        if (record == null) {
            items.put(definition.getItemId(), new InventoryRecord(definition, added));
        } else {
            record.quantity += added;
        }
        broadcastChange();
        return added;
    }

    public int removeItem(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank() || quantity <= 0) {
            return 0;
        }

        InventoryRecord record = items.get(itemId);
        if (record == null) {
            return 0;
        }

        int removed = Math.min(quantity, record.quantity);
        record.quantity -= removed;
        if (record.quantity <= 0) {
            items.remove(itemId);
        }
        if (removed > 0) {
            broadcastChange();
        }
        return removed;
    }

    public boolean useItem(String itemId) {
        InventoryRecord record = items.get(itemId);
        if (record == null || record.quantity <= 0) {
            return false;
        }

        ItemDefinition definition = record.definition;
        if (!definition.isConsumable()) {
            return false;
        }

        float healthRestore = definition.getHealthRestoreAmount();
        if (healthRestore <= 0f) {
            return false;
        }

        HealthComponent healthComponent = getOwner() != null
            ? getOwner().getComponent(HealthComponent.class)
            : null;
        if (healthComponent == null) {
            return false;
        }

        healthComponent.heal(healthRestore);
        removeItem(itemId, 1);
        return true;
    }

    public boolean canUse(String itemId) {
        InventoryRecord record = items.get(itemId);
        return record != null
            && record.quantity > 0
            && record.definition.isConsumable()
            && record.definition.getHealthRestoreAmount() > 0f
            && getOwner() != null
            && getOwner().getComponent(HealthComponent.class) != null;
    }

    public InventoryStack getStack(String itemId) {
        InventoryRecord record = items.get(itemId);
        return record != null ? new InventoryStack(record.definition, record.quantity) : null;
    }

    public List<InventoryStack> getItemsSnapshot() {
        // UI dostaje kopie, zeby nie grzebalo w stanie komponentu.
        List<InventoryStack> snapshot = new ArrayList<>();
        for (InventoryRecord record : items.values()) {
            snapshot.add(new InventoryStack(record.definition, record.quantity));
        }
        return snapshot;
    }

    public PropertyBinding<Integer> getRevisionBinding() {
        return revisionBinding;
    }

    private void broadcastChange() {
        revisionBinding.set(++revision);
    }

    private static final class InventoryRecord {

        private final ItemDefinition definition;
        private int quantity;

        private InventoryRecord(ItemDefinition definition, int quantity) {
            this.definition = definition;
            this.quantity = quantity;
        }
    }
}
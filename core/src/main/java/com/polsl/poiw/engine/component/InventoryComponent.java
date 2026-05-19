package com.polsl.poiw.engine.component;

import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;

import java.util.ArrayList;
import java.util.List;

public class InventoryComponent extends AbstractActorComponent {

    private final List<InventoryRecord> stacks = new ArrayList<>();
    // Revision sluzy tylko do odswiezania UI po zmianie zawartosci.
    private final transient PropertyBinding<Integer> revisionBinding = new PropertyBinding<>(0);
    private int revision = 0;

    public int addItem(ItemDefinition definition, int quantity) {
        if (definition == null || quantity <= 0) {
            return 0;
        }

        int remaining = quantity;
        int maxQuantity = definition.getMaxStack();

        for (InventoryRecord record : stacks) {
            if (!definition.getItemId().equals(record.definition.getItemId())) {
                continue;
            }

            int remainingSpace = Math.max(0, maxQuantity - record.quantity);
            if (remainingSpace <= 0) {
                continue;
            }

            int addedToStack = Math.min(remaining, remainingSpace);
            record.quantity += addedToStack;
            remaining -= addedToStack;
            if (remaining <= 0) {
                break;
            }
        }

        while (remaining > 0) {
            int addedToNewStack = Math.min(remaining, maxQuantity);
            stacks.add(new InventoryRecord(definition, addedToNewStack));
            remaining -= addedToNewStack;
        }

        int added = quantity - remaining;
        if (added > 0) {
            broadcastChange();
        }
        return added;
    }

    public int removeItem(String itemId, int quantity) {
        if (itemId == null || itemId.isBlank() || quantity <= 0) {
            return 0;
        }

        int remaining = quantity;
        int removed = 0;

        for (int index = 0; index < stacks.size() && remaining > 0; ) {
            InventoryRecord record = stacks.get(index);
            if (!itemId.equals(record.definition.getItemId())) {
                index += 1;
                continue;
            }

            int removedFromStack = Math.min(remaining, record.quantity);
            record.quantity -= removedFromStack;
            removed += removedFromStack;
            remaining -= removedFromStack;

            if (record.quantity <= 0) {
                stacks.remove(index);
            } else {
                index += 1;
            }
        }

        if (removed > 0) {
            broadcastChange();
        }
        return removed;
    }

    public boolean useItem(String itemId) {
        InventoryRecord record = findFirstRecord(itemId);
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
        InventoryRecord record = findFirstRecord(itemId);
        return record != null
            && record.quantity > 0
            && record.definition.isConsumable()
            && record.definition.getHealthRestoreAmount() > 0f
            && getOwner() != null
            && getOwner().getComponent(HealthComponent.class) != null;
    }

    public InventoryStack getStack(String itemId) {
        InventoryRecord record = findFirstRecord(itemId);
        return record != null ? new InventoryStack(record.definition, record.quantity) : null;
    }

    public List<InventoryStack> getItemsSnapshot() {
        // UI dostaje kopie, zeby nie grzebalo w stanie komponentu.
        List<InventoryStack> snapshot = new ArrayList<>();
        for (InventoryRecord record : stacks) {
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

    private InventoryRecord findFirstRecord(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return null;
        }

        for (InventoryRecord record : stacks) {
            if (itemId.equals(record.definition.getItemId())) {
                return record;
            }
        }
        return null;
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
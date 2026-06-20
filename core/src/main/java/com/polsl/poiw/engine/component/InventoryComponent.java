package com.polsl.poiw.engine.component;

import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;
import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.engine.save.SaveGameData;
import com.polsl.poiw.gameplay.item.GameplayItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InventoryComponent extends AbstractActorComponent {
    private static final String STACK_SEPARATOR = ";";
    private static final String VALUE_SEPARATOR = ":";

    private final List<InventoryRecord> stacks = new ArrayList<>();
    private int maxSlots = Integer.MAX_VALUE;
    // Revision sluzy tylko do odswiezania UI po zmianie zawartosci.
    private final transient PropertyBinding<Integer> revisionBinding = new PropertyBinding<>(0);
    private int revision = 0;

    @Replicated
    @RepNotify("onReplicatedStacksChanged")
    private String replicatedStacks = "";

    /** Inventory jest replikowane, bo jego stan musi byc taki sam na kliencie i serwerze. */
    public InventoryComponent() {
        setReplicated(true);
    }

    /** Dodaje przedmiot do inventory i zwraca, ile sztuk faktycznie weszlo. */
    public int addItem(ItemDefinition definition, int quantity) {
        if (definition == null || quantity <= 0) {
            return 0;
        }

        int added = mergeItem(definition, quantity);
        if (added > 0) {
            syncReplicatedStacks();
            broadcastChange();
        }
        return added;
    }

    /**
     * Najpierw probuje dopelnic istniejące stacki, a potem tworzy nowe sloty.
     * Dzięki temu inventory zachowuje się naturalnie i nie marnuje miejsca.
     */
    private int mergeItem(ItemDefinition definition, int quantity) {
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

        while (remaining > 0 && stacks.size() < maxSlots) {
            int addedToNewStack = Math.min(remaining, maxQuantity);
            stacks.add(new InventoryRecord(definition, addedToNewStack));
            remaining -= addedToNewStack;
        }

        return quantity - remaining;
    }

    /** Sprawdza, czy da się dodać całą wskazaną ilość przedmiotów. */
    public boolean canAddItem(ItemDefinition definition, int quantity) {
        if (definition == null || quantity <= 0) {
            return false;
        }

        return computeAddableQuantity(definition, quantity) >= quantity;
    }

    /** Usuwa wskazaną liczbę sztuk przedmiotu po jego ID. */
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
            clearAssignedItemIfMissing(itemId);
            syncReplicatedStacks();
            broadcastChange();
        }
        return removed;
    }

    /** Zużywa przedmiot i aplikuje leczenie, jeśli to item konsumpcyjny. */
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

    /** Sprawdza, czy dany item można teraz użyć. */
    public boolean canUse(String itemId) {
        InventoryRecord record = findFirstRecord(itemId);
        return record != null
            && record.quantity > 0
            && record.definition.isConsumable()
            && record.definition.getHealthRestoreAmount() > 0f
            && getOwner() != null
            && getOwner().getComponent(HealthComponent.class) != null;
    }

    /** Zwraca stack po ID, jeśli taki istnieje. */
    public InventoryStack getStack(String itemId) {
        int index = findFirstRecordIndex(itemId);
        InventoryRecord record = index >= 0 ? stacks.get(index) : null;
        return record != null ? new InventoryStack(record.definition, record.quantity, index) : null;
    }

    /** Zwraca stack z konkretnego slotu. */
    public InventoryStack getStackAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= stacks.size()) {
            return null;
        }

        InventoryRecord record = stacks.get(slotIndex);
        return record != null ? new InventoryStack(record.definition, record.quantity, slotIndex) : null;
    }

    /** Usuwa przedmioty z konkretnego slotu. */
    public int removeItemAt(int slotIndex, int quantity) {
        if (slotIndex < 0 || slotIndex >= stacks.size() || quantity <= 0) {
            return 0;
        }

        InventoryRecord record = stacks.get(slotIndex);
        if (record == null || record.definition == null || record.quantity <= 0) {
            return 0;
        }

        int removed = Math.min(quantity, record.quantity);
        record.quantity -= removed;
        String itemId = record.definition.getItemId();
        if (record.quantity <= 0) {
            stacks.remove(slotIndex);
        }

        if (removed > 0) {
            clearAssignedItemIfMissing(itemId);
            syncReplicatedStacks();
            broadcastChange();
        }
        return removed;
    }

    /** Używa itemu znajdującego się w konkretnym slocie. */
    public boolean useItemAt(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= stacks.size()) {
            return false;
        }

        InventoryRecord record = stacks.get(slotIndex);
        if (record == null || record.quantity <= 0 || record.definition == null) {
            return false;
        }

        ItemDefinition definition = record.definition;
        if (!definition.isConsumable() || definition.getHealthRestoreAmount() <= 0f) {
            return false;
        }

        HealthComponent healthComponent = getOwner() != null
            ? getOwner().getComponent(HealthComponent.class)
            : null;
        if (healthComponent == null) {
            return false;
        }

        healthComponent.heal(definition.getHealthRestoreAmount());
        removeItemAt(slotIndex, 1);
        return true;
    }

    /** Zwraca kopię listy stacków, żeby UI nie mogło zmienić stanu komponentu bezpośrednio. */
    public List<InventoryStack> getItemsSnapshot() {
        List<InventoryStack> snapshot = new ArrayList<>();
        for (int index = 0; index < stacks.size(); index++) {
            InventoryRecord record = stacks.get(index);
            snapshot.add(new InventoryStack(record.definition, record.quantity, index));
        }
        return snapshot;
    }

    /** Pobiera aktualne stacki i jednocześnie czyści całe inventory. */
    public List<InventoryStack> clearAndExtractSnapshot() {
        List<InventoryStack> snapshot = getItemsSnapshot();
        if (snapshot.isEmpty()) {
            return snapshot;
        }

        stacks.clear();
        clearAssignedItem();
        syncReplicatedStacks();
        broadcastChange();
        return snapshot;
    }

    /** Zamienia zawartość inventory na prostą listę wpisów do save'a. */
    public List<SaveGameData.InventoryEntryData> buildSaveEntries() {
        List<SaveGameData.InventoryEntryData> entries = new ArrayList<>();
        for (InventoryRecord record : stacks) {
            if (record.definition == null || record.quantity <= 0) {
                continue;
            }

            SaveGameData.InventoryEntryData entry = new SaveGameData.InventoryEntryData();
            entry.itemId = record.definition.getItemId();
            entry.quantity = record.quantity;
            entries.add(entry);
        }
        return entries;
    }

    /** Odtwarza inventory z danych save'a i od razu odświeża replikację oraz UI. */
    public void restoreSaveEntries(List<SaveGameData.InventoryEntryData> entries) {
        stacks.clear();
        if (entries != null) {
            for (SaveGameData.InventoryEntryData entry : entries) {
                if (entry == null || entry.itemId == null || entry.itemId.isBlank() || entry.quantity <= 0) {
                    continue;
                }

                ItemDefinition definition = GameplayItems.findById(entry.itemId);
                if (definition == null) {
                    continue;
                }

                mergeItem(definition, entry.quantity);
            }
        }

        syncReplicatedStacks();
        broadcastChange();
    }

    /** Zwraca licznik zmian, który UI może obserwować. */
    public PropertyBinding<Integer> getRevisionBinding() {
        return revisionBinding;
    }

    /** Ustawia, ile slotów maksymalnie może mieć to inventory. */
    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots > 0 ? maxSlots : Integer.MAX_VALUE;
    }

    /** Zwraca limit slotów. */
    public int getMaxSlots() {
        return maxSlots;
    }

    /** Zwraca liczbę zajętych slotów. */
    public int getOccupiedSlotCount() {
        return stacks.size();
    }

    /** Zwiększa licznik zmian, żeby UI wiedziało, że trzeba odświeżyć zawartość. */
    private void broadcastChange() {
        revisionBinding.set(++revision);
    }

    /** Zamienia stan inventory na jeden string do replikacji. */
    private void syncReplicatedStacks() {
        String serialized = serializeStacks();
        if (Objects.equals(replicatedStacks, serialized)) {
            return;
        }

        replicatedStacks = serialized;
        markDirty("replicatedStacks");
    }

    /** Odtwarza inventory po stronie klienta z zserializowanej wartości. */
    @SuppressWarnings("unused")
    private void onReplicatedStacksChanged() {
        deserializeStacks(replicatedStacks);
        broadcastChange();
    }

    /** Sprowadza wszystkie sloty do prostego formatu tekstowego. */
    private String serializeStacks() {
        if (stacks.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (InventoryRecord record : stacks) {
            if (record.definition == null || record.quantity <= 0) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(STACK_SEPARATOR);
            }
            builder.append(record.definition.getItemId())
                .append(VALUE_SEPARATOR)
                .append(record.quantity);
        }
        return builder.toString();
    }

    /** Odtwarza stacki z tekstu otrzymanego z replikacji. */
    private void deserializeStacks(String serialized) {
        stacks.clear();
        if (serialized == null || serialized.isBlank()) {
            return;
        }

        String[] rawStacks = serialized.split(STACK_SEPARATOR);
        for (String rawStack : rawStacks) {
            if (rawStack == null || rawStack.isBlank()) {
                continue;
            }

            String[] parts = rawStack.split(VALUE_SEPARATOR, 2);
            if (parts.length != 2) {
                continue;
            }

            ItemDefinition definition = GameplayItems.findById(parts[0]);
            if (definition == null) {
                continue;
            }

            int quantity;
            try {
                quantity = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                continue;
            }

            if (quantity <= 0) {
                continue;
            }

            mergeItem(definition, quantity);
        }
    }

    /** Sprawdza, ile sztuk danego itemu da się jeszcze dołożyć. */
    private int computeAddableQuantity(ItemDefinition definition, int requestedQuantity) {
        int remaining = requestedQuantity;
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
            remaining -= addedToStack;
            if (remaining <= 0) {
                return requestedQuantity;
            }
        }

        int freeSlots = Math.max(0, maxSlots - stacks.size());
        if (freeSlots <= 0) {
            return requestedQuantity - remaining;
        }

        int addableFromNewSlots = freeSlots * maxQuantity;
        int addable = Math.min(remaining, addableFromNewSlots);
        return requestedQuantity - remaining + addable;
    }

    /** Szuka pierwszego stacka o danym itemId. */
    private InventoryRecord findFirstRecord(String itemId) {
        int index = findFirstRecordIndex(itemId);
        return index >= 0 ? stacks.get(index) : null;
    }

    /** Szuka indeksu pierwszego stacka o danym itemId. */
    private int findFirstRecordIndex(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return -1;
        }

        for (int index = 0; index < stacks.size(); index++) {
            InventoryRecord record = stacks.get(index);
            if (itemId.equals(record.definition.getItemId())) {
                return index;
            }
        }
        return -1;
    }

    /** Czyści przypisany item, jeśli zniknął z inventory. */
    private void clearAssignedItemIfMissing(String itemId) {
        if (itemId == null || itemId.isBlank() || findFirstRecord(itemId) != null || getOwner() == null) {
            return;
        }

        clearAssignedItem();
    }

    /** Usuwa przypisany item z hotbara. */
    private void clearAssignedItem() {
        if (getOwner() == null) {
            return;
        }

        PlayerAssignedItemComponent assignedItemComponent = getOwner().getComponent(PlayerAssignedItemComponent.class);
        if (assignedItemComponent != null) {
            assignedItemComponent.clearAssignedItem();
        }
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

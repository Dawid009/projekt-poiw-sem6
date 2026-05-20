package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;

import java.util.Objects;

public class PlayerAssignedItemComponent extends AbstractActorComponent {
    public static final ComponentMapper<PlayerAssignedItemComponent> MAPPER =
        ComponentMapper.getFor(PlayerAssignedItemComponent.class);

    @Replicated
    @RepNotify("onAssignedItemIdChanged")
    private String assignedItemId = "";

    private final transient PropertyBinding<String> assignedItemIdBinding = new PropertyBinding<>("");

    public PlayerAssignedItemComponent() {
        setReplicated(true);
    }

    public String getAssignedItemId() {
        return assignedItemId != null ? assignedItemId : "";
    }

    public boolean hasAssignedItem() {
        return !getAssignedItemId().isBlank();
    }

    public void setAssignedItemId(String itemId) {
        String normalizedItemId = itemId != null ? itemId.trim() : "";
        if (Objects.equals(getAssignedItemId(), normalizedItemId)) {
            return;
        }

        assignedItemId = normalizedItemId;
        markDirty("assignedItemId");
        assignedItemIdBinding.set(assignedItemId);
    }

    public void clearAssignedItem() {
        setAssignedItemId("");
    }

    public PropertyBinding<String> getAssignedItemIdBinding() {
        return assignedItemIdBinding;
    }

    @SuppressWarnings("unused")
    private void onAssignedItemIdChanged() {
        assignedItemIdBinding.set(getAssignedItemId());
    }
}
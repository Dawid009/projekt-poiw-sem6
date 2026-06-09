package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.save.SaveGameData;
import com.polsl.poiw.gameplay.character.PlayerCharacter;

import java.util.List;
import java.util.Map;

public class ChestActor extends TiledVisualActor {
    public static final String PROP_STORAGE_TITLE = "storageTitle";
    public static final String PROP_STORAGE_SLOTS = "storageSlots";
    public static final String DEFAULT_STORAGE_TITLE = "Skrzynia";
    public static final int DEFAULT_STORAGE_SLOTS = 16;
    private static final float INTERACTION_PADDING = 0.85f;

    private final Vector2 actorCenter = new Vector2();
    private final Vector2 playerCenter = new Vector2();

    private String storageTitle = DEFAULT_STORAGE_TITLE;
    private int storageSlots = DEFAULT_STORAGE_SLOTS;

    public void configure(TiledMap map,
                          int tileGid,
                          TextureRegion region,
                          float sizeW,
                          float sizeH,
                          float sortOffsetY,
                          int zOrder,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          String title,
                          int slotCount) {
        super.configure(map, tileGid, region, sizeW, sizeH, sortOffsetY, zOrder, collHalfW, collHalfH, collOffset);
        ensureInventoryConfigured(title, slotCount, null);
    }

    public void configure(TiledMap map,
                          int tileGid,
                          TextureRegion region,
                          float sizeW,
                          float sizeH,
                          float sortOffsetY,
                          int zOrder,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          String title,
                          int slotCount,
                          List<SaveGameData.InventoryEntryData> inventoryEntries) {
        super.configure(map, tileGid, region, sizeW, sizeH, sortOffsetY, zOrder, collHalfW, collHalfH, collOffset);
        ensureInventoryConfigured(title, slotCount, inventoryEntries);
    }

    public void configureServer(int tileGid,
                                float sizeW,
                                float sizeH,
                                float sortOffsetY,
                                int zOrder,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                String title,
                                int slotCount) {
        super.configureServer(tileGid, sizeW, sizeH, sortOffsetY, zOrder, collHalfW, collHalfH, collOffset);
        ensureInventoryConfigured(title, slotCount, null);
    }

    public void configureServer(int tileGid,
                                float sizeW,
                                float sizeH,
                                float sortOffsetY,
                                int zOrder,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                String title,
                                int slotCount,
                                List<SaveGameData.InventoryEntryData> inventoryEntries) {
        super.configureServer(tileGid, sizeW, sizeH, sortOffsetY, zOrder, collHalfW, collHalfH, collOffset);
        ensureInventoryConfigured(title, slotCount, inventoryEntries);
    }

    @Override
    public void configureFromReplication(TiledMap map, Map<String, Object> initialProperties) {
        super.configureFromReplication(map, initialProperties);
        String replicatedTitle = initialProperties != null ? (String) initialProperties.get(PROP_STORAGE_TITLE) : null;
        int replicatedSlotCount = getInt(initialProperties, PROP_STORAGE_SLOTS, DEFAULT_STORAGE_SLOTS);
        ensureInventoryConfigured(replicatedTitle, replicatedSlotCount, null);
    }

    @Override
    public Map<String, Object> buildInitialReplicationProperties() {
        Map<String, Object> properties = super.buildInitialReplicationProperties();
        properties.put(PROP_STORAGE_TITLE, storageTitle);
        properties.put(PROP_STORAGE_SLOTS, storageSlots);
        return properties;
    }

    public SaveGameData.ChestData buildChestSaveData() {
        SaveGameData.ChestData data = new SaveGameData.ChestData();
        TransformComponent transform = getComponent(TransformComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        InventoryComponent inventory = getInventoryComponent();

        data.title = storageTitle;
        data.slotCount = storageSlots;
        if (transform != null) {
            data.x = transform.getPosition().x;
            data.y = transform.getPosition().y;
            data.sizeW = transform.getSize().x;
            data.sizeH = transform.getSize().y;
            data.sortOffsetY = transform.getSortOffsetY();
            data.zOrder = transform.getZOrder();
        }
        if (collision != null) {
            data.collHalfW = collision.getHalfWidth();
            data.collHalfH = collision.getHalfHeight();
            data.collOffsetX = collision.getOffset().x;
            data.collOffsetY = collision.getOffset().y;
        }
        var tileVisual = getComponent(com.polsl.poiw.engine.component.TileVisualStateComponent.class);
        if (tileVisual != null) {
            data.tileGid = tileVisual.getTileGid();
        }
        if (inventory != null) {
            data.inventory = inventory.buildSaveEntries();
        }
        return data;
    }

    public InventoryComponent getInventoryComponent() {
        return getComponent(InventoryComponent.class);
    }

    public List<InventoryStack> getInventoryItems() {
        InventoryComponent inventory = getInventoryComponent();
        return inventory != null ? inventory.getItemsSnapshot() : List.of();
    }

    public String getStorageTitle() {
        return storageTitle;
    }

    public int getStorageSlots() {
        return storageSlots;
    }

    public boolean isPlayerInInteractionRange(PlayerCharacter player) {
        if (player == null) {
            return false;
        }

        computeInteractionCenter(this, actorCenter);
        computeInteractionCenter(player, playerCenter);
        float halfWidthSum = getHalfWidth(this) + getHalfWidth(player) + INTERACTION_PADDING;
        float halfHeightSum = getHalfHeight(this) + getHalfHeight(player) + INTERACTION_PADDING;
        return Math.abs(actorCenter.x - playerCenter.x) <= halfWidthSum
            && Math.abs(actorCenter.y - playerCenter.y) <= halfHeightSum;
    }

    private void ensureInventoryConfigured(String title,
                                           int slotCount,
                                           List<SaveGameData.InventoryEntryData> inventoryEntries) {
        storageTitle = title != null && !title.isBlank() ? title : DEFAULT_STORAGE_TITLE;
        storageSlots = slotCount > 0 ? slotCount : DEFAULT_STORAGE_SLOTS;

        InventoryComponent inventory = getInventoryComponent();
        if (inventory == null) {
            inventory = new InventoryComponent();
            addComponent(inventory);
        }
        inventory.setMaxSlots(storageSlots);
        if (inventoryEntries != null) {
            inventory.restoreSaveEntries(inventoryEntries);
        }
    }

    private void computeInteractionCenter(com.polsl.poiw.engine.actor.Actor actor, Vector2 out) {
        if (actor == null) {
            out.setZero();
            return;
        }

        TransformComponent transform = actor.getComponent(TransformComponent.class);
        BoxCollisionComponent collision = actor.getComponent(BoxCollisionComponent.class);
        float centerX = actor.getPosition().x;
        float centerY = actor.getPosition().y;
        if (transform != null) {
            centerX += transform.getSize().x * 0.5f;
            centerY += transform.getSize().y * 0.5f;
        }
        if (collision != null) {
            centerX += collision.getOffset().x;
            centerY += collision.getOffset().y;
        }
        out.set(centerX, centerY);
    }

    private float getHalfWidth(com.polsl.poiw.engine.actor.Actor actor) {
        BoxCollisionComponent collision = actor != null ? actor.getComponent(BoxCollisionComponent.class) : null;
        if (collision != null) {
            return collision.getHalfWidth();
        }

        TransformComponent transform = actor != null ? actor.getComponent(TransformComponent.class) : null;
        return transform != null ? transform.getSize().x * 0.5f : 0.5f;
    }

    private float getHalfHeight(com.polsl.poiw.engine.actor.Actor actor) {
        BoxCollisionComponent collision = actor != null ? actor.getComponent(BoxCollisionComponent.class) : null;
        if (collision != null) {
            return collision.getHalfHeight();
        }

        TransformComponent transform = actor != null ? actor.getComponent(TransformComponent.class) : null;
        return transform != null ? transform.getSize().y * 0.5f : 0.5f;
    }

    private int getInt(Map<String, Object> properties, String key, int defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }
}

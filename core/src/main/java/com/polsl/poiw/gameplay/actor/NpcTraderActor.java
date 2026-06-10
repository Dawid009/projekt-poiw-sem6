package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.NpcAnimationComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.save.SaveGameData;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.trade.TradeOfferDefinition;
import com.polsl.poiw.gameplay.trade.TraderCatalog;
import com.polsl.poiw.gameplay.trade.TraderKind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcTraderActor extends AbstractActor {
    public static final String PROP_TRADER_KIND = "traderKind";
    public static final String PROP_SIZE_W = "sizeW";
    public static final String PROP_SIZE_H = "sizeH";
    public static final String PROP_COLL_HALF_W = "collHalfW";
    public static final String PROP_COLL_HALF_H = "collHalfH";
    public static final String PROP_COLL_OFFSET_X = "collOffsetX";
    public static final String PROP_COLL_OFFSET_Y = "collOffsetY";
    public static final String PROP_SORT_OFFSET_Y = "sortOffsetY";
    public static final String PROP_Z_ORDER = "zOrder";
    private static final float INTERACTION_PADDING = 1.0f;

    private final Vector2 actorCenter = new Vector2();
    private final Vector2 playerCenter = new Vector2();
    private TraderKind traderKind;

    public void configure(TextureAtlas npcAtlas,
                          TraderKind traderKind,
                          float sizeW,
                          float sizeH,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          float sortOffsetY,
                          int zOrder) {
        this.traderKind = traderKind;
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder);
        addComponent(new SpriteComponent(npcAtlas.findRegion(traderKind.getIdleAnimationRegion()), Color.WHITE.cpy()));
        addComponent(new NpcAnimationComponent(npcAtlas, traderKind.getIdleAnimationRegion()));
        ensureInventoryConfigured(null);
    }

    public void configure(TextureAtlas npcAtlas,
                          TraderKind traderKind,
                          float sizeW,
                          float sizeH,
                          float collHalfW,
                          float collHalfH,
                          Vector2 collOffset,
                          float sortOffsetY,
                          int zOrder,
                          List<SaveGameData.InventoryEntryData> inventoryEntries) {
        this.traderKind = traderKind;
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder);
        addComponent(new SpriteComponent(npcAtlas.findRegion(traderKind.getIdleAnimationRegion()), Color.WHITE.cpy()));
        addComponent(new NpcAnimationComponent(npcAtlas, traderKind.getIdleAnimationRegion()));
        ensureInventoryConfigured(inventoryEntries);
    }

    public void configureServer(TraderKind traderKind,
                                float sizeW,
                                float sizeH,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                float sortOffsetY,
                                int zOrder) {
        this.traderKind = traderKind;
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder);
        ensureInventoryConfigured(null);
    }

    public void configureServer(TraderKind traderKind,
                                float sizeW,
                                float sizeH,
                                float collHalfW,
                                float collHalfH,
                                Vector2 collOffset,
                                float sortOffsetY,
                                int zOrder,
                                List<SaveGameData.InventoryEntryData> inventoryEntries) {
        this.traderKind = traderKind;
        addSharedComponents(sizeW, sizeH, collHalfW, collHalfH, collOffset, sortOffsetY, zOrder);
        ensureInventoryConfigured(inventoryEntries);
    }

    public void configureFromReplication(TextureAtlas npcAtlas, Map<String, Object> initialProperties) {
        TraderKind kind = TraderKind.fromId(getString(initialProperties, PROP_TRADER_KIND, TraderKind.FARMER.getId()));
        float sizeW = getFloat(initialProperties, PROP_SIZE_W, 2f);
        float sizeH = getFloat(initialProperties, PROP_SIZE_H, 2f);
        float collHalfW = getFloat(initialProperties, PROP_COLL_HALF_W, 0.3f);
        float collHalfH = getFloat(initialProperties, PROP_COLL_HALF_H, 0.12f);
        float collOffsetX = getFloat(initialProperties, PROP_COLL_OFFSET_X, 0f);
        float collOffsetY = getFloat(initialProperties, PROP_COLL_OFFSET_Y, 0f);
        float sortOffsetY = getFloat(initialProperties, PROP_SORT_OFFSET_Y, 0f);
        int zOrder = getInt(initialProperties, PROP_Z_ORDER, 1);
        configure(npcAtlas, kind != null ? kind : TraderKind.FARMER, sizeW, sizeH, collHalfW, collHalfH,
            new Vector2(collOffsetX, collOffsetY), sortOffsetY, zOrder);
    }

    public Map<String, Object> buildInitialReplicationProperties() {
        Map<String, Object> properties = new HashMap<>();
        TransformComponent transform = getComponent(TransformComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        if (traderKind != null) {
            properties.put(PROP_TRADER_KIND, traderKind.getId());
        }
        if (transform != null) {
            properties.put(PROP_SIZE_W, transform.getSize().x);
            properties.put(PROP_SIZE_H, transform.getSize().y);
            properties.put(PROP_SORT_OFFSET_Y, transform.getSortOffsetY());
            properties.put(PROP_Z_ORDER, transform.getZOrder());
        }
        if (collision != null) {
            properties.put(PROP_COLL_HALF_W, collision.getHalfWidth());
            properties.put(PROP_COLL_HALF_H, collision.getHalfHeight());
            properties.put(PROP_COLL_OFFSET_X, collision.getOffset().x);
            properties.put(PROP_COLL_OFFSET_Y, collision.getOffset().y);
        }
        return properties;
    }

    public SaveGameData.TraderData buildSaveData() {
        SaveGameData.TraderData data = new SaveGameData.TraderData();
        TransformComponent transform = getComponent(TransformComponent.class);
        BoxCollisionComponent collision = getComponent(BoxCollisionComponent.class);
        InventoryComponent inventory = getInventoryComponent();

        data.traderKind = traderKind != null ? traderKind.getId() : "";
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
        if (inventory != null) {
            data.inventory = inventory.buildSaveEntries();
        }
        return data;
    }

    public TraderKind getTraderKind() {
        return traderKind;
    }

    public String getDisplayName() {
        return traderKind != null ? traderKind.getDisplayName() : "Trader";
    }

    public List<TradeOfferDefinition> getOffers() {
        return TraderCatalog.getOffers(traderKind);
    }

    public TradeOfferDefinition getOffer(String itemId) {
        return TraderCatalog.findOffer(traderKind, itemId);
    }

    public InventoryComponent getInventoryComponent() {
        return getComponent(InventoryComponent.class);
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

    private void addSharedComponents(float sizeW,
                                     float sizeH,
                                     float collHalfW,
                                     float collHalfH,
                                     Vector2 collOffset,
                                     float sortOffsetY,
                                     int zOrder) {
        addComponent(new TransformComponent(
            new Vector2(),
            zOrder,
            new Vector2(sizeW, sizeH),
            new Vector2(1f, 1f),
            0f,
            sortOffsetY
        ));

        if (collHalfW > 0f && collHalfH > 0f) {
            BoxCollisionComponent collision = new BoxCollisionComponent(
                CollisionProfile.ENVIRONMENT,
                collHalfW,
                collHalfH,
                collOffset != null ? new Vector2(collOffset) : new Vector2()
            );
            collision.setBodyTypeOverride(BodyDef.BodyType.StaticBody);
            addComponent(collision);
        }
    }

    private void ensureInventoryConfigured(List<SaveGameData.InventoryEntryData> inventoryEntries) {
        InventoryComponent inventory = getInventoryComponent();
        if (inventory == null) {
            inventory = new InventoryComponent();
            addComponent(inventory);
        }

        if (inventoryEntries != null) {
            inventory.restoreSaveEntries(inventoryEntries);
            return;
        }

        if (inventory.getOccupiedSlotCount() > 0 || traderKind == null) {
            return;
        }

        for (TradeOfferDefinition offer : TraderCatalog.getOffers(traderKind)) {
            inventory.addItem(offer.itemDefinition(), offer.initialStock());
        }
    }

    private void computeInteractionCenter(com.polsl.poiw.engine.actor.Actor actor, Vector2 out) {
        TransformComponent transform = actor != null ? actor.getComponent(TransformComponent.class) : null;
        BoxCollisionComponent collision = actor != null ? actor.getComponent(BoxCollisionComponent.class) : null;
        float centerX = actor != null ? actor.getPosition().x : 0f;
        float centerY = actor != null ? actor.getPosition().y : 0f;
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

    private float getFloat(Map<String, Object> properties, String key, float defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        return value instanceof Number number ? number.floatValue() : defaultValue;
    }

    private int getInt(Map<String, Object> properties, String key, int defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    private String getString(Map<String, Object> properties, String key, String defaultValue) {
        Object value = properties != null ? properties.get(key) : null;
        return value instanceof String stringValue && !stringValue.isBlank() ? stringValue : defaultValue;
    }
}

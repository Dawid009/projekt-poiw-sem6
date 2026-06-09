package com.polsl.poiw.gameplay.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.*;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.save.SaveGameData;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

import java.util.List;

/**
 * Główny aktor gracza.
 * Składa w jednym miejscu komponenty ruchu, walki, inventory i danych potrzebnych do save'a.
 */
public class PlayerCharacter extends AbstractActor {
    public static final int INVENTORY_SLOT_COUNT = 32;

    /** Prędkość gracza w metrach/s */
    private static final float PLAYER_SPEED = 3.5f;

    /** Maksymalne i początkowe HP */
    private static final float MAX_HEALTH = 100f;

    /** Klucz regionu w atlasie */
    private static final String PLAYER_REGION = "player";

    /** Rozmiar sprite'a w pikselach (32x32) */
    private static final float SPRITE_PX = 32f;

    public PlayerCharacter() {
        // components are added in configure() or configureServer()
    }

    /**
     * server configuration (headless) — without sprites and camera.
     * adds TransformComponent, MovementComponent, ControllerComponent, BoxCollisionComponent, HealthComponent.
     */
    public void configureServer() {
        float sizeW = SPRITE_PX / 16f;
        float sizeH = SPRITE_PX / 16f;

        addComponent(new TransformComponent(
            new Vector2(), 1, new Vector2(sizeW, sizeH)
        ));
        addComponent(new MovementComponent(PLAYER_SPEED));
        addComponent(new ControllerComponent());
        addComponent(CombatComponent.createPlayerMelee());
        addComponent(new HealthComponent(MAX_HEALTH, MAX_HEALTH));
        addComponent(new DamageReactionComponent());
        InventoryComponent inventory = new InventoryComponent();
        inventory.setMaxSlots(INVENTORY_SLOT_COUNT);
        addComponent(inventory);
        addComponent(new PlayerToolComponent());
        addComponent(new PlayerAssignedItemComponent());

        float ppm = 16f;
        float collHalfW = 9f / 2f / ppm;
        float collHalfH = 5f / 2f / ppm;
        float offsetX = (11f + 4.5f - 16f) / ppm;
        float offsetY = -((18f + 2.5f - 16f) / ppm);
        addComponent(new BoxCollisionComponent(
            CollisionProfile.PLAYER, collHalfW, collHalfH, new Vector2(offsetX, offsetY)
        ));
    }

    /**
     * Konfiguruje gracza z podanym atlasem (klient).
     * Wywoływane po stworzeniu, ale przed beginPlay().
     */
    public void configure(TextureAtlas atlas, TextureAtlas playerActionsAtlas) {
        TextureAtlas playerAtlas = resolvePlayerAtlas(atlas, playerActionsAtlas);
        TextureRegion region = playerAtlas != null ? playerAtlas.findRegion(PLAYER_REGION) : null;
        if (region == null) {
            throw new RuntimeException("Nie znaleziono regionu: " + PLAYER_REGION + " w atlasie");
        }

        float sizeW = SPRITE_PX * Main.UNIT_SCALE;
        float sizeH = SPRITE_PX * Main.UNIT_SCALE;

        addComponent(new TransformComponent(
            new Vector2(), 1, new Vector2(sizeW, sizeH)
        ));
        addComponent(new SpriteComponent(region, Color.WHITE.cpy()));


        // Animacje idle/walk zależne od kierunku i ruchu
        TextureAtlas actionAtlas = playerActionsAtlas != null ? playerActionsAtlas : playerAtlas;
        addComponent(new PlayerAnimationComponent(playerAtlas, actionAtlas));

        // Movement component - opisuje aktualny ruch i jego parametry
        addComponent(new MovementComponent(PLAYER_SPEED));
        addComponent(new CameraFollowComponent());
        addComponent(new ControllerComponent());
        addComponent(CombatComponent.createPlayerMelee());
        addComponent(new HealthComponent(MAX_HEALTH, MAX_HEALTH));
        addComponent(new DamageReactionComponent());
        InventoryComponent inventory = new InventoryComponent();
        inventory.setMaxSlots(INVENTORY_SLOT_COUNT);
        addComponent(inventory);
        addComponent(new PlayerToolComponent());
        addComponent(new PlayerAssignedItemComponent());

        // Kolizja gracza — kształt z objects.tsx: x=11,y=18,w=9,h=5 px (sprite 32x32)
        float ppm = 16f;
        float collHalfW = 9f / 2f / ppm;
        float collHalfH = 5f / 2f / ppm;
        float offsetX = (11f + 4.5f - 16f) / ppm;
        float offsetY = -((18f + 2.5f - 16f) / ppm);
        addComponent(new BoxCollisionComponent(
            CollisionProfile.PLAYER, collHalfW, collHalfH, new Vector2(offsetX, offsetY)
        ));

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.setSortOffsetY(sizeH / 2f + offsetY - collHalfH);
        }
    }

    private TextureAtlas resolvePlayerAtlas(TextureAtlas preferredAtlas, TextureAtlas fallbackAtlas) {
        if (preferredAtlas != null && preferredAtlas.findRegion(PLAYER_REGION) != null) {
            return preferredAtlas;
        }
        if (fallbackAtlas != null && fallbackAtlas.findRegion(PLAYER_REGION) != null) {
            return fallbackAtlas;
        }
        return preferredAtlas != null ? preferredAtlas : fallbackAtlas;
    }

    @Override
    public void beginPlay() {
        super.beginPlay();
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
    }

    // ===== System zdrowia (delegacja do HealthComponent) =====

    /** Zadaje obrażenia graczowi — deleguje do HealthComponent */
    public void applyDamage(float amount) {
        HealthComponent hc = getComponent(HealthComponent.class);
        if (hc != null) hc.applyDamage(amount);
    }

    /** Leczy gracza — deleguje do HealthComponent */
    public void heal(float amount) {
        HealthComponent hc = getComponent(HealthComponent.class);
        if (hc != null) hc.heal(amount);
    }

    public boolean isAlive() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null && hc.isAlive();
    }

    /** Obserwowalne HP — binduj do UI (bridge do HealthComponent) */
    public PropertyBinding<Float> getHealth() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null ? hc.getHealthProperty() : new PropertyBinding<>(0f);
    }

    /** Obserwowalne max HP — binduj do UI (bridge do HealthComponent) */
    public PropertyBinding<Float> getMaxHealth() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null ? hc.getMaxHealthProperty() : new PropertyBinding<>(0f);
    }

    public InventoryComponent getInventoryComponent() {
        return getComponent(InventoryComponent.class);
    }

    public PropertyBinding<Integer> getInventoryRevision() {
        InventoryComponent inventory = getInventoryComponent();
        return inventory != null ? inventory.getRevisionBinding() : new PropertyBinding<>(0);
    }

    public List<InventoryStack> getInventoryItems() {
        InventoryComponent inventory = getInventoryComponent();
        return inventory != null ? inventory.getItemsSnapshot() : List.of();
    }

    public PlayerToolComponent getPlayerToolComponent() {
        return getComponent(PlayerToolComponent.class);
    }

    public PlayerToolType getActiveTool() {
        PlayerToolComponent toolComponent = getPlayerToolComponent();
        return toolComponent != null ? toolComponent.getActiveTool() : PlayerToolType.SWORD;
    }

    public PlayerAssignedItemComponent getPlayerAssignedItemComponent() {
        return getComponent(PlayerAssignedItemComponent.class);
    }

    /** Buduje lekki zapis gracza bez zależności od reszty świata. */
    public SaveGameData.PlayerData buildSaveData() {
        SaveGameData.PlayerData data = new SaveGameData.PlayerData();
        TransformComponent transform = getComponent(TransformComponent.class);
        HealthComponent health = getComponent(HealthComponent.class);
        InventoryComponent inventory = getInventoryComponent();
        PlayerToolComponent toolComponent = getPlayerToolComponent();
        PlayerAssignedItemComponent assignedItemComponent = getPlayerAssignedItemComponent();

        if (transform != null) {
            data.x = transform.getPosition().x;
            data.y = transform.getPosition().y;
        } else {
            data.x = getPosition().x;
            data.y = getPosition().y;
        }
        if (health != null) {
            data.maxHealth = health.getMaxHealth();
            data.currentHealth = health.getCurrentHealth();
        }
        data.activeToolOrdinal = toolComponent != null ? toolComponent.getActiveTool().ordinal() : PlayerToolType.SWORD.ordinal();
        data.assignedItemId = assignedItemComponent != null ? assignedItemComponent.getAssignedItemId() : "";
        if (inventory != null) {
            data.inventory = inventory.buildSaveEntries();
        }
        return data;
    }

    /** Odtwarza pozycję, zdrowie, inventory i aktywne sloty gracza z danych save'a. */
    public void applySaveData(SaveGameData.PlayerData data) {
        if (data == null) {
            return;
        }

        setWorldPosition(data.x, data.y);

        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null) {
            float maxHealth = data.maxHealth > 0f ? data.maxHealth : MAX_HEALTH;
            float currentHealth = Math.max(0f, Math.min(maxHealth, data.currentHealth));
            health.restoreState(maxHealth, currentHealth);
        }

        InventoryComponent inventory = getInventoryComponent();
        if (inventory != null) {
            inventory.restoreSaveEntries(data.inventory);
        }

        PlayerToolComponent toolComponent = getPlayerToolComponent();
        if (toolComponent != null) {
            toolComponent.setActiveTool(PlayerToolType.fromOrdinal(data.activeToolOrdinal));
        }

        PlayerAssignedItemComponent assignedItemComponent = getPlayerAssignedItemComponent();
        if (assignedItemComponent != null) {
            assignedItemComponent.setAssignedItemId(data.assignedItemId);
        }
    }

    /**
     * Przenosi gracza razem z ciałem Box2D, żeby render i kolizja od razu były zgodne.
     */
    private void setWorldPosition(float x, float y) {
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.getPosition().set(x, y);
        } else {
            setPosition(x, y);
        }

        CollisionComponent collision = getComponentByType(CollisionComponent.class);
        if (collision == null || collision.getBody() == null) {
            return;
        }

        Vector2 size = transform != null ? transform.getSize() : new Vector2(SPRITE_PX * Main.UNIT_SCALE, SPRITE_PX * Main.UNIT_SCALE);
        Body body = collision.getBody();
        body.setTransform(x + size.x * 0.5f, y + size.y * 0.5f, 0f);
        body.setLinearVelocity(0f, 0f);
        collision.capturePreviousBodyPosition();
        collision.captureCurrentBodyPosition();
    }
}

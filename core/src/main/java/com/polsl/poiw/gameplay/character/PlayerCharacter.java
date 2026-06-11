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
import com.polsl.poiw.engine.component.CameraFollowComponent;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.ControllerComponent;
import com.polsl.poiw.engine.component.DamageReactionComponent;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.PlayerAnimationComponent;
import com.polsl.poiw.engine.component.PlayerAssignedItemComponent;
import com.polsl.poiw.engine.component.PlayerToolComponent;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TradeBasketComponent;
import com.polsl.poiw.engine.component.TransformComponent;
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

    private static final float PLAYER_SPEED = 3.5f;
    private static final float MAX_HEALTH = 100f;
    private static final float DEATH_ROTATION_DEG = -90f;
    private static final String PLAYER_REGION = "player";
    private static final float SPRITE_PX = 32f;
    private static final float PIXELS_PER_METER = 16f;
    private static final float COLLISION_WIDTH_PX = 9f;
    private static final float COLLISION_HEIGHT_PX = 5f;
    private static final float COLLISION_X_PX = 11f;
    private static final float COLLISION_Y_PX = 18f;
    private static final float SPRITE_CENTER_PX = 16f;
    private boolean deathStateApplied;
    private boolean deathDropHandled;

    public PlayerCharacter() {
    }

    /** Dodaje komponenty potrzebne do działania gracza po stronie serwera. */
    public void configureServer() {
        addCommonComponents(SPRITE_PX / PIXELS_PER_METER, false);
    }

    /**
     * Dodaje pełną konfigurację klienta, razem ze sprite'em i animacjami.
     *
     * @param atlas atlas bazowy gracza
     * @param playerActionsAtlas atlas animacji akcji
     */
    public void configure(TextureAtlas atlas, TextureAtlas playerActionsAtlas) {
        TextureAtlas playerAtlas = resolvePlayerAtlas(atlas, playerActionsAtlas);
        TextureRegion region = playerAtlas != null ? playerAtlas.findRegion(PLAYER_REGION) : null;
        if (region == null) {
            throw new RuntimeException("Nie znaleziono regionu: " + PLAYER_REGION + " w atlasie");
        }

        float size = SPRITE_PX * Main.UNIT_SCALE;
        addCommonComponents(size, true);
        addComponent(new SpriteComponent(region, Color.WHITE.cpy()));
        TextureAtlas actionAtlas = playerActionsAtlas != null ? playerActionsAtlas : playerAtlas;
        addComponent(new PlayerAnimationComponent(playerAtlas, actionAtlas));

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.setSortOffsetY(size * 0.5f + getCollisionOffsetY() - getCollisionHalfHeight());
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

    public void applyDamage(float amount) {
        HealthComponent hc = getComponent(HealthComponent.class);
        if (hc != null) hc.applyDamage(amount);
    }

    /** Leczy gracza o podaną wartość, z zachowaniem górnego limitu HP. */
    public void heal(float amount) {
        HealthComponent hc = getComponent(HealthComponent.class);
        if (hc != null) hc.heal(amount);
    }

    /** Zwraca {@code true}, gdy gracz ma dodatnią liczbę punktów życia. */
    public boolean isAlive() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null && hc.isAlive();
    }

    /** Zwraca {@code true}, gdy punkty życia spadły do zera. */
    public boolean isDead() {
        return !isAlive();
    }

    /** Zwraca bindowalne aktualne HP używane przez HUD i inne widoki. */
    public PropertyBinding<Float> getHealth() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null ? hc.getHealthProperty() : new PropertyBinding<>(0f);
    }

    /** Zwraca bindowalne maksymalne HP gracza. */
    public PropertyBinding<Float> getMaxHealth() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null ? hc.getMaxHealthProperty() : new PropertyBinding<>(0f);
    }

    /** Zwraca główne inventory gracza. */
    public InventoryComponent getInventoryComponent() {
        return getComponent(InventoryComponent.class);
    }

    /** Zwraca licznik zmian inventory do odświeżania UI. */
    public PropertyBinding<Integer> getInventoryRevision() {
        InventoryComponent inventory = getInventoryComponent();
        return inventory != null ? inventory.getRevisionBinding() : new PropertyBinding<>(0);
    }

    /** Zwraca migawkę zawartości inventory. */
    public List<InventoryStack> getInventoryItems() {
        InventoryComponent inventory = getInventoryComponent();
        return inventory != null ? inventory.getItemsSnapshot() : List.of();
    }

    /** Zwraca komponent aktywnego narzędzia gracza. */
    public PlayerToolComponent getPlayerToolComponent() {
        return getComponent(PlayerToolComponent.class);
    }

    /** Zwraca pomocniczy koszyk handlowy używany przy sprzedaży do NPC. */
    public TradeBasketComponent getTradeBasketComponent() {
        return getComponent(TradeBasketComponent.class);
    }

    /** Zwraca licznik zmian koszyka handlowego do odświeżania UI. */
    public PropertyBinding<Integer> getTradeBasketRevision() {
        TradeBasketComponent tradeBasket = getTradeBasketComponent();
        return tradeBasket != null ? tradeBasket.getRevisionBinding() : new PropertyBinding<>(0);
    }

    /** Zwraca migawkę zawartości koszyka handlowego. */
    public List<InventoryStack> getTradeBasketItems() {
        TradeBasketComponent tradeBasket = getTradeBasketComponent();
        return tradeBasket != null ? tradeBasket.getItemsSnapshot() : List.of();
    }

    /** Zwraca aktualnie wybrane narzędzie gracza. */
    public PlayerToolType getActiveTool() {
        PlayerToolComponent toolComponent = getPlayerToolComponent();
        return toolComponent != null ? toolComponent.getActiveTool() : PlayerToolType.SWORD;
    }

    /** Zwraca komponent przypisanego przedmiotu szybkiego użycia. */
    public PlayerAssignedItemComponent getPlayerAssignedItemComponent() {
        return getComponent(PlayerAssignedItemComponent.class);
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
        syncDeathState();
    }

    /** Przełącza gracza w stan śmierci bez niszczenia aktora. */
    public void enterDeathState() {
        MovementComponent movement = getComponent(MovementComponent.class);
        if (movement != null) {
            movement.getDirection().setZero();
            movement.setSprinting(false);
            movement.setRooted(true);
        }

        CollisionComponent collision = getComponentByType(CollisionComponent.class);
        Body body = collision != null ? collision.getBody() : null;
        if (body != null) {
            body.setLinearVelocity(0f, 0f);
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.setRotationDeg(DEATH_ROTATION_DEG);
        }

        deathStateApplied = true;
    }

    /** Informuje, czy po śmierci trzeba jeszcze wyrzucić przedmioty tego gracza. */
    public boolean needsDeathDropHandling() {
        return isDead() && !deathDropHandled;
    }

    /**
     * Czyści inventory i koszyk handlowy, zwracając listę stacków do wyrzucenia na mapę.
     *
     * @return lista stacków należących do gracza w momencie śmierci
     */
    public List<InventoryStack> clearOwnedItemsForDrop() {
        List<InventoryStack> droppedStacks = new java.util.ArrayList<>();

        InventoryComponent inventory = getInventoryComponent();
        if (inventory != null) {
            droppedStacks.addAll(inventory.clearAndExtractSnapshot());
        }

        TradeBasketComponent tradeBasket = getTradeBasketComponent();
        if (tradeBasket != null) {
            droppedStacks.addAll(tradeBasket.clearAndExtractSnapshot());
        }

        PlayerAssignedItemComponent assignedItemComponent = getPlayerAssignedItemComponent();
        if (assignedItemComponent != null) {
            assignedItemComponent.clearAssignedItem();
        }

        deathDropHandled = true;
        return droppedStacks;
    }

    /**
     * Przywraca gracza do życia i ustawia go w podanym punkcie odrodzenia.
     *
     * @param spawnPosition pozycja startowa po respawnie
     */
    public void respawnAt(Vector2 spawnPosition) {
        Vector2 resolvedSpawn = spawnPosition != null ? spawnPosition : new Vector2();
        setWorldPosition(resolvedSpawn.x, resolvedSpawn.y);

        HealthComponent health = getComponent(HealthComponent.class);
        if (health != null) {
            health.restoreState(health.getMaxHealth(), health.getMaxHealth());
        }

        MovementComponent movement = getComponent(MovementComponent.class);
        if (movement != null) {
            movement.getDirection().setZero();
            movement.setSprinting(false);
            movement.setRooted(false);
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.setRotationDeg(0f);
        }

        CollisionComponent collision = getComponentByType(CollisionComponent.class);
        Body body = collision != null ? collision.getBody() : null;
        if (body != null) {
            body.setLinearVelocity(0f, 0f);
        }

        deathStateApplied = false;
        deathDropHandled = false;
    }

    /** Buduje dane gracza zapisywane do savegame. */
    public SaveGameData.PlayerData buildSaveData() {
        SaveGameData.PlayerData data = new SaveGameData.PlayerData();
        TransformComponent transform = getComponent(TransformComponent.class);
        HealthComponent health = getComponent(HealthComponent.class);
        InventoryComponent inventory = getInventoryComponent();
        PlayerToolComponent toolComponent = getPlayerToolComponent();
        PlayerAssignedItemComponent assignedItemComponent = getPlayerAssignedItemComponent();
        TradeBasketComponent tradeBasketComponent = getTradeBasketComponent();

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
        if (tradeBasketComponent != null) {
            data.tradeBasket = tradeBasketComponent.buildSaveEntries();
        }
        return data;
    }

    /** Odtwarza stan gracza z danych savegame. */
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
        TradeBasketComponent tradeBasketComponent = getTradeBasketComponent();
        if (tradeBasketComponent != null) {
            tradeBasketComponent.restoreSaveEntries(data.tradeBasket);
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

    private void syncDeathState() {
        if (isDead()) {
            if (!deathStateApplied) {
                enterDeathState();
            }
            return;
        }

        if (!deathStateApplied) {
            return;
        }

        MovementComponent movement = getComponent(MovementComponent.class);
        if (movement != null) {
            movement.setRooted(false);
            movement.getDirection().setZero();
            movement.setSprinting(false);
        }

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.setRotationDeg(0f);
        }

        deathStateApplied = false;
    }

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

    private void addCommonComponents(float size, boolean addCameraFollow) {
        addComponent(new TransformComponent(new Vector2(), 1, new Vector2(size, size)));
        addComponent(new MovementComponent(PLAYER_SPEED));
        if (addCameraFollow) {
            addComponent(new CameraFollowComponent());
        }
        addComponent(new ControllerComponent());
        addComponent(CombatComponent.createPlayerMelee());
        addComponent(new HealthComponent(MAX_HEALTH, MAX_HEALTH));
        addComponent(new DamageReactionComponent());

        InventoryComponent inventory = new InventoryComponent();
        inventory.setMaxSlots(INVENTORY_SLOT_COUNT);
        addComponent(inventory);
        addComponent(new TradeBasketComponent());
        addComponent(new PlayerToolComponent());
        addComponent(new PlayerAssignedItemComponent());
        addComponent(createCollision());
    }

    private BoxCollisionComponent createCollision() {
        return new BoxCollisionComponent(
            CollisionProfile.PLAYER,
            getCollisionHalfWidth(),
            getCollisionHalfHeight(),
            new Vector2(getCollisionOffsetX(), getCollisionOffsetY())
        );
    }

    private float getCollisionHalfWidth() {
        return COLLISION_WIDTH_PX * 0.5f / PIXELS_PER_METER;
    }

    private float getCollisionHalfHeight() {
        return COLLISION_HEIGHT_PX * 0.5f / PIXELS_PER_METER;
    }

    private float getCollisionOffsetX() {
        return (COLLISION_X_PX + COLLISION_WIDTH_PX * 0.5f - SPRITE_CENTER_PX) / PIXELS_PER_METER;
    }

    private float getCollisionOffsetY() {
        return -((COLLISION_Y_PX + COLLISION_HEIGHT_PX * 0.5f - SPRITE_CENTER_PX) / PIXELS_PER_METER);
    }
}

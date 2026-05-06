package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.binding.BindingHandle;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.EVisibility;
import com.polsl.poiw.engine.ui.InventoryPanelWidget;
import com.polsl.poiw.engine.ui.PauseMenuWidget;
import com.polsl.poiw.engine.ui.ProgressBarWidget;
import com.polsl.poiw.engine.ui.SettingsPanelWidget;
import com.polsl.poiw.engine.ui.TextBlock;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.item.GameplayItems;

/**
 * Controller gracza — tworzy HUD z wyświetlaniem HP
 * i binduje go do PropertyBinding z PlayerCharacter.
 */
public class MainPlayerController extends PlayerController {

    private TextBlock hpText;
    private ProgressBarWidget progressBar;
    private InventoryPanelWidget inventoryPanel;
    private PauseMenuWidget pauseMenu;
    private SettingsPanelWidget settingsPanel;
    private BindingHandle healthBinding;
    private BindingHandle maxHealthBinding;
    private BindingHandle inventoryBinding;

    /** Aktualne wartości do formatowania tekstu */
    private float currentHp = 0f;
    private float currentMaxHp = 0f;

    @Override
    protected void setupHUD() {
        // TextBlock wyświetlający HP — lewy górny róg
        hpText = new TextBlock("HP: ---", getSkin());
        hpText.setAnchor(EAnchor.BOTTOM_CENTER);
        hpText.setAlignment(EAnchor.TOP_CENTER);
        hpText.setOffset(0f, -4f);
        hpText.setColor(Color.WHITE);
        hpText.setFontScale(1f);
        hpText.setVariable(true);

        progressBar = new ProgressBarWidget(0, 100, 1, false, getSkin(), "curved");
        progressBar.setBarColor(Color.RED);
        progressBar.setAnchor(EAnchor.TOP_LEFT);
        progressBar.setAlignment(EAnchor.TOP_LEFT);
        progressBar.setOffset(5f, -10f);
        progressBar.setVariable(true);
        progressBar.setValue(50f);
        progressBar.setBarSize(100f, 10f);

        progressBar.addChild(hpText);

        addWidgetToViewport(progressBar);

        inventoryPanel = new InventoryPanelWidget(getSkin(), getItemsAtlas());
        inventoryPanel.setAnchor(EAnchor.CENTER);
        inventoryPanel.setAlignment(EAnchor.CENTER);
        inventoryPanel.setOffset(0f, 0f);
        inventoryPanel.setActionListener(new InventoryPanelWidget.InventoryActionListener() {
            @Override
            public void onUseRequested(String itemId) {
                useSelectedItem(itemId);
            }

            @Override
            public void onDropRequested(String itemId) {
                dropSelectedItem(itemId);
            }
        });
        addWidgetToViewport(inventoryPanel);

        pauseMenu = new PauseMenuWidget(getSkin());
        pauseMenu.setAnchor(EAnchor.CENTER);
        pauseMenu.setAlignment(EAnchor.CENTER);
        pauseMenu.setActionListener(new PauseMenuWidget.PauseMenuActionListener() {
            @Override
            public void onResumeRequested() {
                hidePauseMenu();
            }

            @Override
            public void onOptionsRequested() {
                openSettingsFromPauseMenu();
            }

            @Override
            public void onQuitRequested() {
                quitToMainMenu();
            }
        });
        addWidgetToViewport(pauseMenu);

        settingsPanel = new SettingsPanelWidget(getSkin());
        settingsPanel.setAnchor(EAnchor.CENTER);
        settingsPanel.setAlignment(EAnchor.CENTER);
        settingsPanel.setCloseAction(this::closeSettingsToPauseMenu);
        addWidgetToViewport(settingsPanel);
    }

    @Override
    protected void onPossess(Actor pawn) {
        // Binduj HP z PlayerCharacter do TextBlock
        if (pawn instanceof PlayerCharacter player) {
            healthBinding = player.getHealth().bind(val -> {
                currentHp = val;
                updateHpText();
            });
            maxHealthBinding = player.getMaxHealth().bind(val -> {
                currentMaxHp = val;
                updateHpText();
            });
            inventoryBinding = player.getInventoryRevision().bind(revision -> inventoryPanel.setItems(player.getInventoryItems()));
        }
    }

    @Override
    protected void onUnpossess() {
        if (healthBinding != null) {
            healthBinding.unbind();
            healthBinding = null;
        }
        if (maxHealthBinding != null) {
            maxHealthBinding.unbind();
            maxHealthBinding = null;
        }
        if (inventoryBinding != null) {
            inventoryBinding.unbind();
            inventoryBinding = null;
        }
        if (inventoryPanel != null) {
            inventoryPanel.setItems(java.util.List.of());
            inventoryPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
    }

    @Override
    public void destroy() {
        onUnpossess();
        super.destroy();
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
        handlePauseToggle();

        if (isOverlayVisible()) {
            return;
        }

        handleInventoryToggle();
        handleDebugItemSpawn();
    }

    private void updateHpText() {
        if (hpText != null) {
            int hp = Math.round(currentHp);
            int max = Math.round(currentMaxHp);
            hpText.setText(hp + " / " + max);
            progressBar.setValue(hp);

            // Kolor zależny od poziomu HP
            float ratio = currentMaxHp > 0 ? currentHp / currentMaxHp : 0f;
            if (ratio > 0.5f) {
                hpText.setColor(Color.WHITE);
            } else if (ratio > 0.25f) {
                hpText.setColor(Color.YELLOW);
            } else {
                hpText.setColor(Color.RED);
            }
        }
    }

    private void handleInventoryToggle() {
        if (inventoryPanel == null || Gdx.input == null) {
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            inventoryPanel.setVisibility(inventoryPanel.isVisible()
                ? EVisibility.HIDDEN
                : EVisibility.VISIBLE);
        }
    }

    private void handleDebugItemSpawn() {
        if (!(getPossessedPawn() instanceof PlayerCharacter player) || !player.hasAuthority()) {
            return;
        }

        spawnDebugItemIfPressed(player, Input.Keys.NUM_1, 0);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_2, 1);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_3, 2);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_4, 3);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_5, 4);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_6, 5);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_7, 6);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_8, 7);
        spawnDebugItemIfPressed(player, Input.Keys.NUM_9, 8);
    }

    private void spawnDebugItemIfPressed(PlayerCharacter player, int keycode, int debugSlot) {
        if (!Gdx.input.isKeyJustPressed(keycode)) {
            return;
        }

        ItemDefinition item = GameplayItems.getDebugItem(debugSlot);
        if (item != null) {
            spawnItemNearPlayer(player, item, 1, 0.2f, 0.25f);
        }
    }

    private void useSelectedItem(String itemId) {
        if (!(getPossessedPawn() instanceof PlayerCharacter player) || !player.hasAuthority()) {
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        if (inventory != null) {
            inventory.useItem(itemId);
        }
    }

    private void dropSelectedItem(String itemId) {
        if (!(getPossessedPawn() instanceof PlayerCharacter player) || !player.hasAuthority()) {
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        if (inventory == null) {
            return;
        }

        InventoryStack stack = inventory.getStack(itemId);
        if (stack == null) {
            return;
        }

        if (inventory.removeItem(itemId, 1) > 0) {
            spawnItemNearPlayer(player, stack.getDefinition(), 1, 0.35f, 0.45f);
        }
    }

    private void spawnItemNearPlayer(PlayerCharacter player, ItemDefinition item, int quantity,
                                     float heightOffset, float pickupGraceSeconds) {
        GameWorld world = getWorld();
        if (world == null) {
            return;
        }

        TransformComponent transform = player.getComponent(TransformComponent.class);
        Vector2 playerPosition = player.getPosition();
        float playerWidth = transform != null ? transform.getSize().x : 1f;
        float playerHeight = transform != null ? transform.getSize().y : 1f;
        float itemSize = 0.5f;

        Vector2 spawnPosition = new Vector2(
            playerPosition.x + playerWidth * 0.5f - itemSize * 0.5f,
            playerPosition.y + playerHeight + heightOffset
        );

        ItemPickupActor pickupActor = new ItemPickupActor();
        pickupActor.configure(item, quantity, getItemsAtlas());
        pickupActor.setPickupGrace(player.getActorId(), pickupGraceSeconds);
        world.spawnActor(pickupActor, spawnPosition);
    }

    private void handlePauseToggle() {
        if (Gdx.input == null || !Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return;
        }

        if (settingsPanel != null && settingsPanel.isVisible()) {
            closeSettingsToPauseMenu();
            return;
        }

        if (pauseMenu != null && pauseMenu.isVisible()) {
            hidePauseMenu();
            return;
        }

        showPauseMenu();
    }

    private boolean isOverlayVisible() {
        return (pauseMenu != null && pauseMenu.isVisible())
            || (settingsPanel != null && settingsPanel.isVisible());
    }

    private void showPauseMenu() {
        if (inventoryPanel != null) {
            inventoryPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.VISIBLE);
        }
    }

    private void hidePauseMenu() {
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
    }

    private void openSettingsFromPauseMenu() {
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.refreshFromAppliedSettings();
            settingsPanel.setVisibility(EVisibility.VISIBLE);
        }
    }

    private void closeSettingsToPauseMenu() {
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.VISIBLE);
        }
    }

    private void quitToMainMenu() {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance != null) {
            gameInstance.returnToMenu("Wyjscie do menu glownego");
        }
    }
}

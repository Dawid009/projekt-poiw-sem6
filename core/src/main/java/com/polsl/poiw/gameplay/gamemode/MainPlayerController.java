package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.auth.AuthService;
import com.polsl.poiw.engine.binding.BindingHandle;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.PlayerToolComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.EVisibility;
import com.polsl.poiw.engine.ui.InventoryPanelWidget;
import com.polsl.poiw.engine.ui.PauseMenuWidget;
import com.polsl.poiw.engine.ui.ProgressBarWidget;
import com.polsl.poiw.engine.ui.SettingsPanelWidget;
import com.polsl.poiw.engine.ui.StatsPanelWidget;
import com.polsl.poiw.engine.ui.TextBlock;
import com.polsl.poiw.engine.ui.ToolbeltWidget;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.item.GameplayItems;
import com.polsl.poiw.gameplay.tool.PlayerToolType;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller gracza — tworzy HUD z wyświetlaniem HP
 * i binduje go do PropertyBinding z PlayerCharacter.
 */
public class MainPlayerController extends PlayerController {

    private TextBlock hpText;
    private ProgressBarWidget progressBar;
    private InventoryPanelWidget inventoryPanel;
    private ToolbeltWidget toolbeltWidget;
    private PauseMenuWidget pauseMenu;
    private SettingsPanelWidget settingsPanel;
    private StatsPanelWidget statsPanel;
    private BindingHandle healthBinding;
    private BindingHandle maxHealthBinding;
    private BindingHandle inventoryBinding;
    private BindingHandle toolBinding;
    private final Map<String, Integer> trackedInventoryQuantities = new HashMap<>();
    private PlayerToolType lastObservedTool;

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

        toolbeltWidget = new ToolbeltWidget(getSkin(), getItemsAtlas());
        toolbeltWidget.setAnchor(EAnchor.BOTTOM_RIGHT);
        toolbeltWidget.setAlignment(EAnchor.BOTTOM_RIGHT);
        toolbeltWidget.setOffset(-6f, 6f);
        toolbeltWidget.setVisibility(EVisibility.HIDDEN);
        addWidgetToViewport(toolbeltWidget);

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
            public void onStatsRequested() {
                openStatsPanel();
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

        statsPanel = new StatsPanelWidget(getSkin());
        statsPanel.setAnchor(EAnchor.CENTER);
        statsPanel.setAlignment(EAnchor.CENTER);
        statsPanel.setActionListener(new StatsPanelWidget.StatsPanelActionListener() {
            @Override
            public void onRefreshRequested() {
                refreshStatsPanel();
            }

            @Override
            public void onCloseRequested() {
                closeStatsPanel();
            }
        });
        addWidgetToViewport(statsPanel);
        updateStatsUiState();
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
            List<InventoryStack> initialItems = player.getInventoryItems();
            inventoryPanel.setItems(initialItems);
            resetInventoryTracking(initialItems);
            inventoryBinding = player.getInventoryRevision().bind(revision -> handleInventoryChanged(player));
            PlayerToolComponent toolComponent = player.getPlayerToolComponent();
            if (toolComponent != null) {
                lastObservedTool = null;
                toolBinding = toolComponent.getActiveToolBinding().bind(this::handleToolChanged);
            }
            if (toolbeltWidget != null) {
                toolbeltWidget.setVisibility(EVisibility.VISIBLE);
            }
            updateStatsUiState();
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
        if (toolBinding != null) {
            toolBinding.unbind();
            toolBinding = null;
        }
        lastObservedTool = null;
        trackedInventoryQuantities.clear();
        if (inventoryPanel != null) {
            inventoryPanel.setItems(java.util.List.of());
            inventoryPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (toolbeltWidget != null) {
            toolbeltWidget.setVisibility(EVisibility.HIDDEN);
            toolbeltWidget.setSelectedTool(PlayerToolType.SWORD);
        }
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
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

        // Skip all key handling when chat input is active
        if (GameInstance.isChatInputActive()) {
            return;
        }

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

    private void handleInventoryChanged(PlayerCharacter player) {
        List<InventoryStack> items = player.getInventoryItems();
        inventoryPanel.setItems(items);
        trackInventoryStats(items);
    }

    private void resetInventoryTracking(List<InventoryStack> items) {
        trackedInventoryQuantities.clear();
        trackedInventoryQuantities.putAll(buildInventoryQuantityMap(items));
    }

    private void trackInventoryStats(List<InventoryStack> items) {
        Map<String, Integer> currentQuantities = buildInventoryQuantityMap(items);
        int collectedCrops = computePositiveDelta(currentQuantities, GameplayItems.ITEM_CARROT.getItemId())
            + computePositiveDelta(currentQuantities, GameplayItems.ITEM_WHEAT.getItemId());
        int collectedResources = computePositiveDelta(currentQuantities, GameplayItems.IRON_ORE.getItemId())
            + computePositiveDelta(currentQuantities, GameplayItems.GOLD_ORE.getItemId());

        trackedInventoryQuantities.clear();
        trackedInventoryQuantities.putAll(currentQuantities);

        GameInstance gameInstance = getGameInstance();
        AuthService authService = gameInstance != null ? gameInstance.getAuthService() : null;
        if (authService == null) {
            return;
        }

        if (collectedCrops > 0) {
            authService.recordCollectedCrops(collectedCrops);
        }
        if (collectedResources > 0) {
            authService.recordCollectedResources(collectedResources);
        }
    }

    private int computePositiveDelta(Map<String, Integer> currentQuantities, String itemId) {
        int previous = trackedInventoryQuantities.getOrDefault(itemId, 0);
        int current = currentQuantities.getOrDefault(itemId, 0);
        return Math.max(0, current - previous);
    }

    private Map<String, Integer> buildInventoryQuantityMap(List<InventoryStack> items) {
        Map<String, Integer> quantities = new HashMap<>();
        if (items == null) {
            return quantities;
        }

        for (InventoryStack stack : items) {
            if (stack == null || stack.getDefinition() == null) {
                continue;
            }

            quantities.merge(stack.getDefinition().getItemId(), stack.getQuantity(), Integer::sum);
        }

        return quantities;
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
        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        if (!player.hasAuthority()) {
            requestInventoryAction(itemId, NetworkProtocol.InventoryActionType.USE);
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        if (inventory != null) {
            inventory.useItem(itemId);
        }
    }

    private void dropSelectedItem(String itemId) {
        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        if (!player.hasAuthority()) {
            requestInventoryAction(itemId, NetworkProtocol.InventoryActionType.DROP);
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

    private void requestInventoryAction(String itemId, NetworkProtocol.InventoryActionType actionType) {
        if (itemId == null || itemId.isBlank() || actionType == null) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientInventoryAction request = new NetworkProtocol.ClientInventoryAction();
        request.playerId = getPlayerId();
        request.itemId = itemId;
        request.action = actionType;
        gameInstance.getNetDriver().sendToServer(request, true);
    }

    private void handleToolChanged(PlayerToolType toolType) {
        PlayerToolType resolvedTool = toolType != null ? toolType : PlayerToolType.SWORD;
        if (toolbeltWidget != null) {
            toolbeltWidget.setSelectedTool(resolvedTool);
        }

        if (lastObservedTool == null) {
            lastObservedTool = resolvedTool;
            return;
        }

        if (resolvedTool == lastObservedTool) {
            return;
        }

        lastObservedTool = resolvedTool;
        requestToolSelection(resolvedTool);
    }

    private void requestToolSelection(PlayerToolType toolType) {
        if (toolType == null) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientToolSelection request = new NetworkProtocol.ClientToolSelection();
        request.playerId = getPlayerId();
        request.toolOrdinal = toolType.ordinal();
        gameInstance.getNetDriver().sendToServer(request, true);
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

        if (statsPanel != null && statsPanel.isVisible()) {
            closeStatsPanel();
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
            || (settingsPanel != null && settingsPanel.isVisible())
            || (statsPanel != null && statsPanel.isVisible());
    }

    private void openStatsPanel() {
        if (statsPanel == null) {
            return;
        }

        if (inventoryPanel != null) {
            inventoryPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }

        statsPanel.setVisibility(EVisibility.VISIBLE);
        refreshStatsPanel();
    }

    private void closeStatsPanel() {
        if (statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null && isStatsAvailable()) {
            pauseMenu.setVisibility(EVisibility.VISIBLE);
        }
    }

    private void refreshStatsPanel() {
        if (statsPanel == null) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        AuthService authService = gameInstance != null ? gameInstance.getAuthService() : null;
        if (authService == null || !authService.isAuthenticated() || authService.isOfflineSession()) {
            statsPanel.showError("Statystyki sa dostepne tylko po zalogowaniu.");
            return;
        }

        statsPanel.showLoading();
        authService.fetchCurrentStats(new AuthService.StatsResultListener() {
            @Override
            public void onSuccess(AuthService.PlayerStatsSnapshot stats) {
                if (statsPanel == null || !statsPanel.isAddedToViewport()) {
                    return;
                }

                statsPanel.setStats(stats);
            }

            @Override
            public void onFailure(String message) {
                if (statsPanel == null || !statsPanel.isAddedToViewport()) {
                    return;
                }

                statsPanel.showError(message);
            }
        });
    }

    private void updateStatsUiState() {
        boolean statsAvailable = isStatsAvailable();
        if (pauseMenu != null) {
            pauseMenu.setStatsVisible(statsAvailable);
        }
        if (!statsAvailable && statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
        }
    }

    private boolean isStatsAvailable() {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null) {
            return false;
        }

        AuthService authService = gameInstance.getAuthService();
        return authService != null && authService.isAuthenticated() && !authService.isOfflineSession();
    }

    private void showPauseMenu() {
        if (inventoryPanel != null) {
            inventoryPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
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

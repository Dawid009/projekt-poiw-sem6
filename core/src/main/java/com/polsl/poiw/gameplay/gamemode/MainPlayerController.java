package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.asset.SkinAsset;
import com.polsl.poiw.engine.auth.AuthService;
import com.polsl.poiw.engine.binding.BindingHandle;
import com.polsl.poiw.engine.component.CombatComponent;
import com.polsl.poiw.engine.component.ControllerComponent;
import com.polsl.poiw.engine.component.InventoryComponent;
import com.polsl.poiw.engine.component.PlayerAssignedItemComponent;
import com.polsl.poiw.engine.component.PlayerToolComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
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
import com.polsl.poiw.gameplay.actor.ChestActor;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.crop.CropPlantingService;
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
    private static final float SAVE_STATUS_DURATION_SECONDS = 2.2f;
    private static final float CHEST_PANEL_SPACING = 8f;
    /**
     * Wygasza chwilowy komunikat o zapisie bez mieszania w reszcie HUD-u.
     */
    private static final int TOOL_SLOT_COUNT = PlayerToolType.values().length;
    private static final int ASSIGNED_ITEM_SLOT_INDEX = TOOL_SLOT_COUNT;

    private TextBlock hpText;
    private ProgressBarWidget progressBar;
    private InventoryPanelWidget inventoryPanel;
    private InventoryPanelWidget chestPanel;
    private ToolbeltWidget toolbeltWidget;
    private PauseMenuWidget pauseMenu;
    private TextBlock saveStatusText;
    private SettingsPanelWidget settingsPanel;
    private StatsPanelWidget statsPanel;
    private BindingHandle healthBinding;
    private BindingHandle maxHealthBinding;
    private BindingHandle inventoryBinding;
    private BindingHandle chestInventoryBinding;
    private BindingHandle assignedItemBinding;
    private BindingHandle toolBinding;
    private final Map<String, Integer> trackedInventoryQuantities = new HashMap<>();
    private PlayerToolType lastObservedTool;
    private String assignedItemId = "";
    private int selectedHotbarSlot = 0;
    private float saveStatusTimer;
    private ChestActor activeChest;

    /** Aktualne wartości do formatowania tekstu */
    private float currentHp = 0f;
    private float currentMaxHp = 0f;

    @Override
    protected void setupHUD() {
        Skin overlaySkin = resolveOverlaySkin();

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
            public void onUseRequested(int slotIndex, String itemId) {
                useSelectedItem(slotIndex, itemId);
            }

            @Override
            public void onDropRequested(int slotIndex, String itemId, boolean wholeStack) {
                dropSelectedItem(slotIndex, itemId, wholeStack);
            }

            @Override
            public void onAssignRequested(int slotIndex, String itemId) {
                assignSelectedItem(slotIndex, itemId);
            }

            @Override
            public void onQuickTransferRequested(int slotIndex, String itemId, boolean wholeStack) {
                transferInventoryItemToChest(slotIndex, itemId, wholeStack);
            }
        });
        addWidgetToViewport(inventoryPanel);

        chestPanel = new InventoryPanelWidget(getSkin(), getItemsAtlas(), "Skrzynia", 4, 4, false);
        chestPanel.setAnchor(EAnchor.CENTER);
        chestPanel.setAlignment(EAnchor.CENTER);
        chestPanel.setVisibility(EVisibility.HIDDEN);
        chestPanel.setActionListener(new InventoryPanelWidget.InventoryActionListener() {
            @Override
            public void onUseRequested(int slotIndex, String itemId) {
            }

            @Override
            public void onDropRequested(int slotIndex, String itemId, boolean wholeStack) {
            }

            @Override
            public void onAssignRequested(int slotIndex, String itemId) {
            }

            @Override
            public void onQuickTransferRequested(int slotIndex, String itemId, boolean wholeStack) {
                transferChestItemToInventory(slotIndex, itemId, wholeStack);
            }
        });
        addWidgetToViewport(chestPanel);

        toolbeltWidget = new ToolbeltWidget(getSkin(), getItemsAtlas());
        toolbeltWidget.setAnchor(EAnchor.BOTTOM_RIGHT);
        toolbeltWidget.setAlignment(EAnchor.BOTTOM_RIGHT);
        toolbeltWidget.setOffset(-6f, 6f);
        toolbeltWidget.setVisibility(EVisibility.HIDDEN);
        addWidgetToViewport(toolbeltWidget);

        pauseMenu = new PauseMenuWidget(overlaySkin);
        pauseMenu.setAnchor(EAnchor.CENTER);
        pauseMenu.setAlignment(EAnchor.CENTER);
        pauseMenu.setActionListener(new PauseMenuWidget.PauseMenuActionListener() {
            @Override
            public void onResumeRequested() {
                hidePauseMenu();
            }

            @Override
            public void onSaveRequested() {
                saveGameProgress();
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
        pauseMenu.setSaveVisible(getGameInstance() != null && getGameInstance().isSinglePlayer());
        addWidgetToViewport(pauseMenu);

        saveStatusText = new TextBlock("Zapisano gre", getSkin());
        saveStatusText.setAnchor(EAnchor.TOP_CENTER);
        saveStatusText.setAlignment(EAnchor.TOP_CENTER);
        saveStatusText.setOffset(0f, -14f);
        saveStatusText.setColor(new Color(0.75f, 1f, 0.75f, 1f));
        saveStatusText.setFontScale(0.9f);
        saveStatusText.setVariable(true);
        saveStatusText.setVisibility(EVisibility.HIDDEN);
        addWidgetToViewport(saveStatusText);

        settingsPanel = new SettingsPanelWidget(overlaySkin);
        settingsPanel.setAnchor(EAnchor.CENTER);
        settingsPanel.setAlignment(EAnchor.CENTER);
        settingsPanel.setCloseAction(this::closeSettingsToPauseMenu);
        addWidgetToViewport(settingsPanel);

        statsPanel = new StatsPanelWidget(overlaySkin);
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

    private Skin resolveOverlaySkin() {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance != null) {
            var context = gameInstance.getActiveWorldContext();
            if (context != null) {
                Main game = context.getGame();
                if (game != null && game.getAssetService() != null) {
                    return game.getAssetService().get(SkinAsset.MENU);
                }
            }
        }
        return getSkin();
    }

    @Override
    protected void onPossess(Actor pawn) {
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

            PlayerAssignedItemComponent assignedItemComponent = player.getPlayerAssignedItemComponent();
            if (assignedItemComponent != null) {
                assignedItemBinding = assignedItemComponent.getAssignedItemIdBinding().bind(this::handleAssignedItemChanged);
            }

            PlayerToolComponent toolComponent = player.getPlayerToolComponent();
            if (toolComponent != null) {
                lastObservedTool = null;
                toolBinding = toolComponent.getActiveToolBinding().bind(this::handleToolChanged);
            }

            if (toolbeltWidget != null) {
                toolbeltWidget.setVisibility(EVisibility.VISIBLE);
                toolbeltWidget.setSelectedSlot(selectedHotbarSlot);
            }
            refreshAssignedItemSlot(player);
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
        if (chestInventoryBinding != null) {
            chestInventoryBinding.unbind();
            chestInventoryBinding = null;
        }
        if (assignedItemBinding != null) {
            assignedItemBinding.unbind();
            assignedItemBinding = null;
        }
        if (toolBinding != null) {
            toolBinding.unbind();
            toolBinding = null;
        }

        lastObservedTool = null;
        assignedItemId = "";
        selectedHotbarSlot = 0;
        trackedInventoryQuantities.clear();
        activeChest = null;

        if (inventoryPanel != null) {
            inventoryPanel.setItems(java.util.List.of());
            inventoryPanel.setHiddenItemId(null);
            inventoryPanel.setVisibility(EVisibility.HIDDEN);
            inventoryPanel.setOffset(0f, 0f);
        }
        if (chestPanel != null) {
            chestPanel.setItems(java.util.List.of());
            chestPanel.setVisibility(EVisibility.HIDDEN);
            chestPanel.setOffset(0f, 0f);
        }
        if (toolbeltWidget != null) {
            toolbeltWidget.setVisibility(EVisibility.HIDDEN);
            toolbeltWidget.setAssignedItem(null);
            toolbeltWidget.setSelectedSlot(selectedHotbarSlot);
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
        if (saveStatusText != null) {
            saveStatusText.setVisibility(EVisibility.HIDDEN);
        }
        saveStatusTimer = 0f;
    }

    @Override
    public void destroy() {
        onUnpossess();
        super.destroy();
    }

    @Override
    public void tick(float delta) {
        updateSaveStatus(delta);

        if (GameInstance.isChatInputActive()) {
            clearPendingAttackState();
            super.tick(delta);
            return;
        }

        handlePauseToggle();
        if (isOverlayVisible()) {
            clearPendingAttackState();
            super.tick(delta);
            return;
        }

        handleInventoryToggle();
        updateChestInteractionState();
        if (isInventoryInteractionActive()) {
            clearPendingAttackState();
            super.tick(delta);
            return;
        }

        handleHotbarSelection();
        handleToolAttack();
        handleAssignedItemUse();
        super.tick(delta);
    }

    private void updateSaveStatus(float delta) {
        if (saveStatusText == null || !saveStatusText.isVisible() || delta <= 0f) {
            return;
        }

        saveStatusTimer = Math.max(0f, saveStatusTimer - delta);
        if (saveStatusTimer <= 0f) {
            saveStatusText.setVisibility(EVisibility.HIDDEN);
        }
    }

    /** Pokazuje krótki komunikat na górze ekranu, np. po ręcznym zapisie. */
    private void showSaveStatus(String text, Color color) {
        if (saveStatusText == null) {
            return;
        }

        saveStatusText.setText(text);
        saveStatusText.setColor(color);
        saveStatusText.setVisibility(EVisibility.VISIBLE);
        saveStatusTimer = SAVE_STATUS_DURATION_SECONDS;
    }

    private void updateHpText() {
        if (hpText != null) {
            int hp = Math.round(currentHp);
            int max = Math.round(currentMaxHp);
            hpText.setText(hp + " / " + max);
            progressBar.setValue(hp);

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
        refreshAssignedItemSlot(player);
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
            boolean nextVisible = !inventoryPanel.isVisible();
            inventoryPanel.setVisibility(nextVisible ? EVisibility.VISIBLE : EVisibility.HIDDEN);
            if (!nextVisible) {
                setActiveChest(null);
            }
        }
    }

    private void updateChestInteractionState() {
        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            setActiveChest(null);
            return;
        }

        if (inventoryPanel == null || !inventoryPanel.isVisible()) {
            setActiveChest(null);
            return;
        }

        setActiveChest(findNearbyChest(player));
    }

    private ChestActor findNearbyChest(PlayerCharacter player) {
        GameWorld world = getWorld();
        if (world == null || player == null) {
            return null;
        }

        ChestActor nearestChest = null;
        float bestDistance2 = Float.MAX_VALUE;
        for (ChestActor chest : world.getActorsOfClass(ChestActor.class)) {
            if (chest == null || !chest.isPlayerInInteractionRange(player)) {
                continue;
            }

            float distance2 = chest.getPosition().dst2(player.getPosition());
            if (distance2 < bestDistance2) {
                bestDistance2 = distance2;
                nearestChest = chest;
            }
        }
        return nearestChest;
    }

    private void setActiveChest(ChestActor chest) {
        if (activeChest == chest) {
            if (activeChest != null && chestPanel != null && chestPanel.isVisible()) {
                updateInventoryPanelLayout();
            }
            return;
        }

        if (chestInventoryBinding != null) {
            chestInventoryBinding.unbind();
            chestInventoryBinding = null;
        }

        activeChest = chest;
        if (chestPanel == null) {
            return;
        }

        if (activeChest == null) {
            chestPanel.setItems(List.of());
            chestPanel.setVisibility(EVisibility.HIDDEN);
            updateInventoryPanelLayout();
            return;
        }

        chestPanel.setTitle(activeChest.getStorageTitle());
        chestPanel.setItems(activeChest.getInventoryItems());
        chestPanel.setVisibility(EVisibility.VISIBLE);
        updateInventoryPanelLayout();

        InventoryComponent chestInventory = activeChest.getInventoryComponent();
        if (chestInventory != null) {
            ChestActor boundChest = activeChest;
            chestInventoryBinding = chestInventory.getRevisionBinding().bind(revision -> handleChestInventoryChanged(boundChest));
        }
    }

    private void handleChestInventoryChanged(ChestActor chest) {
        if (chest == null || chest != activeChest || chestPanel == null) {
            return;
        }

        chestPanel.setTitle(chest.getStorageTitle());
        chestPanel.setItems(chest.getInventoryItems());
        updateInventoryPanelLayout();
    }

    private void updateInventoryPanelLayout() {
        if (inventoryPanel == null) {
            return;
        }

        if (chestPanel == null || !chestPanel.isVisible()) {
            inventoryPanel.setOffset(0f, 0f);
            if (chestPanel != null) {
                chestPanel.setOffset(0f, 0f);
            }
            return;
        }

        float inventoryOffsetX = -(chestPanel.getWidth() + CHEST_PANEL_SPACING) * 0.5f;
        float chestOffsetX = (inventoryPanel.getWidth() + CHEST_PANEL_SPACING) * 0.5f;
        inventoryPanel.setOffset(inventoryOffsetX, 0f);
        chestPanel.setOffset(chestOffsetX, 0f);
    }

    private boolean isInventoryInteractionActive() {
        return inventoryPanel != null && inventoryPanel.isVisible();
    }

    private void clearPendingAttackState() {
        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        ControllerComponent controller = player.getComponent(ControllerComponent.class);
        if (controller != null) {
            controller.consumeAttackInputTrigger();
        }

        CombatComponent combat = player.getComponent(CombatComponent.class);
        if (combat != null) {
            combat.consumeAttackRequest();
        }
    }

    private void handleHotbarSelection() {
        if (Gdx.input == null || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            selectToolSlot(player, 0);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            selectToolSlot(player, 1);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            selectToolSlot(player, 2);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            selectToolSlot(player, 3);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5)) {
            selectToolSlot(player, 4);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
            selectAssignedItemSlot();
        }
    }

    private void handleToolAttack() {
        if (Gdx.input == null || selectedHotbarSlot == ASSIGNED_ITEM_SLOT_INDEX
            || !Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
            || isInventoryInteractionActive()) {
            return;
        }

        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        ControllerComponent controller = player.getComponent(ControllerComponent.class);
        if (controller != null) {
            controller.triggerAttackInput();
        }

        CombatComponent combat = player.getComponent(CombatComponent.class);
        if (combat != null) {
            combat.requestAttack();
        }
    }

    private void handleAssignedItemUse() {
        if (Gdx.input == null
            || !Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
            || isInventoryInteractionActive()) {
            return;
        }

        if (assignedItemId == null || assignedItemId.isBlank()) {
            return;
        }

        if (getPossessedPawn() instanceof PlayerCharacter player) {
            useAssignedItem(player);
        }
    }

    private void selectToolSlot(PlayerCharacter player, int slotIndex) {
        if (player == null || slotIndex < 0 || slotIndex >= TOOL_SLOT_COUNT) {
            return;
        }

        selectedHotbarSlot = slotIndex;
        if (toolbeltWidget != null) {
            toolbeltWidget.setSelectedSlot(selectedHotbarSlot);
        }

        PlayerToolComponent toolComponent = player.getPlayerToolComponent();
        if (toolComponent != null) {
            toolComponent.setActiveTool(PlayerToolType.values()[slotIndex]);
        }
    }

    private void selectAssignedItemSlot() {
        selectedHotbarSlot = ASSIGNED_ITEM_SLOT_INDEX;
        if (toolbeltWidget != null) {
            toolbeltWidget.setSelectedSlot(selectedHotbarSlot);
        }
    }

    private void useSelectedItem(int slotIndex, String itemId) {
        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        if (!player.hasAuthority()) {
            requestInventoryAction(slotIndex, itemId, NetworkProtocol.InventoryActionType.USE, false);
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        InventoryStack stack = inventory != null ? inventory.getStackAt(slotIndex) : null;
        if (stack == null || stack.getDefinition() == null) {
            return;
        }

        if (CropPlantingService.tryPlant(player, itemId, getWorld(), getCurrentTiledMap(), false)) {
            return;
        }

        if (inventory != null) {
            inventory.useItemAt(slotIndex);
        }
    }

    private void dropSelectedItem(int slotIndex, String itemId, boolean wholeStack) {
        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        if (!player.hasAuthority()) {
            requestInventoryAction(slotIndex, itemId, NetworkProtocol.InventoryActionType.DROP, wholeStack);
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        if (inventory == null) {
            return;
        }

        InventoryStack stack = inventory.getStackAt(slotIndex);
        if (stack == null) {
            return;
        }

        int dropQuantity = wholeStack ? stack.getQuantity() : 1;
        if (inventory.removeItemAt(slotIndex, dropQuantity) > 0) {
            spawnItemNearPlayer(player, stack.getDefinition(), dropQuantity, 0.35f, 0.45f);
        }
    }

    private void assignSelectedItem(int slotIndex, String itemId) {
        if (itemId == null || itemId.isBlank() || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        InventoryStack selectedStack = inventory != null ? inventory.getStackAt(slotIndex) : null;
        if (selectedStack == null || selectedStack.getDefinition() == null) {
            return;
        }

        assignedItemId = selectedStack.getDefinition().getItemId();
        refreshAssignedItemSlot(player);
        selectAssignedItemSlot();

        if (!player.hasAuthority()) {
            requestAssignedItemUpdate(assignedItemId);
            return;
        }

        PlayerAssignedItemComponent assignedItemComponent = player.getPlayerAssignedItemComponent();
        if (assignedItemComponent != null) {
            assignedItemComponent.setAssignedItemId(assignedItemId);
        }
    }

    private void transferInventoryItemToChest(int slotIndex, String itemId, boolean wholeStack) {
        if (itemId == null || itemId.isBlank()
            || activeChest == null
            || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        InventoryComponent playerInventory = player.getInventoryComponent();
        InventoryComponent chestInventory = activeChest.getInventoryComponent();
        if (playerInventory == null || chestInventory == null) {
            return;
        }

        if (!player.hasAuthority()) {
            requestChestTransfer(activeChest.getActorId(), slotIndex, itemId,
                wholeStack,
                NetworkProtocol.ChestInventoryTransferDirection.PLAYER_TO_CHEST);
            return;
        }

        transferInventoryStack(playerInventory, chestInventory, slotIndex, wholeStack);
    }

    private void transferChestItemToInventory(int slotIndex, String itemId, boolean wholeStack) {
        if (itemId == null || itemId.isBlank()
            || activeChest == null
            || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        InventoryComponent playerInventory = player.getInventoryComponent();
        InventoryComponent chestInventory = activeChest.getInventoryComponent();
        if (playerInventory == null || chestInventory == null) {
            return;
        }

        if (!player.hasAuthority()) {
            requestChestTransfer(activeChest.getActorId(), slotIndex, itemId,
                wholeStack,
                NetworkProtocol.ChestInventoryTransferDirection.CHEST_TO_PLAYER);
            return;
        }

        transferInventoryStack(chestInventory, playerInventory, slotIndex, wholeStack);
    }

    private boolean transferInventoryStack(InventoryComponent source,
                                           InventoryComponent target,
                                           int slotIndex,
                                           boolean wholeStack) {
        if (source == null || target == null || slotIndex < 0) {
            return false;
        }

        InventoryStack stack = source.getStackAt(slotIndex);
        if (stack == null || stack.getDefinition() == null || stack.getQuantity() <= 0) {
            return false;
        }

        int transferQuantity = wholeStack ? stack.getQuantity() : 1;
        if (!target.canAddItem(stack.getDefinition(), transferQuantity)) {
            return false;
        }

        int removed = source.removeItemAt(slotIndex, transferQuantity);
        if (removed <= 0) {
            return false;
        }

        int added = target.addItem(stack.getDefinition(), removed);
        if (added == removed) {
            return true;
        }

        source.addItem(stack.getDefinition(), removed - Math.max(0, added));
        return false;
    }

    private void useAssignedItem(PlayerCharacter player) {
        if (player == null || assignedItemId == null || assignedItemId.isBlank()) {
            return;
        }

        InventoryComponent inventory = player.getInventoryComponent();
        InventoryStack assignedStack = inventory != null ? inventory.getStack(assignedItemId) : null;
        if (assignedStack == null) {
            return;
        }

        useSelectedItem(assignedStack.getSlotIndex(), assignedItemId);
    }

    private void requestInventoryAction(int slotIndex,
                                        String itemId,
                                        NetworkProtocol.InventoryActionType actionType,
                                        boolean wholeStack) {
        if (slotIndex < 0 || itemId == null || itemId.isBlank() || actionType == null) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientInventoryAction request = new NetworkProtocol.ClientInventoryAction();
        request.playerId = getPlayerId();
        request.slotIndex = slotIndex;
        request.itemId = itemId;
        request.action = actionType;
        request.wholeStack = wholeStack;
        gameInstance.getNetDriver().sendToServer(request, true);
    }

    private void requestAssignedItemUpdate(String itemId) {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientAssignedItemUpdate request = new NetworkProtocol.ClientAssignedItemUpdate();
        request.playerId = getPlayerId();
        request.itemId = itemId;
        gameInstance.getNetDriver().sendToServer(request, true);
    }

    private void requestChestTransfer(int chestActorId,
                                      int slotIndex,
                                      String itemId,
                                      boolean wholeStack,
                                      NetworkProtocol.ChestInventoryTransferDirection direction) {
        if (chestActorId <= 0 || slotIndex < 0 || itemId == null || itemId.isBlank() || direction == null) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientChestInventoryTransfer request = new NetworkProtocol.ClientChestInventoryTransfer();
        request.playerId = getPlayerId();
        request.chestActorId = chestActorId;
        request.slotIndex = slotIndex;
        request.itemId = itemId;
        request.wholeStack = wholeStack;
        request.direction = direction;
        gameInstance.getNetDriver().sendToServer(request, true);
    }

    private void handleAssignedItemChanged(String itemId) {
        assignedItemId = itemId != null ? itemId.trim() : "";

        if (getPossessedPawn() instanceof PlayerCharacter player) {
            refreshAssignedItemSlot(player);
        } else {
            if (inventoryPanel != null) {
                inventoryPanel.setHiddenItemId(null);
            }
            if (toolbeltWidget != null) {
                toolbeltWidget.setAssignedItem(null);
            }
        }
    }

    private void refreshAssignedItemSlot(PlayerCharacter player) {
        InventoryStack assignedStack = null;
        if (player != null && assignedItemId != null && !assignedItemId.isBlank()) {
            InventoryComponent inventory = player.getInventoryComponent();
            if (inventory != null) {
                assignedStack = inventory.getStack(assignedItemId);
            }
        }

        if (inventoryPanel != null) {
            inventoryPanel.setHiddenItemId(null);
        }
        if (toolbeltWidget != null) {
            toolbeltWidget.setAssignedItem(assignedStack);
            toolbeltWidget.setSelectedSlot(selectedHotbarSlot);
        }
    }

    private void handleToolChanged(PlayerToolType toolType) {
        PlayerToolType resolvedTool = toolType != null ? toolType : PlayerToolType.SWORD;
        selectedHotbarSlot = resolvedTool.ordinal();
        if (toolbeltWidget != null) {
            toolbeltWidget.setSelectedSlot(selectedHotbarSlot);
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

    /** Uruchamia ręczny zapis aktywnego slota z poziomu menu pauzy. */
    private void saveGameProgress() {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isSinglePlayer()) {
            return;
        }

        boolean saved = gameInstance.getSinglePlayerSaveService().saveCurrentGame(gameInstance.getActiveWorldContext());
        if (saved) {
            Gdx.app.log("MainPlayerController", "Zapisano postep gry do slotu #"
                + (gameInstance.getSinglePlayerSaveService().getActiveSlotIndex() + 1));
            showSaveStatus("Zapisano gre", new Color(0.75f, 1f, 0.75f, 1f));
        } else {
            Gdx.app.error("MainPlayerController", "Nie udalo sie zapisac postepu gry");
            showSaveStatus("Blad zapisu", new Color(1f, 0.7f, 0.7f, 1f));
        }
    }

    private com.badlogic.gdx.maps.tiled.TiledMap getCurrentTiledMap() {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || gameInstance.getLevelScreen() == null
            || gameInstance.getLevelScreen().getActiveContext() == null
            || gameInstance.getLevelScreen().getActiveContext().getTiledParser() == null) {
            return null;
        }

        return gameInstance.getLevelScreen().getActiveContext().getTiledParser().getCurrentMap();
    }
}

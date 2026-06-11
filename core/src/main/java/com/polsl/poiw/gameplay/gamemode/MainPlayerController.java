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
import com.polsl.poiw.engine.component.TradeBasketComponent;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.EVisibility;
import com.polsl.poiw.engine.ui.DeathMenuWidget;
import com.polsl.poiw.engine.ui.InventoryPanelWidget;
import com.polsl.poiw.engine.ui.PauseMenuWidget;
import com.polsl.poiw.engine.ui.ProgressBarWidget;
import com.polsl.poiw.engine.ui.SettingsPanelWidget;
import com.polsl.poiw.engine.ui.StatsPanelWidget;
import com.polsl.poiw.engine.ui.TextBlock;
import com.polsl.poiw.engine.ui.TradePanelWidget;
import com.polsl.poiw.engine.ui.ToolbeltWidget;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.actor.ChestActor;
import com.polsl.poiw.gameplay.actor.ItemPickupActor;
import com.polsl.poiw.gameplay.actor.NpcTraderActor;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.crop.CropPlantingService;
import com.polsl.poiw.gameplay.item.GameplayItems;
import com.polsl.poiw.gameplay.trade.TradeOfferDefinition;
import com.polsl.poiw.gameplay.trade.TradeLogic;
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
    private static final float CHEST_PANEL_SPACING = 8f;
    private static final int TOOL_SLOT_COUNT = PlayerToolType.values().length;
    private static final int ASSIGNED_ITEM_SLOT_INDEX = TOOL_SLOT_COUNT;

    private TextBlock hpText;
    private ProgressBarWidget progressBar;
    private InventoryPanelWidget inventoryPanel;
    private InventoryPanelWidget chestPanel;
    private TradePanelWidget tradePanel;
    private ToolbeltWidget toolbeltWidget;
    private PauseMenuWidget pauseMenu;
    private DeathMenuWidget deathMenu;
    private TextBlock saveStatusText;
    private SettingsPanelWidget settingsPanel;
    private StatsPanelWidget statsPanel;
    private PlayerOverlayController overlayController;
    private BindingHandle healthBinding;
    private BindingHandle maxHealthBinding;
    private BindingHandle inventoryBinding;
    private BindingHandle chestInventoryBinding;
    private BindingHandle tradeBasketBinding;
    private BindingHandle traderInventoryBinding;
    private BindingHandle assignedItemBinding;
    private BindingHandle toolBinding;
    private final Map<String, Integer> trackedInventoryQuantities = new HashMap<>();
    private PlayerToolType lastObservedTool;
    private String assignedItemId = "";
    private int selectedHotbarSlot = 0;
    private ChestActor activeChest;
    private NpcTraderActor activeTrader;
    private boolean respawnRequestPending;

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

        inventoryPanel = new InventoryPanelWidget(getSkin(), overlaySkin, getItemsAtlas(), "Ekwipunek", 8, 4, true);
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
                if (activeTrader != null) {
                    transferInventoryItemToTradeBasket(slotIndex, itemId, wholeStack);
                } else {
                    transferInventoryItemToChest(slotIndex, itemId, wholeStack);
                }
            }
        });
        addWidgetToViewport(inventoryPanel);

        chestPanel = new InventoryPanelWidget(getSkin(), overlaySkin, getItemsAtlas(), "Skrzynia", 4, 4, false);
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

        tradePanel = new TradePanelWidget(getSkin(), overlaySkin, getItemsAtlas());
        tradePanel.setAnchor(EAnchor.CENTER);
        tradePanel.setAlignment(EAnchor.CENTER);
        tradePanel.setVisibility(EVisibility.HIDDEN);
        tradePanel.setActionListener(new TradePanelWidget.TradePanelActionListener() {
            @Override
            public void onBuyRequested(int traderSlotIndex, String itemId) {
                buyTraderItem(traderSlotIndex, itemId);
            }

            @Override
            public void onSellRequested() {
                sellTradeBasketItems();
            }

            @Override
            public void onSellSlotTransferRequested(int slotIndex, String itemId, boolean wholeStack) {
                transferTradeBasketItemToInventory(slotIndex, itemId, wholeStack);
            }
        });
        addWidgetToViewport(tradePanel);

        toolbeltWidget = new ToolbeltWidget(getSkin(), getItemsAtlas());
        toolbeltWidget.setAnchor(EAnchor.BOTTOM_RIGHT);
        toolbeltWidget.setAlignment(EAnchor.BOTTOM_RIGHT);
        toolbeltWidget.setOffset(-6f, 6f);
        toolbeltWidget.setVisibility(EVisibility.HIDDEN);
        addWidgetToViewport(toolbeltWidget);

        pauseMenu = new PauseMenuWidget(overlaySkin);
        pauseMenu.setAnchor(EAnchor.CENTER);
        pauseMenu.setAlignment(EAnchor.CENTER);
        pauseMenu.setSaveVisible(getGameInstance() != null && getGameInstance().isSinglePlayer());
        addWidgetToViewport(pauseMenu);

        deathMenu = new DeathMenuWidget(overlaySkin);
        deathMenu.setAnchor(EAnchor.CENTER);
        deathMenu.setAlignment(EAnchor.CENTER);
        addWidgetToViewport(deathMenu);

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
        addWidgetToViewport(settingsPanel);

        statsPanel = new StatsPanelWidget(overlaySkin);
        statsPanel.setAnchor(EAnchor.CENTER);
        statsPanel.setAlignment(EAnchor.CENTER);
        addWidgetToViewport(statsPanel);

        overlayController = new PlayerOverlayController(
            this,
            inventoryPanel,
            pauseMenu,
            settingsPanel,
            statsPanel,
            deathMenu,
            saveStatusText,
            this::clearActiveContainerPanels
        );

        pauseMenu.setActionListener(new PauseMenuWidget.PauseMenuActionListener() {
            @Override
            public void onResumeRequested() {
                overlayController.hidePauseMenu();
            }

            @Override
            public void onSaveRequested() {
                overlayController.saveGameProgress();
            }

            @Override
            public void onOptionsRequested() {
                overlayController.openSettingsFromPauseMenu();
            }

            @Override
            public void onStatsRequested() {
                overlayController.openStatsPanel();
            }

            @Override
            public void onQuitRequested() {
                overlayController.saveAndQuitToMainMenu();
            }
        });
        deathMenu.setActionListener(new DeathMenuWidget.DeathMenuActionListener() {
            @Override
            public void onRespawnRequested() {
                handleRespawnRequested();
            }

            @Override
            public void onQuitRequested() {
                overlayController.saveAndQuitToMainMenu();
            }
        });
        settingsPanel.setCloseAction(overlayController::closeSettingsToPauseMenu);
        statsPanel.setActionListener(new StatsPanelWidget.StatsPanelActionListener() {
            @Override
            public void onRefreshRequested() {
                overlayController.refreshStatsPanel();
            }

            @Override
            public void onCloseRequested() {
                overlayController.closeStatsPanel();
            }
        });
        overlayController.updateStatsUiState();
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
            tradeBasketBinding = player.getTradeBasketRevision().bind(revision -> handleTradeBasketChanged(player));

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
            handleTradeBasketChanged(player);
            refreshAssignedItemSlot(player);
            respawnRequestPending = false;
            if (overlayController != null) {
                overlayController.updateStatsUiState();
            }
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
        if (tradeBasketBinding != null) {
            tradeBasketBinding.unbind();
            tradeBasketBinding = null;
        }
        if (traderInventoryBinding != null) {
            traderInventoryBinding.unbind();
            traderInventoryBinding = null;
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
        activeTrader = null;
        respawnRequestPending = false;

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
        if (tradePanel != null) {
            tradePanel.setOffers(List.of());
            tradePanel.setSellItems(List.of());
            tradePanel.setVisibility(EVisibility.HIDDEN);
            tradePanel.setOffset(0f, 0f);
        }
        if (toolbeltWidget != null) {
            toolbeltWidget.setVisibility(EVisibility.HIDDEN);
            toolbeltWidget.setAssignedItem(null);
            toolbeltWidget.setSelectedSlot(selectedHotbarSlot);
        }
        if (overlayController != null) {
            overlayController.reset();
        }
    }

    /** Czyści bindy i widgety przypisane do tego kontrolera. */
    @Override
    public void destroy() {
        onUnpossess();
        super.destroy();
    }

    /** Obsługuje logikę HUD-u, menu oraz wejście lokalnego gracza w trakcie klatki. */
    @Override
    public void tick(float delta) {
        if (overlayController != null) {
            overlayController.updateSaveStatus(delta);
        }

        if (getPossessedPawn() instanceof PlayerCharacter player) {
            if (player.isDead()) {
                handlePlayerDeath(player);
                return;
            }

            respawnRequestPending = false;
            if (overlayController != null && overlayController.isDeathMenuVisible()) {
                overlayController.hideDeathMenu();
            }
        }

        if (GameInstance.isChatInputActive()) {
            clearPendingAttackState();
            super.tick(delta);
            return;
        }

        if (overlayController != null) {
            overlayController.handlePauseToggle();
        }
        if (overlayController != null && overlayController.isOverlayVisible()) {
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

    private void handlePlayerDeath(PlayerCharacter player) {
        if (player == null) {
            return;
        }

        if (player.hasAuthority() && player.needsDeathDropHandling()) {
            dropPlayerItemsOnDeath(player);
        }

        if (overlayController != null) {
            overlayController.showDeathMenu();
        }
        clearPendingAttackState();
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

    private void handleTradeBasketChanged(PlayerCharacter player) {
        if (tradePanel == null || player == null) {
            return;
        }

        tradePanel.setSellItems(player.getTradeBasketItems());
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
                setActiveTrader(null);
                setActiveChest(null);
            }
        }
    }

    private void updateChestInteractionState() {
        if (!(getPossessedPawn() instanceof PlayerCharacter player)) {
            setActiveTrader(null);
            setActiveChest(null);
            return;
        }

        if (inventoryPanel == null || !inventoryPanel.isVisible()) {
            setActiveTrader(null);
            setActiveChest(null);
            return;
        }

        NpcTraderActor nearbyTrader = findNearbyTrader(player);
        setActiveTrader(nearbyTrader);
        if (nearbyTrader != null) {
            setActiveChest(null);
            return;
        }

        setActiveChest(findNearbyChest(player));
    }

    private NpcTraderActor findNearbyTrader(PlayerCharacter player) {
        GameWorld world = getWorld();
        if (world == null || player == null) {
            return null;
        }

        NpcTraderActor nearestTrader = null;
        float bestDistance2 = Float.MAX_VALUE;
        for (NpcTraderActor trader : world.getActorsOfClass(NpcTraderActor.class)) {
            if (trader == null || !trader.isPlayerInInteractionRange(player)) {
                continue;
            }

            float distance2 = trader.getPosition().dst2(player.getPosition());
            if (distance2 < bestDistance2) {
                bestDistance2 = distance2;
                nearestTrader = trader;
            }
        }
        return nearestTrader;
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

    private void setActiveTrader(NpcTraderActor trader) {
        if (activeTrader == trader) {
            if (activeTrader != null && tradePanel != null && tradePanel.isVisible()) {
                refreshTradePanel();
                updateInventoryPanelLayout();
            }
            return;
        }

        if (traderInventoryBinding != null) {
            traderInventoryBinding.unbind();
            traderInventoryBinding = null;
        }

        activeTrader = trader;
        if (tradePanel == null) {
            return;
        }

        if (activeTrader == null) {
            tradePanel.setOffers(List.of());
            tradePanel.setVisibility(EVisibility.HIDDEN);
            updateInventoryPanelLayout();
            return;
        }

        refreshTradePanel();
        tradePanel.setVisibility(EVisibility.VISIBLE);
        updateInventoryPanelLayout();

        InventoryComponent traderInventory = activeTrader.getInventoryComponent();
        if (traderInventory != null) {
            NpcTraderActor boundTrader = activeTrader;
            traderInventoryBinding = traderInventory.getRevisionBinding().bind(revision -> handleTraderInventoryChanged(boundTrader));
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

    private void handleTraderInventoryChanged(NpcTraderActor trader) {
        if (trader == null || trader != activeTrader || tradePanel == null) {
            return;
        }

        refreshTradePanel();
        updateInventoryPanelLayout();
    }

    private void refreshTradePanel() {
        if (tradePanel == null) {
            return;
        }

        if (activeTrader == null) {
            tradePanel.setOffers(List.of());
            return;
        }

        tradePanel.setTraderName(activeTrader.getDisplayName());
        tradePanel.setOffers(buildTradeOfferViews(activeTrader));
        if (getPossessedPawn() instanceof PlayerCharacter player) {
            tradePanel.setSellItems(player.getTradeBasketItems());
        }
    }

    private List<TradePanelWidget.TradeOfferView> buildTradeOfferViews(NpcTraderActor trader) {
        if (trader == null) {
            return List.of();
        }

        List<InventoryStack> traderItems = trader.getInventoryComponent() != null
            ? trader.getInventoryComponent().getItemsSnapshot()
            : List.of();
        Map<String, Integer> quantitiesByItemId = new HashMap<>();
        Map<String, Integer> slotIndicesByItemId = new HashMap<>();
        for (InventoryStack stack : traderItems) {
            if (stack != null && stack.getDefinition() != null) {
                String itemId = stack.getDefinition().getItemId();
                quantitiesByItemId.merge(itemId, stack.getQuantity(), Integer::sum);
                slotIndicesByItemId.putIfAbsent(itemId, stack.getSlotIndex());
            }
        }

        List<TradePanelWidget.TradeOfferView> views = new java.util.ArrayList<>();
        for (TradeOfferDefinition offer : trader.getOffers()) {
            String itemId = offer.itemDefinition().getItemId();
            views.add(new TradePanelWidget.TradeOfferView(
                slotIndicesByItemId.getOrDefault(itemId, -1),
                offer.itemDefinition(),
                quantitiesByItemId.getOrDefault(itemId, 0),
                offer.buyPrice().toDisplayString(),
                offer.sellPrice().toDisplayString()
            ));
        }
        return views;
    }

    private void updateInventoryPanelLayout() {
        if (inventoryPanel == null) {
            return;
        }

        float sidePanelWidth = 0f;
        boolean sidePanelVisible = false;
        if (tradePanel != null && tradePanel.isVisible()) {
            sidePanelWidth = tradePanel.getWidth();
            sidePanelVisible = true;
        } else if (chestPanel != null && chestPanel.isVisible()) {
            sidePanelWidth = chestPanel.getWidth();
            sidePanelVisible = true;
        }

        if (!sidePanelVisible) {
            inventoryPanel.setOffset(0f, 0f);
            if (chestPanel != null) {
                chestPanel.setOffset(0f, 0f);
            }
            if (tradePanel != null) {
                tradePanel.setOffset(0f, 0f);
            }
            return;
        }

        float inventoryOffsetX = -(sidePanelWidth + CHEST_PANEL_SPACING) * 0.5f;
        float chestOffsetX = (inventoryPanel.getWidth() + CHEST_PANEL_SPACING) * 0.5f;
        inventoryPanel.setOffset(inventoryOffsetX, 0f);
        if (chestPanel != null) {
            chestPanel.setOffset(chestPanel.isVisible() ? chestOffsetX : 0f, 0f);
        }
        if (tradePanel != null) {
            tradePanel.setOffset(tradePanel.isVisible() ? chestOffsetX : 0f, 0f);
        }
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

    private void dropPlayerItemsOnDeath(PlayerCharacter player) {
        List<InventoryStack> droppedStacks = player.clearOwnedItemsForDrop();
        for (int index = 0; index < droppedStacks.size(); index++) {
            InventoryStack stack = droppedStacks.get(index);
            if (stack == null || stack.getDefinition() == null || stack.getQuantity() <= 0) {
                continue;
            }

            spawnDeathDropAroundPlayer(player, stack.getDefinition(), stack.getQuantity(), index, droppedStacks.size());
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

        TradeLogic.transferStack(playerInventory, chestInventory, slotIndex, wholeStack);
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

        TradeLogic.transferStack(chestInventory, playerInventory, slotIndex, wholeStack);
    }

    private void transferInventoryItemToTradeBasket(int slotIndex, String itemId, boolean wholeStack) {
        if (itemId == null || itemId.isBlank()
            || activeTrader == null
            || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        InventoryComponent playerInventory = player.getInventoryComponent();
        TradeBasketComponent tradeBasket = player.getTradeBasketComponent();
        if (playerInventory == null || tradeBasket == null || activeTrader.getOffer(itemId) == null) {
            return;
        }

        if (!player.hasAuthority()) {
            requestTradeTransfer(
                activeTrader.getActorId(),
                slotIndex,
                itemId,
                wholeStack,
                NetworkProtocol.TradeTransferDirection.PLAYER_TO_TRADE
            );
            return;
        }

        TradeLogic.transferStack(playerInventory, tradeBasket, slotIndex, wholeStack);
    }

    private void transferTradeBasketItemToInventory(int slotIndex, String itemId, boolean wholeStack) {
        if (itemId == null || itemId.isBlank()
            || activeTrader == null
            || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        InventoryComponent playerInventory = player.getInventoryComponent();
        TradeBasketComponent tradeBasket = player.getTradeBasketComponent();
        if (playerInventory == null || tradeBasket == null) {
            return;
        }

        if (!player.hasAuthority()) {
            requestTradeTransfer(
                activeTrader.getActorId(),
                slotIndex,
                itemId,
                wholeStack,
                NetworkProtocol.TradeTransferDirection.TRADE_TO_PLAYER
            );
            return;
        }

        TradeLogic.transferStack(tradeBasket, playerInventory, slotIndex, wholeStack);
    }

    private void buyTraderItem(int traderSlotIndex, String itemId) {
        if (itemId == null || itemId.isBlank()
            || activeTrader == null
            || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        if (!player.hasAuthority()) {
            requestTradePurchase(activeTrader.getActorId(), traderSlotIndex, itemId);
            return;
        }

        InventoryComponent playerInventory = player.getInventoryComponent();
        InventoryComponent traderInventory = activeTrader.getInventoryComponent();
        TradeOfferDefinition offer = activeTrader.getOffer(itemId);
        if (playerInventory == null || traderInventory == null || offer == null) {
            return;
        }

        TradeLogic.buyTraderItem(playerInventory, traderInventory, offer, traderSlotIndex, itemId);
    }

    private void sellTradeBasketItems() {
        if (activeTrader == null || !(getPossessedPawn() instanceof PlayerCharacter player)) {
            return;
        }

        if (!player.hasAuthority()) {
            requestTradeSell(activeTrader.getActorId());
            return;
        }

        TradeBasketComponent tradeBasket = player.getTradeBasketComponent();
        InventoryComponent playerInventory = player.getInventoryComponent();
        if (tradeBasket == null || playerInventory == null) {
            return;
        }

        TradeLogic.sellTradeBasket(tradeBasket, playerInventory, activeTrader::getOffer);
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

    private void requestTradeTransfer(int traderActorId,
                                      int slotIndex,
                                      String itemId,
                                      boolean wholeStack,
                                      NetworkProtocol.TradeTransferDirection direction) {
        if (traderActorId <= 0 || slotIndex < 0 || itemId == null || itemId.isBlank() || direction == null) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientTradeTransfer request = new NetworkProtocol.ClientTradeTransfer();
        request.playerId = getPlayerId();
        request.traderActorId = traderActorId;
        request.slotIndex = slotIndex;
        request.itemId = itemId;
        request.wholeStack = wholeStack;
        request.direction = direction;
        gameInstance.getNetDriver().sendToServer(request, true);
    }

    private void requestTradePurchase(int traderActorId, int traderSlotIndex, String itemId) {
        if (traderActorId <= 0 || traderSlotIndex < 0 || itemId == null || itemId.isBlank()) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientTradePurchase request = new NetworkProtocol.ClientTradePurchase();
        request.playerId = getPlayerId();
        request.traderActorId = traderActorId;
        request.traderSlotIndex = traderSlotIndex;
        request.itemId = itemId;
        gameInstance.getNetDriver().sendToServer(request, true);
    }

    private void requestTradeSell(int traderActorId) {
        if (traderActorId <= 0) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientTradeSell request = new NetworkProtocol.ClientTradeSell();
        request.playerId = getPlayerId();
        request.traderActorId = traderActorId;
        gameInstance.getNetDriver().sendToServer(request, true);
    }

    private void requestRespawn() {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null || !gameInstance.isClient() || gameInstance.getNetDriver() == null) {
            return;
        }

        NetworkProtocol.ClientRespawnRequest request = new NetworkProtocol.ClientRespawnRequest();
        request.playerId = getPlayerId();
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

    private void spawnDeathDropAroundPlayer(PlayerCharacter player,
                                            ItemDefinition item,
                                            int quantity,
                                            int dropIndex,
                                            int totalDrops) {
        GameWorld world = getWorld();
        if (world == null || player == null || item == null || quantity <= 0) {
            return;
        }

        TransformComponent transform = player.getComponent(TransformComponent.class);
        Vector2 playerPosition = player.getPosition();
        float playerWidth = transform != null ? transform.getSize().x : 1f;
        float playerHeight = transform != null ? transform.getSize().y : 1f;
        float itemSize = 0.5f;
        float angle = totalDrops <= 0 ? 0f : (float) (Math.PI * 2.0 * dropIndex / Math.max(1, totalDrops));
        float radius = 0.55f + 0.08f * (dropIndex % 3);

        Vector2 spawnPosition = new Vector2(
            playerPosition.x + playerWidth * 0.5f - itemSize * 0.5f + (float) Math.cos(angle) * radius,
            playerPosition.y + playerHeight * 0.5f + (float) Math.sin(angle) * radius
        );

        ItemPickupActor pickupActor = new ItemPickupActor();
        pickupActor.configure(item, quantity, getItemsAtlas());
        pickupActor.setPickupGrace(player.getActorId(), 0.7f);
        world.spawnActor(pickupActor, spawnPosition);
    }

    private void handleRespawnRequested() {
        if (!(getPossessedPawn() instanceof PlayerCharacter player) || !player.isDead()) {
            return;
        }

        GameInstance gameInstance = getGameInstance();
        if (gameInstance != null && gameInstance.isClient()) {
            if (respawnRequestPending) {
                return;
            }

            respawnRequestPending = true;
            requestRespawn();
            return;
        }

        Vector2 spawnPosition = resolveRespawnPosition();
        player.respawnAt(spawnPosition);
        respawnRequestPending = false;
        if (overlayController != null) {
            overlayController.hideDeathMenu();
        }
    }

    private Vector2 resolveRespawnPosition() {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance != null && gameInstance.getActiveWorldContext() != null
            && gameInstance.getActiveWorldContext().getTiledParser() != null) {
            return new Vector2(gameInstance.getActiveWorldContext().getTiledParser().getPlayerStartPosition(0));
        }

        if (getGameMode() != null) {
            return new Vector2(getGameMode().getPlayerStartPosition(0));
        }

        return new Vector2(2f, 2f);
    }

    private void clearActiveContainerPanels() {
        setActiveTrader(null);
        setActiveChest(null);
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

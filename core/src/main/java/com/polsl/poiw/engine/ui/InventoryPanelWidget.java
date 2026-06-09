package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.Align;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InventoryPanelWidget extends UserWidget {

    private static final int DEFAULT_GRID_COLUMNS = 8;
    private static final int DEFAULT_VISIBLE_ROWS = 4;
    private static final float SLOT_SIZE = 14f;
    private static final float SLOT_PADDING = 0.5f;
    private static final float ICON_SIZE = 8f;
    private static final float GRID_PADDING = 1f;
    private static final float BUTTON_WIDTH = 34f;
    private static final float BUTTON_HEIGHT = 12f;
    private static final float TOOLTIP_MIN_WIDTH = 56f;
    private static final float TOOLTIP_MAX_WIDTH = 120f;
    private static final float TOOLTIP_OFFSET_X = 8f;
    private static final float TOOLTIP_OFFSET_Y = 10f;

    public interface InventoryActionListener {
        void onUseRequested(int slotIndex, String itemId);
        void onDropRequested(int slotIndex, String itemId, boolean wholeStack);
        void onAssignRequested(int slotIndex, String itemId);
        void onQuickTransferRequested(int slotIndex, String itemId, boolean wholeStack);
    }

    private final Skin skin;
    private final TextureAtlas itemsAtlas;
    private final Window window;
    private final Table itemsTable;
    private final Label titleLabel;
    private final Table tooltipTable;
    private final Label tooltipNameLabel;
    private final Label tooltipDescLabel;
    private final Cell<Label> tooltipDescCell;
    private final TextButton useButton;
    private final TextButton dropButton;
    private final TextButton assignButton;
    private final Vector2 stagePoint = new Vector2();
    private final int gridColumns;
    private final int visibleRows;
    private final int minVisibleSlots;
    private final boolean showActionButtons;

    private List<InventoryStack> items = List.of();
    private int selectedSlotIndex = -1;
    private String hiddenItemId;
    private InventoryActionListener actionListener;

    public InventoryPanelWidget(Skin skin, TextureAtlas itemsAtlas) {
        this(skin, itemsAtlas, "Ekwipunek", DEFAULT_GRID_COLUMNS, DEFAULT_VISIBLE_ROWS, true);
    }

    public InventoryPanelWidget(Skin skin,
                                TextureAtlas itemsAtlas,
                                String title,
                                int gridColumns,
                                int visibleRows,
                                boolean showActionButtons) {
        super();
        this.skin = skin;
        this.itemsAtlas = itemsAtlas;
        this.gridColumns = Math.max(1, gridColumns);
        this.visibleRows = Math.max(1, visibleRows);
        this.minVisibleSlots = this.gridColumns * this.visibleRows;
        this.showActionButtons = showActionButtons;
        this.window = new Window("", skin);
        this.window.setMovable(false);
        this.window.defaults().pad(1f);
        this.window.getTitleTable().clearChildren();

        this.itemsTable = new Table();
        this.itemsTable.top().left();

        Label.LabelStyle titleStyle = UiSkinStyles.resolveLabelStyle(skin, "default");
        this.titleLabel = new Label(title != null && !title.isBlank() ? title : "Ekwipunek", titleStyle);
        this.titleLabel.setColor(Color.WHITE);
        this.titleLabel.setFontScale(0.55f);
        this.titleLabel.setAlignment(Align.left);

        Label.LabelStyle tooltipNameStyle = UiSkinStyles.resolveLabelStyle(skin, "list");
        this.tooltipNameLabel = new Label("", tooltipNameStyle);
        this.tooltipNameLabel.setFontScale(0.72f);

        Label.LabelStyle tooltipDescStyle = UiSkinStyles.resolveLabelStyle(skin, "list");
        this.tooltipDescLabel = new Label("", tooltipDescStyle);
        this.tooltipDescLabel.setWrap(true);
        this.tooltipDescLabel.setFontScale(0.58f);

        this.tooltipTable = new Table();
        this.tooltipTable.setBackground(skin.getDrawable("list"));
        this.tooltipTable.defaults().left();
        this.tooltipTable.add(tooltipNameLabel).left().row();
        this.tooltipDescCell = this.tooltipTable.add(tooltipDescLabel).left();
        this.tooltipTable.setVisible(false);

        this.useButton = new TextButton("Uzyj", UiSkinStyles.copyTextButtonStyle(skin, "default"));
        this.dropButton = new TextButton("Wyrzuc", UiSkinStyles.copyTextButtonStyle(skin, "default"));
        this.assignButton = new TextButton("Przypisz", UiSkinStyles.copyTextButtonStyle(skin, "default"));
        this.useButton.getLabel().setFontScale(0.6f);
        this.dropButton.getLabel().setFontScale(0.6f);
        this.assignButton.getLabel().setFontScale(0.6f);

        Table buttonRow = new Table();
        buttonRow.defaults().width(BUTTON_WIDTH).height(BUTTON_HEIGHT).padTop(1f);
        buttonRow.add(useButton).padRight(3f);
        buttonRow.add(dropButton).padRight(3f);
        buttonRow.add(assignButton);

        Table gridContainer = new Table();
        gridContainer.setBackground(skin.getDrawable("list"));
        gridContainer.add(itemsTable).pad(GRID_PADDING);

        Table content = new Table();
        content.defaults().left();
        content.add(titleLabel).left().padLeft(1f).padTop(1f).padBottom(1f).row();
        content.add(gridContainer).left().padBottom(1f).row();
        if (showActionButtons) {
            content.add(buttonRow).right().padBottom(1f);
        }

        window.add(content).pad(2f);
        window.pack();

        addActor(window);
        addActor(tooltipTable);
        wireButtons();
        rebuildItems();
        setVisibility(EVisibility.HIDDEN);
    }

    public void setActionListener(InventoryActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setItems(List<InventoryStack> items) {
        List<InventoryStack> normalizedItems = items != null ? List.copyOf(items) : List.of();
        boolean itemsChanged = !areItemsEquivalent(this.items, normalizedItems);
        this.items = normalizedItems;

        if (selectedSlotIndex >= 0 && getSelectedStack() == null) {
            selectedSlotIndex = -1;
            itemsChanged = true;
        }

        if (itemsChanged) {
            rebuildItems();
        } else {
            updateActionButtons();
        }
    }

    public void setHiddenItemId(String itemId) {
        String normalizedItemId = itemId != null && !itemId.isBlank() ? itemId : null;
        if (Objects.equals(hiddenItemId, normalizedItemId)) {
            return;
        }
        InventoryStack selectedStack = getSelectedStack();
        if (normalizedItemId != null
            && selectedStack != null
            && selectedStack.getDefinition() != null
            && normalizedItemId.equals(selectedStack.getDefinition().getItemId())) {
            selectedSlotIndex = -1;
        }
        hiddenItemId = normalizedItemId;
        rebuildItems();
    }

    public String getSelectedItemId() {
        InventoryStack selectedStack = getSelectedStack();
        return selectedStack != null && selectedStack.getDefinition() != null
            ? selectedStack.getDefinition().getItemId()
            : null;
    }

    public int getSelectedSlotIndex() {
        return selectedSlotIndex;
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null && !title.isBlank() ? title : "Ekwipunek");
        syncSize();
    }

    private void wireButtons() {
        useButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                InventoryStack selectedStack = getSelectedStack();
                if (actionListener != null && selectedStack != null && selectedStack.getDefinition() != null) {
                    actionListener.onUseRequested(selectedStack.getSlotIndex(), selectedStack.getDefinition().getItemId());
                }
            }
        });

        dropButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                InventoryStack selectedStack = getSelectedStack();
                if (actionListener != null && selectedStack != null && selectedStack.getDefinition() != null) {
                    actionListener.onDropRequested(
                        selectedStack.getSlotIndex(),
                        selectedStack.getDefinition().getItemId(),
                        isShiftPressed()
                    );
                }
            }
        });

        assignButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                InventoryStack selectedStack = getSelectedStack();
                if (actionListener != null && selectedStack != null && selectedStack.getDefinition() != null) {
                    actionListener.onAssignRequested(selectedStack.getSlotIndex(), selectedStack.getDefinition().getItemId());
                }
            }
        });

        updateActionButtons();
    }

    private void rebuildItems() {
        itemsTable.clearChildren();
        List<InventoryStack> visibleItems = getVisibleItems();

        int slotCount = minVisibleSlots;
        for (int index = 0; index < slotCount; index++) {
            InventoryStack stack = index < visibleItems.size() ? visibleItems.get(index) : null;
            itemsTable.add(createSlot(stack))
                .size(SLOT_SIZE, SLOT_SIZE)
                .pad(SLOT_PADDING);

            if ((index + 1) % gridColumns == 0) {
                itemsTable.row();
            }
        }

        itemsTable.pack();
        updateActionButtons();
        syncSize();
    }

    private Button createSlot(InventoryStack stack) {
        if (stack == null) {
            Button emptyButton = new Button(UiSkinStyles.copyButtonStyle(skin, "default"));
            emptyButton.setDisabled(true);
            return emptyButton;
        }

        ItemDefinition definition = stack.getDefinition();
        Button.ButtonStyle style = UiSkinStyles.copyButtonStyle(skin, "default");
        style.checked = style.down;
        style.checkedOver = style.over;

        Button button = new Button(style);
        button.setChecked(stack.getSlotIndex() == selectedSlotIndex);

        Image icon = createIcon(definition);

        Label quantity = new Label(String.valueOf(stack.getQuantity()), UiSkinStyles.resolveLabelStyle(skin, "list"));
        quantity.setFontScale(0.6f);

        Table iconLayer = new Table();
        iconLayer.add(icon).size(ICON_SIZE, ICON_SIZE);

        Table quantityLayer = new Table();
        quantityLayer.bottom().right();
        quantityLayer.add(quantity).pad(0f, 0f, 1f, 1f);

        Stack slotStack = new Stack();
        slotStack.add(iconLayer);
        slotStack.add(quantityLayer);

        button.add(slotStack).grow();

        button.addListener(new ClickListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y,
                                     int pointer, int buttonCode) {
                selectedSlotIndex = stack.getSlotIndex();
                rebuildItems();
                if (buttonCode == Input.Buttons.RIGHT && actionListener != null) {
                    actionListener.onQuickTransferRequested(
                        stack.getSlotIndex(),
                        definition.getItemId(),
                        isShiftPressed()
                    );
                }
                return true;
            }

            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                              com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (pointer == -1) {
                    showTooltip(definition, event.getStageX(), event.getStageY());
                }
                super.enter(event, x, y, pointer, fromActor);
            }

            @Override
            public boolean mouseMoved(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                showTooltip(definition, event.getStageX(), event.getStageY());
                return false;
            }

            @Override
            public void exit(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (pointer == -1) {
                    hideTooltip();
                }
                super.exit(event, x, y, pointer, toActor);
            }
        });

        return button;
    }

    private Image createIcon(ItemDefinition definition) {
        TextureRegion region = findItemRegion(definition);
        if (region != null) {
            Image icon = new Image(region);
            icon.setScaling(Scaling.contain);
            return icon;
        }

        Image icon = new Image(skin.newDrawable("white", definition.getDisplayColor()));
        icon.setScaling(Scaling.stretch);
        return icon;
    }

    private TextureRegion findItemRegion(ItemDefinition definition) {
        if (itemsAtlas == null || definition == null) {
            return null;
        }

        String regionName = definition.getTextureRegionName();
        if (regionName == null || regionName.isBlank()) {
            return null;
        }

        return itemsAtlas.findRegion(regionName);
    }

    private void showTooltip(ItemDefinition definition, float stageX, float stageY) {
        // To jest wlasny hover panel, zeby nie polegac na domyslnym tooltipie Scene2D.
        tooltipNameLabel.setText(definition.getDisplayName());
        tooltipNameLabel.setColor(definition.getQuality().getDisplayColor());
        tooltipDescLabel.setText(definition.getDescription());
        tooltipDescCell.width(calculateTooltipWidth(definition));
        tooltipTable.pack();
        tooltipTable.setVisible(true);
        updateTooltipPosition(stageX, stageY);
        tooltipTable.toFront();
    }

    private void hideTooltip() {
        tooltipTable.setVisible(false);
    }

    private float calculateTooltipWidth(ItemDefinition definition) {
        int maxLength = Math.max(
            definition.getDisplayName() != null ? definition.getDisplayName().length() : 0,
            definition.getDescription() != null ? definition.getDescription().length() : 0
        );
        return Math.min(TOOLTIP_MAX_WIDTH, Math.max(TOOLTIP_MIN_WIDTH, maxLength * 2.6f));
    }

    private void updateTooltipPosition(float stageX, float stageY) {
        if (root.getStage() == null) {
            return;
        }

        float stageWidth = root.getStage().getViewport().getWorldWidth();
        float stageHeight = root.getStage().getViewport().getWorldHeight();

        float tooltipStageX = Math.min(stageX + TOOLTIP_OFFSET_X, stageWidth - tooltipTable.getWidth() - 2f);
        float tooltipStageY = Math.min(stageY - TOOLTIP_OFFSET_Y, stageHeight - tooltipTable.getHeight() - 2f);
        tooltipStageY = Math.max(2f, tooltipStageY);

        stagePoint.set(tooltipStageX, tooltipStageY);
        root.stageToLocalCoordinates(stagePoint);
        tooltipTable.setPosition(stagePoint.x, stagePoint.y);
    }

    private void updateActionButtons() {
        InventoryStack selectedStack = getSelectedStack();
        boolean canDrop = selectedStack != null;
        boolean canUse = selectedStack != null
            && selectedStack.getDefinition().isConsumable()
            && selectedStack.getDefinition().getHealthRestoreAmount() > 0f;

        useButton.setDisabled(!showActionButtons || !canUse);
        dropButton.setDisabled(!showActionButtons || !canDrop);
        assignButton.setDisabled(!showActionButtons || !canDrop);
    }

    private InventoryStack getSelectedStack() {
        if (selectedSlotIndex < 0) {
            return null;
        }

        for (InventoryStack stack : items) {
            if (stack.getSlotIndex() == selectedSlotIndex) {
                return stack;
            }
        }
        return null;
    }

    private List<InventoryStack> getVisibleItems() {
        if (hiddenItemId == null || hiddenItemId.isBlank()) {
            return items;
        }

        List<InventoryStack> visibleItems = new ArrayList<>(items.size());
        for (InventoryStack stack : items) {
            if (stack == null || stack.getDefinition() == null) {
                continue;
            }
            if (hiddenItemId.equals(stack.getDefinition().getItemId())) {
                continue;
            }
            visibleItems.add(stack);
        }
        return visibleItems;
    }

    private boolean areItemsEquivalent(List<InventoryStack> left, List<InventoryStack> right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null || left.size() != right.size()) {
            return false;
        }

        for (int index = 0; index < left.size(); index++) {
            InventoryStack leftStack = left.get(index);
            InventoryStack rightStack = right.get(index);
            if (leftStack == rightStack) {
                continue;
            }
            if (leftStack == null || rightStack == null) {
                return false;
            }
            if (leftStack.getQuantity() != rightStack.getQuantity()) {
                return false;
            }
            if (leftStack.getSlotIndex() != rightStack.getSlotIndex()) {
                return false;
            }
            if (!Objects.equals(leftStack.getDefinition().getItemId(), rightStack.getDefinition().getItemId())) {
                return false;
            }
        }

        return true;
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }

    private boolean isShiftPressed() {
        return Gdx.input != null
            && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));
    }
}

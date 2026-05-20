package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

import java.util.EnumMap;

public class ToolbeltWidget extends UserWidget {
    private static final float SLOT_SIZE = 16f;
    private static final float ICON_SIZE = 9f;
    private static final float SLOT_PADDING = 0.5f;
    private static final Color ACTIVE_SLOT_COLOR = new Color(1f, 0.86f, 0.38f, 1f);
    private static final Color INACTIVE_SLOT_COLOR = new Color(0.72f, 0.77f, 0.86f, 1f);
    private static final Color ACTIVE_ICON_COLOR = new Color(1f, 1f, 1f, 1f);
    private static final Color INACTIVE_ICON_COLOR = new Color(0.78f, 0.82f, 0.88f, 1f);
    private static final int ITEM_SLOT_INDEX = PlayerToolType.values().length;

    private final Skin skin;
    private final TextureAtlas itemsAtlas;
    private final Table content;
    private final EnumMap<PlayerToolType, Button> slotButtons = new EnumMap<>(PlayerToolType.class);
    private final EnumMap<PlayerToolType, Image> slotIcons = new EnumMap<>(PlayerToolType.class);
    private final Button itemSlotButton;
    private final Image itemSlotIcon;
    private final Label itemSlotQuantity;
    private final Label itemSlotPlaceholder;

    public ToolbeltWidget(Skin skin, TextureAtlas itemsAtlas) {
        super();
        this.skin = skin;
        this.itemsAtlas = itemsAtlas;

        content = new Table();
        content.defaults().pad(SLOT_PADDING);

        for (PlayerToolType toolType : PlayerToolType.values()) {
            Button slotButton = createSlotButton();
            Image icon = createToolIcon(toolType);
            slotButton.add(icon).size(ICON_SIZE, ICON_SIZE);
            slotButtons.put(toolType, slotButton);
            slotIcons.put(toolType, icon);
            content.add(slotButton).size(SLOT_SIZE, SLOT_SIZE);
        }

        itemSlotButton = createSlotButton();
        itemSlotIcon = new Image();
        itemSlotIcon.setScaling(Scaling.fit);
        itemSlotQuantity = new Label("", UiSkinStyles.resolveLabelStyle(skin, "list"));
        itemSlotQuantity.setFontScale(0.6f);
        itemSlotPlaceholder = new Label("", UiSkinStyles.resolveLabelStyle(skin, "list"));
        itemSlotPlaceholder.setFontScale(0.65f);
        itemSlotPlaceholder.setColor(INACTIVE_ICON_COLOR);

        Stack itemSlotStack = new Stack();
        Table placeholderLayer = new Table();
        placeholderLayer.add(itemSlotPlaceholder).center();

        Table iconLayer = new Table();
        iconLayer.add(itemSlotIcon).size(ICON_SIZE, ICON_SIZE);

        Table quantityLayer = new Table();
        quantityLayer.bottom().right();
        quantityLayer.add(itemSlotQuantity).pad(0f, 0f, 1f, 1f);

        itemSlotStack.add(placeholderLayer);
        itemSlotStack.add(iconLayer);
        itemSlotStack.add(quantityLayer);
        itemSlotButton.add(itemSlotStack).grow();
        content.add(itemSlotButton).size(SLOT_SIZE, SLOT_SIZE);

        addActor(content);
        syncSize();
        setAssignedItem(null);
        setSelectedSlot(0);
    }

    public void setSelectedSlot(int slotIndex) {
        for (PlayerToolType candidate : PlayerToolType.values()) {
            boolean selected = candidate.ordinal() == slotIndex;
            Button button = slotButtons.get(candidate);
            Image icon = slotIcons.get(candidate);
            if (button != null) {
                button.setChecked(selected);
                button.setColor(selected ? ACTIVE_SLOT_COLOR : INACTIVE_SLOT_COLOR);
            }
            if (icon != null) {
                icon.setColor(selected ? ACTIVE_ICON_COLOR : INACTIVE_ICON_COLOR);
            }
        }

        boolean itemSelected = slotIndex == ITEM_SLOT_INDEX;
        itemSlotButton.setChecked(itemSelected);
        itemSlotButton.setColor(itemSelected ? ACTIVE_SLOT_COLOR : INACTIVE_SLOT_COLOR);
        itemSlotIcon.setColor(itemSelected ? ACTIVE_ICON_COLOR : INACTIVE_ICON_COLOR);
        itemSlotQuantity.setColor(itemSelected ? ACTIVE_ICON_COLOR : INACTIVE_ICON_COLOR);
        itemSlotPlaceholder.setColor(itemSelected ? ACTIVE_ICON_COLOR : INACTIVE_ICON_COLOR);
    }

    public void setAssignedItem(InventoryStack stack) {
        if (stack == null || stack.getDefinition() == null || stack.getQuantity() <= 0) {
            itemSlotIcon.setDrawable(null);
            itemSlotIcon.setVisible(false);
            itemSlotQuantity.setText("");
            itemSlotQuantity.setVisible(false);
            itemSlotPlaceholder.setVisible(false);
            return;
        }

        ItemDefinition definition = stack.getDefinition();
        itemSlotIcon.setDrawable(createItemDrawable(definition));
        itemSlotIcon.setVisible(true);
        itemSlotQuantity.setText(String.valueOf(stack.getQuantity()));
        itemSlotQuantity.setVisible(true);
        itemSlotPlaceholder.setVisible(false);
    }

    private Button createSlotButton() {
        Button.ButtonStyle style = UiSkinStyles.copyButtonStyle(skin, "default");
        style.checked = style.down != null ? style.down : style.checked;
        style.checkedOver = style.over != null ? style.over : style.checkedOver;
        return new Button(style);
    }

    private Image createToolIcon(PlayerToolType toolType) {
        TextureRegion region = itemsAtlas.findRegion(toolType.getIconRegionName());
        if (region == null) {
            throw new IllegalArgumentException("Nie znaleziono ikony narzedzia: " + toolType.getIconRegionName());
        }

        Image image = new Image(region);
        image.setScaling(Scaling.fit);
        return image;
    }

    private Drawable createItemDrawable(ItemDefinition definition) {
        if (definition != null && definition.getTextureRegionName() != null && !definition.getTextureRegionName().isBlank()) {
            TextureRegion region = itemsAtlas.findRegion(definition.getTextureRegionName());
            if (region != null) {
                return new TextureRegionDrawable(region);
            }
        }

        return skin.newDrawable("white", definition != null ? definition.getDisplayColor() : Color.WHITE);
    }

    private void syncSize() {
        content.pack();
        content.setSize(content.getPrefWidth(), content.getPrefHeight());
        setSize(content.getWidth(), content.getHeight());
    }
}
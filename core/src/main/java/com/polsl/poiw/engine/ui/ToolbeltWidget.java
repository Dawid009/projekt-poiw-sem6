package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Scaling;
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

    private final Table content;
    private final EnumMap<PlayerToolType, Button> slotButtons = new EnumMap<>(PlayerToolType.class);
    private final EnumMap<PlayerToolType, Image> slotIcons = new EnumMap<>(PlayerToolType.class);

    public ToolbeltWidget(Skin skin, TextureAtlas itemsAtlas) {
        super();

        content = new Table();
        content.defaults().pad(SLOT_PADDING);

        for (PlayerToolType toolType : PlayerToolType.values()) {
            Button.ButtonStyle style = UiSkinStyles.copyButtonStyle(skin, "default");
            style.checked = style.down != null ? style.down : style.checked;
            style.checkedOver = style.over != null ? style.over : style.checkedOver;

            Button slotButton = new Button(style);
            Image icon = createIcon(itemsAtlas, toolType);
            slotButton.add(icon).size(ICON_SIZE, ICON_SIZE);
            slotButtons.put(toolType, slotButton);
            slotIcons.put(toolType, icon);
            content.add(slotButton).size(SLOT_SIZE, SLOT_SIZE);
        }

        addActor(content);
        syncSize();
        setSelectedTool(PlayerToolType.SWORD);
    }

    public void setSelectedTool(PlayerToolType toolType) {
        PlayerToolType resolvedTool = toolType != null ? toolType : PlayerToolType.SWORD;

        for (PlayerToolType candidate : PlayerToolType.values()) {
            boolean selected = candidate == resolvedTool;
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
    }

    private Image createIcon(TextureAtlas itemsAtlas, PlayerToolType toolType) {
        TextureRegion region = itemsAtlas.findRegion(toolType.getIconRegionName());
        if (region == null) {
            throw new IllegalArgumentException("Nie znaleziono ikony narzedzia: " + toolType.getIconRegionName());
        }

        Image image = new Image(region);
        image.setScaling(Scaling.fit);
        return image;
    }

    private void syncSize() {
        content.pack();
        content.setSize(content.getPrefWidth(), content.getPrefHeight());
        setSize(content.getWidth(), content.getHeight());
    }
}
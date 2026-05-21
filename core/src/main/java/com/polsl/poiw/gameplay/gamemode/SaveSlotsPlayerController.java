package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.save.SaveSlotSummary;
import com.polsl.poiw.engine.save.SinglePlayerSaveService;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.FullscreenBackgroundRenderer;
import com.polsl.poiw.engine.ui.UiSkinStyles;
import com.polsl.poiw.engine.ui.UserWidget;
import com.polsl.poiw.gameplay.level.LevelDefinitions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SaveSlotsPlayerController extends PlayerController {
    private static final float CONTENT_FONT_SCALE = 0.5f;
    private static final float SLOT_CARD_INFO_WIDTH = 88f;
    private static final float SLOT_BUTTON_WIDTH = 44f;
    private static final float SLOT_BUTTON_HEIGHT = 12f;
    private static final float DELETE_BUTTON_WIDTH = 12f;
    private static final float DELETE_BUTTON_HEIGHT = 12f;

    private UserWidget container;
    private FullscreenBackgroundRenderer backgroundRenderer;
    private Table slotsTable;

    @Override
    protected void setupHUD() {
        Skin skin = getSkin();
        backgroundRenderer = new FullscreenBackgroundRenderer("menu_bg.png");

        container = new UserWidget();
        container.setAnchor(EAnchor.CENTER);
        container.setAlignment(EAnchor.CENTER);

        Window window = new Window("Wybierz zapis", skin, "atlas");
        window.setMovable(false);
        UiSkinStyles.centerWindowTitle(window);

        slotsTable = new Table();
        slotsTable.defaults().left();
        rebuildSlots(skin);

        window.add(slotsTable).pad(10f, 8f, 7f, 8f);
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        container.setSize(window.getWidth(), window.getHeight());
        container.getRoot().addActor(window);
        addWidgetToViewport(container);
    }

    @Override
    public void renderBeforeHud() {
        if (backgroundRenderer != null) {
            backgroundRenderer.render(getHUD().getStage().getBatch());
        }
    }

    @Override
    public void destroy() {
        if (backgroundRenderer != null) {
            backgroundRenderer.dispose();
            backgroundRenderer = null;
        }
        super.destroy();
    }

    private void rebuildSlots(Skin skin) {
        if (slotsTable == null) {
            return;
        }

        slotsTable.clearChildren();

        GameInstance gameInstance = getGameInstance();
        SinglePlayerSaveService saveService = gameInstance != null ? gameInstance.getSinglePlayerSaveService() : null;
        List<SaveSlotSummary> summaries = saveService != null ? saveService.getSlotSummaries() : List.of();

        for (int slotIndex = 0; slotIndex < SinglePlayerSaveService.SLOT_COUNT; slotIndex++) {
            SaveSlotSummary summary = slotIndex < summaries.size()
                ? summaries.get(slotIndex)
                : new SaveSlotSummary(slotIndex, false, 0f, 0L);
            slotsTable.add(createSlotRow(skin, summary)).left().fillX().padBottom(5f).row();
        }

        TextButton backButton = createActionButton(skin, "Powrot", SLOT_BUTTON_WIDTH, SLOT_BUTTON_HEIGHT);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                GameInstance gi = getGameInstance();
                if (gi != null) {
                    gi.travel(LevelDefinitions.MAIN_MENU);
                }
            }
        });
        slotsTable.add(backButton).left().padTop(4f);
    }

    private Table createSlotRow(Skin skin, SaveSlotSummary summary) {
        Table row = new Table();
        row.setBackground(skin.getDrawable("list"));
        row.defaults().left().top();
        row.pad(4f, 5f, 4f, 5f);

        Table infoTable = new Table();
        infoTable.defaults().left();

        Label titleLabel = new Label("Slot " + (summary.slotIndex() + 1), UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
        titleLabel.setColor(Color.WHITE);
        infoTable.add(titleLabel).left().row();

        if (!summary.occupied()) {
            Label emptyLabel = new Label("Pusty zapis", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
            emptyLabel.setColor(Color.LIGHT_GRAY);
            infoTable.add(emptyLabel).left().padTop(2f).row();
        } else {
            Label playTimeLabel = new Label("Czas gry: " + formatPlayTime(summary.totalPlayTimeSeconds()),
                UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
            playTimeLabel.setColor(Color.WHITE);
            infoTable.add(playTimeLabel).left().padTop(2f).row();

            Label lastPlayedLabel = new Label("Ostatnio: " + formatLastPlayed(summary.lastPlayedEpochMillis()),
                UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
            lastPlayedLabel.setColor(Color.WHITE);
            infoTable.add(lastPlayedLabel).left().padTop(1f).row();
        }

        TextButton startButton = createActionButton(
            skin,
            summary.occupied() ? "Graj" : "Nowa",
            SLOT_BUTTON_WIDTH,
            SLOT_BUTTON_HEIGHT
        );
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                startSlot(summary.slotIndex());
            }
        });

        row.add(infoTable).width(SLOT_CARD_INFO_WIDTH).expandX().fillX().padRight(8f);
        row.add(startButton).width(SLOT_BUTTON_WIDTH).height(SLOT_BUTTON_HEIGHT).center();

        if (summary.occupied()) {
            TextButton deleteButton = createActionButton(skin, "X", DELETE_BUTTON_WIDTH, DELETE_BUTTON_HEIGHT);
            deleteButton.addListener(new ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    deleteSlot(summary.slotIndex());
                }
            });
            row.add(deleteButton).width(DELETE_BUTTON_WIDTH).height(DELETE_BUTTON_HEIGHT).padLeft(3f).center();
        }

        return row;
    }

    private String formatPlayTime(float seconds) {
        int totalSeconds = Math.max(0, Math.round(seconds));
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, secs);
    }

    private String formatLastPlayed(long epochMillis) {
        if (epochMillis <= 0L) {
            return "-";
        }

        return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(epochMillis));
    }

    private void startSlot(int slotIndex) {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null) {
            return;
        }

        gameInstance.setMode(GameInstance.Mode.SINGLE_PLAYER);
        gameInstance.getSinglePlayerSaveService().selectSlot(slotIndex);
        Gdx.app.debug("SaveSlotsPlayerController", "Uruchamianie slotu zapisu #" + (slotIndex + 1));
        gameInstance.travel(LevelDefinitions.GAME);
    }

    private void deleteSlot(int slotIndex) {
        GameInstance gameInstance = getGameInstance();
        if (gameInstance == null) {
            return;
        }

        gameInstance.getSinglePlayerSaveService().deleteSlot(slotIndex);
        rebuildSlots(getSkin());
    }

    private TextButton createActionButton(Skin skin, String text, float width, float height) {
        TextButton button = new TextButton(
            text,
            UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE)
        );
        button.getLabelCell().growX();
        button.setSize(width, height);
        return button;
    }
}
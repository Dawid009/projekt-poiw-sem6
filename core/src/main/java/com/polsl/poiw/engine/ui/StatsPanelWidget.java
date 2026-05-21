package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.polsl.poiw.engine.auth.AuthService;

public class StatsPanelWidget extends UserWidget {

    private static final float CONTENT_FONT_SCALE = 0.5f;
    private static final float BUTTON_WIDTH = 38f;
    private static final float BUTTON_HEIGHT = 12f;
    private static final float CONTENT_PADDING = 4f;
    private static final float ROW_SPACING = 1.5f;
    private static final float SECTION_SPACING = 3f;
    private static final float TITLE_CLEARANCE = 2f;

    public interface StatsPanelActionListener {
        void onRefreshRequested();
        void onCloseRequested();
    }

    private StatsPanelActionListener actionListener;
    private final Window window;
    private final Label statusLabel;
    private final Label statsLabel;
    private final TextButton refreshButton;

    public StatsPanelWidget(Skin skin) {
        super();
        this.window = new Window("Statystyki", skin, "atlas");
        this.window.setMovable(false);
        UiSkinStyles.centerWindowTitle(window);

        this.statusLabel = new Label(" ", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
        this.statsLabel = new Label("Brak danych.", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
        this.statsLabel.setAlignment(Align.left);

        this.refreshButton = new TextButton("Odswiez", UiSkinStyles.copyCompactTextButtonStyle(
            skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE));
        TextButton closeButton = new TextButton("Zamknij", UiSkinStyles.copyCompactTextButtonStyle(
            skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE));

        Table content = new Table();
        content.defaults().left().padBottom(ROW_SPACING);
        content.add(statusLabel).left().padTop(TITLE_CLEARANCE).padBottom(SECTION_SPACING).row();
        content.add(statsLabel).left().padBottom(SECTION_SPACING).row();

        Table buttons = new Table();
        buttons.defaults().width(BUTTON_WIDTH).height(BUTTON_HEIGHT);
        buttons.add(refreshButton).padRight(1.5f);
        buttons.add(closeButton);

        content.add(buttons).left().padTop(1.5f).row();

        window.add(content).pad(7f, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING);
        window.pack();

        addActor(window);
        syncSize();
        setVisibility(EVisibility.HIDDEN);

        refreshButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onRefreshRequested();
                }
            }
        });

        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onCloseRequested();
                }
            }
        });
    }

    public void setActionListener(StatsPanelActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void showLoading() {
        refreshButton.setDisabled(true);
        setStatus("Pobieranie statystyk...", Color.YELLOW);
        syncSize();
    }

    public void showError(String message) {
        refreshButton.setDisabled(false);
        setStatus(message == null || message.isBlank() ? "Nie udalo sie pobrac statystyk." : message, Color.RED);
        syncSize();
    }

    public void setStats(AuthService.PlayerStatsSnapshot stats) {
        refreshButton.setDisabled(false);
        setStatus("Statystyki gracza: " + stats.username(), Color.WHITE);
        statsLabel.setText(buildStatsText(stats));
        syncSize();
    }

    private String buildStatsText(AuthService.PlayerStatsSnapshot stats) {
        return "Punkty: " + stats.points() + "\n"
            + "Wejscia do gry: " + stats.entryCount() + "\n"
            + "Zabici przeciwnicy: " + stats.enemyKills() + "\n"
            + "Zabite zwierzeta: " + stats.animalKills() + "\n"
            + "Sciete drzewa: " + stats.treesCut() + "\n"
            + "Zebrane surowce: " + stats.collectedResources() + "\n"
            + "Zebrane plony: " + stats.collectedCrops();
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text == null || text.isBlank() ? " " : text);
        statusLabel.setColor(color == null ? Color.WHITE : color);
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }
}
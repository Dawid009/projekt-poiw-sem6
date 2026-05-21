package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Małe menu pauzy używane w trakcie gry.
 * Pokazuje tylko akcje, które mają sens w danym trybie.
 */
public class PauseMenuWidget extends UserWidget {

    private static final float CONTENT_FONT_SCALE = 0.5f;
    private static final float BUTTON_WIDTH = 42f;
    private static final float BUTTON_HEIGHT = 18f;
    private static final float CONTENT_PADDING = 2.5f;

    /** Zdarzenia wysyłane do kontrolera po kliknięciu przycisków menu. */
    public interface PauseMenuActionListener {
        void onResumeRequested();
        void onSaveRequested();
        void onOptionsRequested();
        void onStatsRequested();
        void onQuitRequested();
    }

    private PauseMenuActionListener actionListener;
    private final Window window;
    private final Cell<TextButton> saveButtonCell;
    private final Cell<TextButton> statsButtonCell;
    private final Cell<TextButton> quitButtonCell;
    private boolean saveVisible = true;
    private boolean statsVisible;

    public PauseMenuWidget(Skin skin) {
        super();
        this.window = new Window("Menu", skin, "atlas");
        this.window.setMovable(false);
        UiSkinStyles.centerWindowTitle(window);

        var compactButtonStyle = UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE);
        TextButton resumeButton = new TextButton("Wznow", compactButtonStyle);
        TextButton saveButton = new TextButton("Zapisz", compactButtonStyle);
        TextButton optionsButton = new TextButton("Opcje", compactButtonStyle);
        TextButton statsButton = new TextButton("Staty", compactButtonStyle);
        TextButton quitButton = new TextButton("Wyjdz", compactButtonStyle);

        Table content = new Table();
        content.defaults().width(BUTTON_WIDTH).height(BUTTON_HEIGHT);
        content.add(resumeButton).padBottom(1.5f).row();
        saveButtonCell = content.add(saveButton).padBottom(1.5f);
        content.row();
        content.add(optionsButton).row();
        statsButtonCell = content.add(statsButton).padTop(1.5f);
        content.row();
        quitButtonCell = content.add(quitButton).padTop(3f);

        window.add(content).pad(7f, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING);
        window.pack();

        addActor(window);
        syncSize();
        setVisibility(EVisibility.HIDDEN);

        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onResumeRequested();
                }
            }
        });

        saveButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onSaveRequested();
                }
            }
        });

        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onOptionsRequested();
                }
            }
        });

        statsButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onStatsRequested();
                }
            }
        });

        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onQuitRequested();
                }
            }
        });

        setStatsVisible(false);
    }

    public void setActionListener(PauseMenuActionListener actionListener) {
        this.actionListener = actionListener;
    }

    /** Pokazuje lub ukrywa przycisk statystyk bez rozwalania układu okna. */
    public void setStatsVisible(boolean visible) {
        statsVisible = visible;
        TextButton statsButton = statsButtonCell != null ? statsButtonCell.getActor() : null;
        if (statsButton != null) {
            statsButton.setVisible(visible);
            statsButton.setDisabled(!visible);
        }
        if (statsButtonCell != null) {
            statsButtonCell.height(visible ? BUTTON_HEIGHT : 0f);
            statsButtonCell.padTop(visible ? 1.5f : 0f);
        }
        syncButtonSpacing();
    }

    /** Pokazuje lub ukrywa przycisk zapisu, np. poza singleplayerem. */
    public void setSaveVisible(boolean visible) {
        saveVisible = visible;
        TextButton saveButton = saveButtonCell != null ? saveButtonCell.getActor() : null;
        if (saveButton != null) {
            saveButton.setVisible(visible);
            saveButton.setDisabled(!visible);
        }
        if (saveButtonCell != null) {
            saveButtonCell.height(visible ? BUTTON_HEIGHT : 0f);
            saveButtonCell.padBottom(visible ? 1.5f : 0f);
        }
        syncButtonSpacing();
    }

    private void syncButtonSpacing() {
        if (quitButtonCell != null) {
            quitButtonCell.padTop((saveVisible || statsVisible) ? 3f : 1.5f);
        }
        syncSize();
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }
}
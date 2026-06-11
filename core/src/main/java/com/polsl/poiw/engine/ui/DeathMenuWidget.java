package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;

/**
 * Prosty overlay pokazywany po śmierci lokalnego gracza.
 * Pozwala odrodzić postać albo zapisać stan i wrócić do menu.
 */
public class DeathMenuWidget extends UserWidget {
    private static final float TITLE_FONT_SCALE = 0.95f;
    private static final float CONTENT_FONT_SCALE = 0.55f;
    private static final float BUTTON_WIDTH = 76f;
    private static final float BUTTON_HEIGHT = 18f;
    private static final float CONTENT_PADDING = 5f;

    /** Callbacki wywoływane po kliknięciu przycisków menu śmierci. */
    public interface DeathMenuActionListener {
        void onRespawnRequested();
        void onQuitRequested();
    }

    private DeathMenuActionListener actionListener;
    private final Window window;

    /** Tworzy okno śmierci oparte o podany skin. */
    public DeathMenuWidget(Skin skin) {
        window = new Window("", skin, "atlas");
        window.setMovable(false);
        UiSkinStyles.centerWindowTitle(window);

        Label.LabelStyle titleStyle = UiSkinStyles.copyScaledLabelStyle(skin, "font", TITLE_FONT_SCALE);
        titleStyle.fontColor = new Color(1f, 0.82f, 0.82f, 1f);
        Label titleLabel = new Label("ZGINALES", titleStyle);
        titleLabel.setAlignment(Align.center);

        var compactButtonStyle = UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE);
        TextButton respawnButton = new TextButton("Odrodzenie", compactButtonStyle);
        TextButton quitButton = new TextButton("Zapisz i wyjdz", compactButtonStyle);

        Table content = new Table();
        content.defaults().width(BUTTON_WIDTH).height(BUTTON_HEIGHT);
        content.add(titleLabel).width(92f).padBottom(5f).row();
        content.add(respawnButton).padBottom(2f).row();
        content.add(quitButton);

        window.add(content).pad(6f, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING);
        window.pack();

        addActor(window);
        syncSize();
        setVisibility(EVisibility.HIDDEN);

        respawnButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onRespawnRequested();
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
    }

    /** Ustawia obiekt obsługujący akcje menu śmierci. */
    public void setActionListener(DeathMenuActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }
}

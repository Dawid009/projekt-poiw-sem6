package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class PauseMenuWidget extends UserWidget {

    private static final float CONTENT_FONT_SCALE = 0.5f;
    private static final float BUTTON_WIDTH = 42f;
    private static final float BUTTON_HEIGHT = 18f;
    private static final float CONTENT_PADDING = 2.5f;

    public interface PauseMenuActionListener {
        void onResumeRequested();
        void onOptionsRequested();
        void onQuitRequested();
    }

    private PauseMenuActionListener actionListener;
    private final Window window;

    public PauseMenuWidget(Skin skin) {
        super();
        this.window = new Window("Menu", skin, "atlas");
        this.window.setMovable(false);
        UiSkinStyles.centerWindowTitle(window);

        var compactButtonStyle = UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE);
        TextButton resumeButton = new TextButton("Wznow", compactButtonStyle);
        TextButton optionsButton = new TextButton("Opcje", compactButtonStyle);
        TextButton quitButton = new TextButton("Wyjdz", compactButtonStyle);

        Table content = new Table();
        content.defaults().width(BUTTON_WIDTH).height(BUTTON_HEIGHT);
        content.add(resumeButton).padBottom(1.5f).row();
        content.add(optionsButton).row();
        content.add(quitButton).padTop(3f);

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

        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onOptionsRequested();
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

    public void setActionListener(PauseMenuActionListener actionListener) {
        this.actionListener = actionListener;
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }
}
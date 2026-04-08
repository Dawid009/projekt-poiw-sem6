package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget przycisku — reaguje na kliknięcia, hover, press/release.
 * <p>
 * Obsługuje callbacki:
 * <ul>
 *   <li>onClick — kliknięcie (press + release w obrębie buttona)</li>
 *   <li>onPressed — wciśnięcie (mouse down)</li>
 *   <li>onReleased — zwolnienie (mouse up)</li>
 *   <li>onHovered — kursor najeżdża na button</li>
 *   <li>onUnhovered — kursor opuszcza button</li>
 * </ul>
 */
public class ButtonWidget extends UserWidget {

    /** Callbacki */
    public interface ButtonCallback {
        void execute();
    }

    private final TextButton button;

    private final List<ButtonCallback> onClickCallbacks = new ArrayList<>();
    private final List<ButtonCallback> onPressedCallbacks = new ArrayList<>();
    private final List<ButtonCallback> onReleasedCallbacks = new ArrayList<>();
    private final List<ButtonCallback> onHoveredCallbacks = new ArrayList<>();
    private final List<ButtonCallback> onUnhoveredCallbacks = new ArrayList<>();

    public ButtonWidget(String text, Skin skin) {
        this(text, skin, "default");
    }

    public ButtonWidget(String text, Skin skin, String styleName) {
        super();
        this.button = new TextButton(text, skin, styleName);
        addActor(button);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClickCallbacks.forEach(ButtonCallback::execute);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                onPressedCallbacks.forEach(ButtonCallback::execute);
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                onReleasedCallbacks.forEach(ButtonCallback::execute);
                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer,
                              com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (pointer == -1) {
                    onHoveredCallbacks.forEach(ButtonCallback::execute);
                }
                super.enter(event, x, y, pointer, fromActor);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer,
                             com.badlogic.gdx.scenes.scene2d.Actor toActor) {
                if (pointer == -1) {
                    onUnhoveredCallbacks.forEach(ButtonCallback::execute);
                }
                super.exit(event, x, y, pointer, toActor);
            }
        });

        syncSize();
    }

    // ===== Bindowanie callbacków =====

    public void onClick(ButtonCallback callback) { onClickCallbacks.add(callback); }
    public void onPressed(ButtonCallback callback) { onPressedCallbacks.add(callback); }
    public void onReleased(ButtonCallback callback) { onReleasedCallbacks.add(callback); }
    public void onHovered(ButtonCallback callback) { onHoveredCallbacks.add(callback); }
    public void onUnhovered(ButtonCallback callback) { onUnhoveredCallbacks.add(callback); }

    /** Usuwa wszystkie callbacki */
    public void clearCallbacks() {
        onClickCallbacks.clear();
        onPressedCallbacks.clear();
        onReleasedCallbacks.clear();
        onHoveredCallbacks.clear();
        onUnhoveredCallbacks.clear();
    }

    // ===== Tekst i styl =====

    public void setText(String text) {
        button.setText(text);
        syncSize();
    }

    public String getText() {
        return button.getText().toString();
    }

    public void setTextColor(Color color) {
        button.getLabel().setColor(color);
    }

    public void setButtonSize(float width, float height) {
        button.setSize(width, height);
        setSize(width, height);
    }

    public TextButton getButton() { return button; }

    // ===== Internals =====

    private void syncSize() {
        button.pack();
        setSize(button.getPrefWidth(), button.getPrefHeight());
    }
}

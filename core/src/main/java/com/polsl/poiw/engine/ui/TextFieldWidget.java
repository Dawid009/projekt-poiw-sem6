package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/**
 * text field widget
 * wraps Scene2D TextField
 */

public class TextFieldWidget extends UserWidget {

    private final TextField textField;

    public TextFieldWidget(String placeholder, Skin skin) {
        this(placeholder, skin, "default");
    }

    public TextFieldWidget(String placeholder, Skin skin, String styleName) {
        super();
        this.textField = new TextField("", UiSkinStyles.copyTextFieldStyle(skin, styleName));
        this.textField.setMessageText(placeholder);
        addActor(textField);
        syncSize();
    } 

    public String getText() {
        return textField.getText();
    }

    public void setText(String text) {
        textField.setText(text);
    }

    public void setPlaceholder(String placeholder) {
        textField.setMessageText(placeholder);
    }

    public void setStyle(Skin skin, String styleName) {
        textField.setStyle(UiSkinStyles.copyTextFieldStyle(skin, styleName));
        syncSize();
    }


    public void setFieldSize(float width, float height) {
        textField.setSize(width, height);
        setSize(width, height);
    }


    public TextField getTextField() {
        return textField;
    }

    private void syncSize() {
        textField.pack();
        setSize(textField.getPrefWidth(), textField.getPrefHeight());
    }
}

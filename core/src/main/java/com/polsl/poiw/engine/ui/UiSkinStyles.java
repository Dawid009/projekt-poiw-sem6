package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

final class UiSkinStyles {

    private static final String DEFAULT_LABEL_STYLE = "default";

    private UiSkinStyles() {}

    static Label.LabelStyle resolveLabelStyle(Skin skin, String styleNameOrFontName) {
        if (skin.has(styleNameOrFontName, Label.LabelStyle.class)) {
            return copyLabelStyle(skin.get(styleNameOrFontName, Label.LabelStyle.class));
        }

        if (skin.has(styleNameOrFontName, BitmapFont.class)) {
            Label.LabelStyle style = skin.has(DEFAULT_LABEL_STYLE, Label.LabelStyle.class)
                ? copyLabelStyle(skin.get(DEFAULT_LABEL_STYLE, Label.LabelStyle.class))
                : new Label.LabelStyle();

            style.font = skin.getFont(styleNameOrFontName);
            if (style.fontColor == null) {
                style.fontColor = Color.WHITE.cpy();
            }
            return style;
        }

        throw new IllegalArgumentException("Skin does not contain LabelStyle or BitmapFont named: " + styleNameOrFontName);
    }

    static TextButton.TextButtonStyle copyTextButtonStyle(Skin skin, String styleName) {
        return new TextButton.TextButtonStyle(skin.get(styleName, TextButton.TextButtonStyle.class));
    }

    static TextField.TextFieldStyle copyTextFieldStyle(Skin skin, String styleName) {
        return new TextField.TextFieldStyle(skin.get(styleName, TextField.TextFieldStyle.class));
    }

    static ProgressBar.ProgressBarStyle copyProgressBarStyle(Skin skin, String styleName) {
        return new ProgressBar.ProgressBarStyle(skin.get(styleName, ProgressBar.ProgressBarStyle.class));
    }

    private static Label.LabelStyle copyLabelStyle(Label.LabelStyle style) {
        Label.LabelStyle copy = new Label.LabelStyle(style);
        if (style.fontColor != null) {
            copy.fontColor = new Color(style.fontColor);
        }
        return copy;
    }
}
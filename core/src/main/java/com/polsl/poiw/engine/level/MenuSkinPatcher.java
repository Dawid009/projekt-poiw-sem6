package com.polsl.poiw.engine.level;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

final class MenuSkinPatcher {
    private MenuSkinPatcher() {
    }

    static void apply(Skin menuSkin, Skin fallbackSkin) {
        if (menuSkin == null || fallbackSkin == null) {
            return;
        }

        menuSkin.addRegions(fallbackSkin.getAtlas());

        overrideFont(menuSkin, fallbackSkin, "font", "font");
        overrideFont(menuSkin, fallbackSkin, "default", "font");
        overrideFont(menuSkin, fallbackSkin, "default-font", "font");
        overrideFont(menuSkin, fallbackSkin, "list", "list");
        overrideFont(menuSkin, fallbackSkin, "subtitle", "subtitle");
        overrideFont(menuSkin, fallbackSkin, "window", "window");
        overrideFont(menuSkin, fallbackSkin, "title", "window");

        overrideLabelStyle(menuSkin, "default", menuSkin.getFont("font"));
        overrideLabelStyle(menuSkin, "list", menuSkin.getFont("list"));
        overrideLabelStyle(menuSkin, "subtitle", menuSkin.getFont("subtitle"));
        overrideLabelStyle(menuSkin, "window", menuSkin.getFont("window"));
        overrideLabelStyle(menuSkin, "title", menuSkin.getFont("window"));

        overrideTextButtonStyle(menuSkin, "default", menuSkin.getFont("font"));
        overrideTextButtonStyle(menuSkin, "atlas", menuSkin.getFont("font"));
        overrideTextFieldStyle(menuSkin, "default", menuSkin.getFont("font"));
        overrideTextFieldStyle(menuSkin, "atlas", menuSkin.getFont("font"));
        overrideSelectBoxStyle(menuSkin, "default", menuSkin.getFont("font"), menuSkin.getFont("list"));
        overrideSelectBoxStyle(menuSkin, "atlas", menuSkin.getFont("font"), menuSkin.getFont("list"));
        overrideCheckBoxStyle(menuSkin, "default", menuSkin.getFont("font"));
        overrideCheckBoxStyle(menuSkin, "atlas", menuSkin.getFont("font"));
        overrideWindowStyle(menuSkin, "default", menuSkin.getFont("window"));
        overrideWindowStyle(menuSkin, "atlas", menuSkin.getFont("window"));
    }

    private static void overrideFont(Skin targetSkin, Skin sourceSkin, String targetName, String sourceName) {
        if (targetSkin == null || sourceSkin == null || !sourceSkin.has(sourceName, BitmapFont.class)) {
            return;
        }
        targetSkin.add(targetName, sourceSkin.getFont(sourceName), BitmapFont.class);
    }

    private static void overrideLabelStyle(Skin skin, String styleName, BitmapFont font) {
        if (font == null || !skin.has(styleName, Label.LabelStyle.class)) {
            return;
        }
        Label.LabelStyle style = new Label.LabelStyle(skin.get(styleName, Label.LabelStyle.class));
        style.font = font;
        skin.add(styleName, style, Label.LabelStyle.class);
    }

    private static void overrideTextButtonStyle(Skin skin, String styleName, BitmapFont font) {
        if (font == null || !skin.has(styleName, TextButton.TextButtonStyle.class)) {
            return;
        }
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle(skin.get(styleName, TextButton.TextButtonStyle.class));
        style.font = font;
        skin.add(styleName, style, TextButton.TextButtonStyle.class);
    }

    private static void overrideTextFieldStyle(Skin skin, String styleName, BitmapFont font) {
        if (font == null || !skin.has(styleName, TextField.TextFieldStyle.class)) {
            return;
        }
        TextField.TextFieldStyle style = new TextField.TextFieldStyle(skin.get(styleName, TextField.TextFieldStyle.class));
        style.font = font;
        style.messageFont = font;
        skin.add(styleName, style, TextField.TextFieldStyle.class);
    }

    private static void overrideSelectBoxStyle(Skin skin, String styleName, BitmapFont font, BitmapFont listFont) {
        if (font == null || !skin.has(styleName, SelectBox.SelectBoxStyle.class)) {
            return;
        }
        SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle(skin.get(styleName, SelectBox.SelectBoxStyle.class));
        style.font = font;
        if (style.listStyle != null) {
            style.listStyle = new com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle(style.listStyle);
            style.listStyle.font = listFont != null ? listFont : font;
        }
        skin.add(styleName, style, SelectBox.SelectBoxStyle.class);
    }

    private static void overrideCheckBoxStyle(Skin skin, String styleName, BitmapFont font) {
        if (font == null || !skin.has(styleName, CheckBox.CheckBoxStyle.class)) {
            return;
        }
        CheckBox.CheckBoxStyle style = new CheckBox.CheckBoxStyle(skin.get(styleName, CheckBox.CheckBoxStyle.class));
        style.font = font;
        skin.add(styleName, style, CheckBox.CheckBoxStyle.class);
    }

    private static void overrideWindowStyle(Skin skin, String styleName, BitmapFont font) {
        if (font == null || !skin.has(styleName, Window.WindowStyle.class)) {
            return;
        }
        Window.WindowStyle style = new Window.WindowStyle(skin.get(styleName, Window.WindowStyle.class));
        style.titleFont = font;
        skin.add(styleName, style, Window.WindowStyle.class);
    }
}

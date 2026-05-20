package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;

public final class UiSkinStyles {

    private static final String DEFAULT_LABEL_STYLE = "default";

    private UiSkinStyles() {}

    public static Label.LabelStyle resolveLabelStyle(Skin skin, String styleNameOrFontName) {
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

    public static Label.LabelStyle copyScaledLabelStyle(Skin skin, String fontName, float fontScale) {
        Label.LabelStyle style = skin.has(DEFAULT_LABEL_STYLE, Label.LabelStyle.class)
            ? copyLabelStyle(skin.get(DEFAULT_LABEL_STYLE, Label.LabelStyle.class))
            : new Label.LabelStyle();
        style.font = resolveScaledFont(skin, fontName, null, fontScale);
        if (style.fontColor == null) {
            style.fontColor = Color.WHITE.cpy();
        }
        return style;
    }

    static TextButton.TextButtonStyle copyTextButtonStyle(Skin skin, String styleName) {
        return new TextButton.TextButtonStyle(skin.get(styleName, TextButton.TextButtonStyle.class));
    }

    static TextButton.TextButtonStyle copyCompactTextButtonStyle(Skin skin, String styleName,
                                                                 float minWidth, float minHeight) {
        return copyCompactTextButtonStyle(skin, styleName, null, minWidth, minHeight);
    }

    public static TextButton.TextButtonStyle copyCompactTextButtonStyle(Skin skin, String styleName,
                                                                        String fontName,
                                                                        float minWidth, float minHeight) {
        return copyCompactTextButtonStyle(skin, styleName, fontName, minWidth, minHeight, 1f);
    }

    public static TextButton.TextButtonStyle copyCompactTextButtonStyle(Skin skin, String styleName,
                                                                        String fontName,
                                                                        float minWidth, float minHeight,
                                                                        float fontScale) {
        TextButton.TextButtonStyle style = copyTextButtonStyle(skin, styleName);
        style.font = resolveScaledFont(skin, fontName, style.font, fontScale);

        float resolvedMinHeight = resolveMinHeight(style.font, minHeight, 3f);
        style.up = resizeDrawable(style.up, minWidth, resolvedMinHeight);
        style.down = resizeDrawable(style.down, minWidth, resolvedMinHeight);
        style.over = resizeDrawable(style.over, minWidth, resolvedMinHeight);
        style.focused = resizeDrawable(style.focused, minWidth, resolvedMinHeight);
        style.disabled = resizeDrawable(style.disabled, minWidth, resolvedMinHeight);
        style.checked = resizeDrawable(style.checked, minWidth, resolvedMinHeight);
        style.checkedOver = resizeDrawable(style.checkedOver, minWidth, resolvedMinHeight);
        return style;
    }

    static Button.ButtonStyle copyButtonStyle(Skin skin, String styleName) {
        return new Button.ButtonStyle(skin.get(styleName, Button.ButtonStyle.class));
    }

    static CheckBox.CheckBoxStyle copyCompactCheckBoxStyle(Skin skin, String styleName,
                                                           float checkWidth, float checkHeight) {
        return copyCompactCheckBoxStyle(skin, styleName, null, checkWidth, checkHeight);
    }

    static CheckBox.CheckBoxStyle copyCompactCheckBoxStyle(Skin skin, String styleName,
                                                           String fontName,
                                                           float checkWidth, float checkHeight) {
        return copyCompactCheckBoxStyle(skin, styleName, fontName, checkWidth, checkHeight, 1f);
    }

    public static CheckBox.CheckBoxStyle copyCompactCheckBoxStyle(Skin skin, String styleName,
                                                                  String fontName,
                                                                  float checkWidth, float checkHeight,
                                                                  float fontScale) {
        CheckBox.CheckBoxStyle style = new CheckBox.CheckBoxStyle(skin.get(styleName, CheckBox.CheckBoxStyle.class));
        style.font = resolveScaledFont(skin, fontName, style.font, fontScale);
        style.checkboxOn = resizeDrawable(style.checkboxOn, checkWidth, checkHeight);
        style.checkboxOff = resizeDrawable(style.checkboxOff, checkWidth, checkHeight);
        style.checkboxOver = resizeDrawable(style.checkboxOver, checkWidth, checkHeight);
        style.checkboxOnOver = resizeDrawable(style.checkboxOnOver, checkWidth, checkHeight);
        style.checkboxOnDisabled = resizeDrawable(style.checkboxOnDisabled, checkWidth, checkHeight);
        style.checkboxOffDisabled = resizeDrawable(style.checkboxOffDisabled, checkWidth, checkHeight);
        return style;
    }

    static SelectBox.SelectBoxStyle copyCompactSelectBoxStyle(Skin skin, String styleName,
                                                              float backgroundWidth, float backgroundHeight,
                                                              float scrollKnobWidth, float scrollKnobHeight,
                                                              float listSelectionHeight) {
        return copyCompactSelectBoxStyle(skin, styleName, null, backgroundWidth, backgroundHeight,
            scrollKnobWidth, scrollKnobHeight, listSelectionHeight);
    }

    static SelectBox.SelectBoxStyle copyCompactSelectBoxStyle(Skin skin, String styleName,
                                                              String fontName,
                                                              float backgroundWidth, float backgroundHeight,
                                                              float scrollKnobWidth, float scrollKnobHeight,
                                                              float listSelectionHeight) {
        return copyCompactSelectBoxStyle(skin, styleName, fontName, backgroundWidth, backgroundHeight,
            scrollKnobWidth, scrollKnobHeight, listSelectionHeight, 1f);
    }

    public static SelectBox.SelectBoxStyle copyCompactSelectBoxStyle(Skin skin, String styleName,
                                                                     String fontName,
                                                                     float backgroundWidth, float backgroundHeight,
                                                                     float scrollKnobWidth, float scrollKnobHeight,
                                                                     float listSelectionHeight,
                                                                     float fontScale) {
        SelectBox.SelectBoxStyle baseStyle = skin.get(styleName, SelectBox.SelectBoxStyle.class);
        SelectBox.SelectBoxStyle style = new SelectBox.SelectBoxStyle(baseStyle);

        style.font = resolveScaledFont(skin, fontName, style.font, fontScale);
        if (style.listStyle != null) {
            style.listStyle = new List.ListStyle(style.listStyle);
            style.listStyle.font = resolveScaledFont(skin, fontName, style.listStyle.font, fontScale);
        }

        float resolvedBackgroundHeight = resolveMinHeight(style.font, backgroundHeight, 2f);
        float resolvedSelectionHeight = resolveMinHeight(
            style.listStyle != null ? style.listStyle.font : style.font,
            listSelectionHeight,
            1f
        );

        style.background = addHorizontalPadding(resizeDrawable(style.background, backgroundWidth, resolvedBackgroundHeight), 4f, 4f);
        style.backgroundOpen = addHorizontalPadding(resizeDrawable(style.backgroundOpen, backgroundWidth, resolvedBackgroundHeight), 4f, 4f);
        style.backgroundOver = addHorizontalPadding(resizeDrawable(style.backgroundOver, backgroundWidth, resolvedBackgroundHeight), 4f, 4f);
        style.backgroundDisabled = addHorizontalPadding(resizeDrawable(style.backgroundDisabled, backgroundWidth, resolvedBackgroundHeight), 4f, 4f);

        if (style.scrollStyle != null) {
            style.scrollStyle = new ScrollPane.ScrollPaneStyle(style.scrollStyle);
            style.scrollStyle.background = resizeDrawable(style.scrollStyle.background, backgroundWidth, resolvedSelectionHeight * 3f);
            style.scrollStyle.vScrollKnob = resizeDrawable(style.scrollStyle.vScrollKnob, scrollKnobWidth,
                Math.max(scrollKnobHeight, resolvedSelectionHeight * 0.75f));
            style.scrollStyle.hScrollKnob = resizeDrawable(style.scrollStyle.hScrollKnob,
                Math.max(scrollKnobHeight, resolvedSelectionHeight * 0.75f), scrollKnobWidth);
            style.scrollStyle.vScroll = resizeDrawable(style.scrollStyle.vScroll, scrollKnobWidth, resolvedSelectionHeight);
            style.scrollStyle.hScroll = resizeDrawable(style.scrollStyle.hScroll,
                Math.max(scrollKnobHeight, resolvedSelectionHeight * 0.75f), scrollKnobWidth);
        }

        if (style.listStyle != null) {
            style.listStyle.selection = addHorizontalPadding(
                resizeDrawable(style.listStyle.selection, backgroundWidth, resolvedSelectionHeight), 4f, 4f);
            style.listStyle.background = addHorizontalPadding(
                resizeDrawable(style.listStyle.background, backgroundWidth, resolvedSelectionHeight * 3f), 4f, 4f);
        }

        return style;
    }

    static TextField.TextFieldStyle copyTextFieldStyle(Skin skin, String styleName) {
        return new TextField.TextFieldStyle(skin.get(styleName, TextField.TextFieldStyle.class));
    }

    public static TextField.TextFieldStyle copyCompactTextFieldStyle(Skin skin, String styleName,
                                                                     String fontName,
                                                                     float minWidth, float minHeight) {
        return copyCompactTextFieldStyle(skin, styleName, fontName, minWidth, minHeight, 1f);
    }

    public static TextField.TextFieldStyle copyCompactTextFieldStyle(Skin skin, String styleName,
                                                                     String fontName,
                                                                     float minWidth, float minHeight,
                                                                     float fontScale) {
        TextField.TextFieldStyle style = copyTextFieldStyle(skin, styleName);
        BitmapFont resolvedFont = resolveScaledFont(skin, fontName, style.font, fontScale);
        style.font = resolvedFont;
        style.messageFont = resolveScaledFont(skin, fontName, style.messageFont, fontScale);

        float resolvedMinHeight = resolveMinHeight(style.font, minHeight, 4f);
        style.background = addHorizontalPadding(resizeDrawable(style.background, minWidth, resolvedMinHeight), 4f, 4f);
        style.focusedBackground = addHorizontalPadding(resizeDrawable(style.focusedBackground, minWidth, resolvedMinHeight), 4f, 4f);
        style.disabledBackground = addHorizontalPadding(resizeDrawable(style.disabledBackground, minWidth, resolvedMinHeight), 4f, 4f);
        return style;
    }

    static ProgressBar.ProgressBarStyle copyProgressBarStyle(Skin skin, String styleName) {
        return new ProgressBar.ProgressBarStyle(skin.get(styleName, ProgressBar.ProgressBarStyle.class));
    }

    public static void centerWindowTitle(Window window) {
        window.getTitleLabel().setAlignment(Align.center);
        var titleCell = window.getTitleTable().getCell(window.getTitleLabel());
        if (titleCell != null) {
            titleCell.expandX().fillX();
        }
    }

    private static Drawable resizeDrawable(Drawable drawable, float minWidth, float minHeight) {
        if (drawable == null) {
            return null;
        }

        if (drawable instanceof NinePatchDrawable ninePatchDrawable) {
            NinePatchDrawable copy = new NinePatchDrawable(ninePatchDrawable);
            copy.setMinWidth(minWidth);
            copy.setMinHeight(minHeight);
            return copy;
        }

        if (drawable instanceof TextureRegionDrawable textureRegionDrawable) {
            TextureRegionDrawable copy = new TextureRegionDrawable(textureRegionDrawable);
            copy.setMinWidth(minWidth);
            copy.setMinHeight(minHeight);
            return copy;
        }

        if (drawable instanceof TiledDrawable tiledDrawable) {
            TiledDrawable copy = new TiledDrawable(tiledDrawable);
            copy.setMinWidth(minWidth);
            copy.setMinHeight(minHeight);
            return copy;
        }

        if (drawable instanceof SpriteDrawable spriteDrawable) {
            SpriteDrawable copy = new SpriteDrawable(spriteDrawable);
            copy.setMinWidth(minWidth);
            copy.setMinHeight(minHeight);
            return copy;
        }

        return drawable;
    }

    private static Drawable addHorizontalPadding(Drawable drawable, float leftWidth, float rightWidth) {
        if (drawable instanceof BaseDrawable baseDrawable) {
            baseDrawable.setLeftWidth(Math.max(baseDrawable.getLeftWidth(), leftWidth));
            baseDrawable.setRightWidth(Math.max(baseDrawable.getRightWidth(), rightWidth));
        }
        return drawable;
    }

    private static BitmapFont resolveFont(Skin skin, String fontName, BitmapFont fallbackFont) {
        if (fontName != null && skin.has(fontName, BitmapFont.class)) {
            return skin.getFont(fontName);
        }
        return fallbackFont;
    }

    private static BitmapFont resolveScaledFont(Skin skin, String fontName, BitmapFont fallbackFont, float fontScale) {
        BitmapFont baseFont = resolveFont(skin, fontName, fallbackFont);
        if (baseFont == null || Math.abs(fontScale - 1f) < 0.001f || fontName == null) {
            return baseFont;
        }

        String fontFile = resolveFontFile(fontName);
        String regionName = resolveFontRegion(fontName);
        if (fontFile == null || regionName == null || !skin.has(regionName, com.badlogic.gdx.graphics.g2d.TextureRegion.class)) {
            return baseFont;
        }

        BitmapFont scaledFont = new BitmapFont(
            Gdx.files.internal(fontFile),
            skin.getRegion(regionName),
            false
        );
        scaledFont.getData().setScale(fontScale);
        scaledFont.setUseIntegerPositions(true);
        return scaledFont;
    }

    private static String resolveFontFile(String fontName) {
        return switch (fontName) {
            case "default", "default-font", "font" -> "ui/font.fnt";
            case "list" -> "ui/font-list.fnt";
            case "window" -> "ui/font-window.fnt";
            case "subtitle" -> "ui/font-subtitle.fnt";
            default -> null;
        };
    }

    private static String resolveFontRegion(String fontName) {
        return switch (fontName) {
            case "default", "default-font", "font" -> "font";
            case "list" -> "font-list";
            case "window" -> "font-window";
            case "subtitle" -> "font-subtitle";
            default -> null;
        };
    }

    private static float resolveMinHeight(BitmapFont font, float requestedHeight, float extraPadding) {
        if (font == null) {
            return requestedHeight;
        }
        return Math.max(requestedHeight, font.getLineHeight() + extraPadding);
    }

    private static Label.LabelStyle copyLabelStyle(Label.LabelStyle style) {
        Label.LabelStyle copy = new Label.LabelStyle(style);
        if (style.fontColor != null) {
            copy.fontColor = new Color(style.fontColor);
        }
        return copy;
    }
}
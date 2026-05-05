package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Widget tekstowy — wyświetla tekst z konfigurowalnymi parametrami.
 * <p>
 * Parametry:
 * <ul>
 *   <li>text — treść do wyświetlenia</li>
 *   <li>styleNameOrFontName — nazwa LabelStyle lub fontu z Skin (np. "default", "subtitle", "font")</li>
 *   <li>fontSize — skalowanie fontu (1.0 = domyślny)</li>
 *   <li>color — kolor tekstu</li>
 *   <li>variable — czy tekst zmienia się dynamicznie (hint dla optymalizacji)</li>
 * </ul>
 */
public class TextBlock extends UserWidget {

    private final Label label;
    private boolean variable = false;

    public TextBlock(String text, Skin skin) {
        this(text, skin, "default");
    }

    public TextBlock(String text, Skin skin, String styleNameOrFontName) {
        super();
        this.label = new Label(text, UiSkinStyles.resolveLabelStyle(skin, styleNameOrFontName));
        addActor(label);
        syncSize();
    }

    // ===== Tekst =====

    /** Ustawia nowy tekst */
    public void setText(String text) {
        label.setText(text);
        syncSize();
    }

    /** Zwraca aktualny tekst */
    public String getText() {
        return label.getText().toString();
    }

    // ===== Styl =====

    /** Ustawia kolor tekstu */
    public void setColor(Color color) {
        label.setColor(color);
    }

    /** Zwraca kolor tekstu */
    public Color getColor() {
        return label.getColor();
    }

    /** Ustawia skalę fontu (1.0 = domyślna, 2.0 = dwukrotnie większy) */
    public void setFontScale(float scale) {
        label.setFontScale(scale);
        syncSize();
    }

    /** Ustawia skalę fontu z osobnymi wartościami X/Y */
    public void setFontScale(float scaleX, float scaleY) {
        label.setFontScale(scaleX, scaleY);
        syncSize();
    }

    /** Zmienia styl lub font na inny ze Skina */
    public void setStyle(Skin skin, String styleNameOrFontName) {
        label.setStyle(UiSkinStyles.resolveLabelStyle(skin, styleNameOrFontName));
        syncSize();
    }

    /** Zmienia font na inny ze Skina */
    public void setFont(Skin skin, String fontName) {
        setStyle(skin, fontName);
    }

    /** Ustawia wrap (zawijanie tekstu) */
    public void setWrap(boolean wrap) {
        label.setWrap(wrap);
    }

    // ===== Variable =====

    /**
     * Oznacza tekst jako dynamiczny (variable).
     * Dynamiczne teksty mogą być bindowane do danych i aktualizowane co klatkę.
     */
    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    public boolean isVariable() {
        return variable;
    }

    // ===== Wyrównanie tekstu =====

    /** Ustawia wyrównanie tekstu wewnątrz labela (Align.center, Align.left, etc.) */
    public void setTextAlignment(int align) {
        label.setAlignment(align);
    }

    // ===== Widoczność labela =====

    public Label getLabel() { return label; }

    // ===== Internals =====

    private void syncSize() {
        label.pack();
        setSize(label.getPrefWidth(), label.getPrefHeight());
    }
}

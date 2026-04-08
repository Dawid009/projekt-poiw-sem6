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
 *   <li>fontName — nazwa fontu z Skin (np. "default-font", "font-subtitle")</li>
 *   <li>fontSize — skalowanie fontu (1.0 = domyślny)</li>
 *   <li>color — kolor tekstu</li>
 *   <li>variable — czy tekst zmienia się dynamicznie (hint dla optymalizacji)</li>
 * </ul>
 */
public class TextBlock extends UserWidget {

    private final Label label;
    private final Label.LabelStyle style;
    private boolean variable = false;

    public TextBlock(String text, Skin skin) {
        this(text, skin, "default-font");
    }

    public TextBlock(String text, Skin skin, String fontName) {
        super();
        this.style = new Label.LabelStyle(skin.getFont(fontName), Color.WHITE.cpy());
        this.label = new Label(text, style);
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

    /** Zmienia font na inny ze Skina */
    public void setFont(Skin skin, String fontName) {
        style.font = skin.getFont(fontName);
        label.setStyle(style);
        syncSize();
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

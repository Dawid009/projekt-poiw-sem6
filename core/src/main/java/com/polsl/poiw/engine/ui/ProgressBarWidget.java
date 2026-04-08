package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Widget paska postępu (HP bar, XP bar, loading bar).
 * <p>
 * Parametry:
 * <ul>
 *   <li>min/max — zakres wartości</li>
 *   <li>value — aktualna wartość</li>
 *   <li>step — krok (dokładność)</li>
 *   <li>vertical — czy pasek jest pionowy</li>
 * </ul>
 */
public class ProgressBarWidget extends UserWidget {

    private final ProgressBar bar;
    private boolean variable = false;

    public ProgressBarWidget(float min, float max, float step, boolean vertical, Skin skin) {
        this(min, max, step, vertical, skin, "default-" + (vertical ? "vertical" : "horizontal"));
    }

    public ProgressBarWidget(float min, float max, float step, boolean vertical,
                             Skin skin, String styleName) {
        super();
        this.bar = new ProgressBar(min, max, step, vertical, skin, styleName);
        addActor(bar);
        syncSize();
    }

    // ===== Wartość =====

    public void setValue(float value) {
        bar.setValue(value);
    }

    public float getValue() {
        return bar.getValue();
    }

    /** Zwraca procent wypełnienia (0.0 - 1.0) */
    public float getPercent() {
        return bar.getPercent();
    }

    public void setRange(float min, float max) {
        bar.setRange(min, max);
    }

    // ===== Styl =====

    public void setBarColor(Color color) {
        bar.setColor(color);
    }

    public void setBarSize(float width, float height) {
        bar.setSize(width, height);
        setSize(width, height);
    }

    // ===== Variable =====

    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    public boolean isVariable() { return variable; }

    public ProgressBar getBar() { return bar; }

    // ===== Internals =====

    private void syncSize() {
        bar.pack();
        setSize(bar.getPrefWidth(), bar.getPrefHeight());
    }
}

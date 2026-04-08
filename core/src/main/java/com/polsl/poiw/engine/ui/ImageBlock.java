package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Widget obrazka — wyświetla TextureRegion lub Drawable.
 * <p>
 * Parametry:
 * <ul>
 *   <li>region/drawable — grafika do wyświetlenia</li>
 *   <li>color (tint) — kolor nakładany na obrazek</li>
 *   <li>variable — czy obrazek zmienia się dynamicznie</li>
 * </ul>
 */
public class ImageBlock extends UserWidget {

    private final Image image;
    private boolean variable = false;

    public ImageBlock(TextureRegion region) {
        this(new TextureRegionDrawable(region));
    }

    public ImageBlock(Drawable drawable) {
        super();
        this.image = new Image(drawable);
        addActor(image);
        syncSize();
    }

    // ===== Grafika =====

    /** Zmienia wyświetlany obrazek */
    public void setDrawable(Drawable drawable) {
        image.setDrawable(drawable);
        syncSize();
    }

    /** Zmienia wyświetlany region tekstury */
    public void setRegion(TextureRegion region) {
        image.setDrawable(new TextureRegionDrawable(region));
        syncSize();
    }

    /** Zwraca aktualny Drawable */
    public Drawable getDrawable() {
        return image.getDrawable();
    }

    // ===== Styl =====

    /** Ustawia tint (kolor nakładany na obrazek) */
    public void setTint(Color color) {
        image.setColor(color);
    }

    /** Zwraca aktualny tint */
    public Color getTint() {
        return image.getColor();
    }

    // ===== Variable =====

    /** Oznacza obrazek jako dynamiczny (zmieniany w runtime) */
    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    public boolean isVariable() {
        return variable;
    }

    // ===== Rozmiar =====

    /** Wymusza rozmiar obrazka (nadpisuje naturalny rozmiar) */
    public void setImageSize(float width, float height) {
        image.setSize(width, height);
        setSize(width, height);
    }

    public Image getImage() { return image; }

    // ===== Internals =====

    private void syncSize() {
        image.pack();
        setSize(image.getPrefWidth(), image.getPrefHeight());
    }
}

package com.polsl.poiw.engine.ui;

/**
 * Punkt zakotwiczenia widgetu na ekranie.
 * Określa punkt referencyjny do pozycjonowania — np. TOP_LEFT oznacza,
 * że (0,0) to lewy górny róg ekranu/parenta.
 */
public enum EAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT;

    /** Zwraca znormalizowaną pozycję X (0.0 = lewo, 0.5 = środek, 1.0 = prawo) */
    public float getX() {
        return switch (this) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> 0f;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> 0.5f;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> 1f;
        };
    }

    /** Zwraca znormalizowaną pozycję Y (0.0 = dół, 0.5 = środek, 1.0 = góra) */
    public float getY() {
        return switch (this) {
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> 0f;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> 0.5f;
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> 1f;
        };
    }
}

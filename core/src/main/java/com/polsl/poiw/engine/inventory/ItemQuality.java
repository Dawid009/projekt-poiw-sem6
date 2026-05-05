package com.polsl.poiw.engine.inventory;

import com.badlogic.gdx.graphics.Color;

public enum ItemQuality {
    COMMON(new Color(0.35f, 0.85f, 0.35f, 1f)),
    RARE(new Color(0.72f, 0.35f, 0.95f, 1f)),
    LEGENDARY(new Color(0.95f, 0.82f, 0.2f, 1f));

    private final Color displayColor;

    ItemQuality(Color displayColor) {
        this.displayColor = displayColor;
    }

    public Color getDisplayColor() {
        return displayColor.cpy();
    }
}
package com.polsl.poiw.engine.level;

/**
 * Typ świata — determinuje jakie systemy są aktywne.
 */
public enum WorldType {

    /**
     * Świat gry z pełnym pipeline'em: fizyka, rendering, tiled map, ECS.
     */
    GAME,

    /**
     * Świat czysto UI — bez fizyki, bez mapy Tiled, bez ECS.
     * Używany do menu głównego
     */
    UI_ONLY
}

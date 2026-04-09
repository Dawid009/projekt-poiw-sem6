package com.polsl.poiw.engine.level;

/**
 * Tryb inputu dla poziomu — określa jakie urządzenia są aktywne.
 */
public enum InputMode {
    /** Tylko mysz (np. menu główne, UI-only ekrany) */
    MOUSE_ONLY,

    /** Tylko klawiatura (np. retro gameplay bez UI) */
    KEYBOARD_ONLY,

    /** Klawiatura + mysz (standardowy gameplay z HUD) */
    KEYBOARD_AND_MOUSE
}

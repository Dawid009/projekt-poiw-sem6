package com.polsl.poiw.engine.actor;

/** Opisuje charakter aktora - "czym jest na danym kliencie".
 * Potrzebne w networkingu który będzie implementowany później
 */
public enum NetRole {
    AUTHORITY,          // Serwer — pełna kontrola, "źródło prawdy"
    AUTONOMOUS_PROXY,   // Klient — lokalny gracz (input, prediction)
    SIMULATED_PROXY,    // Klient — inny gracz (interpolacja pozycji)
    NONE                // Singleplayer — bez sieci
}

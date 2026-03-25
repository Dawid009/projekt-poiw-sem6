package com.polsl.poiw.input;

/**
 * Interfejs stanu kontrolera — wzorzec State.
 *
 * Pozwala przełączać zachowanie inputu między trybami:
 * - GameControllerState → ruch gracza, atak
 * - UiControllerState → nawigacja po menu
 * - IdleControllerState → ignoruj input
 */
public interface ControllerState {
    void keyDown(Command command);

    default void keyUp(Command command) {
    }
}

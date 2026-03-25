package com.polsl.poiw.input;

/**
 * Stan idle — ignoruje cały input.
 * Używany np. podczas cutscen lub ładowania.
 */
public class IdleControllerState implements ControllerState {
    @Override
    public void keyDown(Command command) {
        // Nic nie robi
    }
}

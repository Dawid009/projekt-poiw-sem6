package com.polsl.poiw.input;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter klawiatury — mapuje klawisze na Command i deleguje do aktywnego ControllerState.
 * przełączaj aktywny stan przez setActiveState().
 * Domyślnie startuje w GameControllerState.
 */

//TODO: W późniejszym czasie przerobić to na mapowalne klawisze, nie hardcodowane
public class KeyboardController extends InputAdapter {
    private static final Map<Integer, Command> KEY_MAPPING = Map.ofEntries(
        Map.entry(Input.Keys.W, Command.UP),
        Map.entry(Input.Keys.S, Command.DOWN),
        Map.entry(Input.Keys.A, Command.LEFT),
        Map.entry(Input.Keys.D, Command.RIGHT),
        Map.entry(Input.Keys.SPACE, Command.SELECT),
        Map.entry(Input.Keys.ESCAPE, Command.CANCEL)
    );

    private final boolean[] commandState;
    private final Map<Class<? extends ControllerState>, ControllerState> stateCache;
    private ControllerState activeState;

    public KeyboardController(Class<? extends ControllerState> initialState, Engine engine) {
        this.commandState = new boolean[Command.values().length];
        this.stateCache = new HashMap<>();

        this.stateCache.put(IdleControllerState.class, new IdleControllerState());
        if (engine != null) {
            this.stateCache.put(GameControllerState.class, new GameControllerState(engine));
        }
        setActiveState(initialState);
    }

    /**
     * Przełącza aktywny stan kontrolera.
     * Automatycznie zwalnia wszystkie wciśnięte klawisze przy zmianie stanu.
     */
    public void setActiveState(Class<? extends ControllerState> stateClass) {
        ControllerState state = stateCache.get(stateClass);
        if (state == null) {
            throw new GdxRuntimeException("State " + stateClass.getSimpleName() + " not found in cache");
        }

        // Zwalnia wszystkie wciśnięte klawisze przy zmianie stanu
        for (Command command : Command.values()) {
            if (this.activeState != null && this.commandState[command.ordinal()]) {
                this.activeState.keyUp(command);
            }
            this.commandState[command.ordinal()] = false;
        }
        this.activeState = state;
    }

    @Override
    public boolean keyDown(int keycode) {
        Command command = KEY_MAPPING.get(keycode);
        if (command == null) return false;

        this.commandState[command.ordinal()] = true;
        this.activeState.keyDown(command);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        Command command = KEY_MAPPING.get(keycode);
        if (command == null) return false;
        if (!this.commandState[command.ordinal()]) return false;

        this.commandState[command.ordinal()] = false;
        this.activeState.keyUp(command);
        return true;
    }
}

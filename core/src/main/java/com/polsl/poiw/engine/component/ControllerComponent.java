package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.polsl.poiw.input.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Komponent sterowania — przechowuje listę wciśniętych/zwolnionych komend.
 * KeyboardController (InputAdapter) dodaje komendy do list.
 * ControllerSystem odczytuje je i tłumaczy na ruch / akcje.
 */
public class ControllerComponent extends AbstractActorComponent {
    public static final ComponentMapper<ControllerComponent> MAPPER =
        ComponentMapper.getFor(ControllerComponent.class);

    private final List<Command> pressedCommands;
    private final List<Command> releasedCommands;

    public ControllerComponent() {
        this.pressedCommands = new ArrayList<>();
        this.releasedCommands = new ArrayList<>();
    }

    public List<Command> getPressedCommands() { return pressedCommands; }
    public List<Command> getReleasedCommands() { return releasedCommands; }
}

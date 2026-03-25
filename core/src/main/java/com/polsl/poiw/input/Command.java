package com.polsl.poiw.input;

/**
 * Komendy gry — abstrakcja nad klawiszami.
 *
 * KeyboardController mapuje klawisze → Command.
 * ControllerSystem odczytuje Command → wykonuje akcję (ruch, atak, menu).
 */
public enum Command {
    LEFT,
    RIGHT,
    DOWN,
    UP,
    SELECT,
    CANCEL
}

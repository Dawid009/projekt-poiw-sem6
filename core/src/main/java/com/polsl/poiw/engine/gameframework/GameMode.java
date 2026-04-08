package com.polsl.poiw.engine.gameframework;

import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.ui.HUD;
import com.polsl.poiw.engine.world.GameWorld;

/**
 * GameMode — definiuje reguły gry dla danego poziomu/mapy.
 * <p>
 * Decyduje o:
 * <ul>
 *   <li>klasie postaci gracza (defaultPawnClass)</li>
 *   <li>klasie PlayerControllera</li>
 *   <li>klasie HUD do wyświetlenia</li>
 * </ul>
 * <p>
 * GameMode jest tworzony przez GameScreen i zarządza cyklem życia rozgrywki.
 * Subklasy mogą nadpisywać metody aby dostosować zachowanie
 */
public class GameMode {

    private GameWorld world;

    /** Klasa Actora używana jako postać gracza */
    private Class<? extends AbstractActor> defaultPawnClass;

    /** Klasa PlayerControllera */
    private Class<? extends PlayerController> playerControllerClass = PlayerController.class;

    /** Klasa HUD (null = brak HUD) */
    private Class<? extends HUD> hudClass = HUD.class;

    public GameMode() {
    }

    // ===== Lifecycle =====

    /** Wywoływane po stworzeniu GameMode. Konfiguruj klasy w subklasach. */
    public void initGame(GameWorld world) {
        this.world = world;
    }

    /** Wywoływane co klatkę */
    public void tick(float delta) {
    }

    /** Wywoływane przy zamykaniu poziomu */
    public void endGame() {
    }

    // ===== Konfiguracja klas =====

    public void setDefaultPawnClass(Class<? extends AbstractActor> pawnClass) {
        this.defaultPawnClass = pawnClass;
    }

    public Class<? extends AbstractActor> getDefaultPawnClass() {
        return defaultPawnClass;
    }

    public void setPlayerControllerClass(Class<? extends PlayerController> controllerClass) {
        this.playerControllerClass = controllerClass;
    }

    public Class<? extends PlayerController> getPlayerControllerClass() {
        return playerControllerClass;
    }

    public void setHudClass(Class<? extends HUD> hudClass) {
        this.hudClass = hudClass;
    }

    public Class<? extends HUD> getHudClass() {
        return hudClass;
    }

    // ===== Dostęp =====

    public GameWorld getWorld() { return world; }
}

package com.polsl.poiw.engine.gameframework;

import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.ui.HUD;
import com.polsl.poiw.engine.world.GameWorld;

import com.badlogic.gdx.math.Vector2;

/**
 * GameMode — definiuje reguły gry dla danego poziomu/mapy.
 * GameMode jest tworzony przez WorldContext i zarządza cyklem życia rozgrywki.
 * Subklasy mogą nadpisywać metody aby dostosować zachowanie
 */
public class GameMode {

    private GameWorld world;
    private GameState gameState;

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
        this.gameState = new GameState();
    }

    /** Wywoływane co klatkę */
    public void tick(float delta) {
    }

    /** Wywoływane przy zamykaniu poziomu */
    public void endGame() {
    }

    // ===== Networking callbacks =====

    // called on server when a new player connects
    // default implementation does nothing - override in subclasses
    public void onPlayerLogin(int connectionId, int playerId, String playerName) {
    }

    // called on server when a player disconnects
    public void onPlayerLogout(int connectionId) {
    }

    // start pos for player spawns
    // can be overridden to provide different spawn points for different players
    // TODO: to change depending on the specifications
    public Vector2 getPlayerStartPosition(int playerIndex) {
        return new Vector2(2f + playerIndex * 2f, 2f);
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
    public GameState getGameState() { return gameState; }
}

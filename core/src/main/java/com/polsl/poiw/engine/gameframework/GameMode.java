package com.polsl.poiw.engine.gameframework;

import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.ui.HUD;
import com.polsl.poiw.engine.world.GameWorld;

import com.badlogic.gdx.math.Vector2;

/** Bazowe reguły rozgrywki dla poziomu. */
public class GameMode {

    private GameWorld world;
    private GameState gameState;

    private Class<? extends AbstractActor> defaultPawnClass;
    private Class<? extends PlayerController> playerControllerClass = PlayerController.class;
    private Class<? extends HUD> hudClass = HUD.class;

    public GameMode() {
    }

    public void initGame(GameWorld world) {
        this.world = world;
        this.gameState = new GameState();
    }

    public void tick(float delta) {
    }

    public void endGame() {
    }
    public void onPlayerLogin(int connectionId, int playerId, String playerName) {
    }

    public void onPlayerLogout(int connectionId) {
    }

    public Vector2 getPlayerStartPosition(int playerIndex) {
        return new Vector2(2f + playerIndex * 2f, 2f);
    }

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

    public GameWorld getWorld() { return world; }
    public GameState getGameState() { return gameState; }
}

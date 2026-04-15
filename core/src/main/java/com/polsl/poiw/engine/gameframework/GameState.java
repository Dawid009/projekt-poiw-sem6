package com.polsl.poiw.engine.gameframework;

import com.polsl.poiw.engine.net.Replicated;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// global game state - replicated to clients

public class GameState {

    @Replicated
    private float gameTime;

    @Replicated
    private int playerCount;

    @Replicated
    private boolean gameInProgress;

    private final Map<Integer, PlayerState> playerStates = new HashMap<>();

    public GameState() {}

    public float getGameTime() { return gameTime; }
    public void setGameTime(float gameTime) { this.gameTime = gameTime; }

    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }

    public boolean isGameInProgress() { return gameInProgress; }
    public void setGameInProgress(boolean gameInProgress) { this.gameInProgress = gameInProgress; }

    public PlayerState getPlayerState(int playerId) {
        return playerStates.get(playerId);
    }

    public void addPlayerState(PlayerState state) {
        playerStates.put(state.getPlayerId(), state);
    }

    public void removePlayerState(int playerId) {
        playerStates.remove(playerId);
    }

    public Collection<PlayerState> getAllPlayerStates() {
        return Collections.unmodifiableCollection(playerStates.values());
    }
}

package com.polsl.poiw.engine.gameframework;

import com.polsl.poiw.engine.net.Replicated;
import com.polsl.poiw.engine.net.ReplicationCondition;

// one player state - replicated to all clients

public class PlayerState {

    @Replicated
    private int playerId;

    @Replicated
    private String playerName;

    @Replicated
    private int score;

    @Replicated(condition = ReplicationCondition.OWNER_ONLY)
    private float ping;

    public PlayerState() {}

    public PlayerState(int playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public float getPing() { return ping; }
    public void setPing(float ping) { this.ping = ping; }
}

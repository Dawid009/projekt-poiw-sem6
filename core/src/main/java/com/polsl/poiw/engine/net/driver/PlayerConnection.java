package com.polsl.poiw.engine.net.driver;

/**
 * represents a single players connection on the server
 * maps kryonet connectionId to game playerId
 */

public class PlayerConnection {

    private final int connectionId;
    private int playerId;
    private String playerName;
    private int lastProcessedInputSeq;
    private int lastCorrectedInputSeq;
    private float ping;

    public PlayerConnection(int connectionId, int playerId, String playerName) {
        this.connectionId = connectionId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.lastProcessedInputSeq = 0;
        this.lastCorrectedInputSeq = -1;
        this.ping = 0f;
    }

    public int getConnectionId() { return connectionId; }
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getLastProcessedInputSeq() { return lastProcessedInputSeq; }
    public void setLastProcessedInputSeq(int seq) { this.lastProcessedInputSeq = seq; }
    public int getLastCorrectedInputSeq() { return lastCorrectedInputSeq; }
    public void setLastCorrectedInputSeq(int seq) { this.lastCorrectedInputSeq = seq; }
    public float getPing() { return ping; }
    public void setPing(float ping) { this.ping = ping; }
}

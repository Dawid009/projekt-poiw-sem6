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
    private int lastAcceptedInputSeq;
    private int lastCorrectionSentSeq;
    private int lastCorrectionSentTick;
    private int spawnIndex;
    private float lastCorrectionX;
    private float lastCorrectionY;
    private float lastCorrectionVelX;
    private float lastCorrectionVelY;
    private int rejectedInputCount;
    private float ping;

    public PlayerConnection(int connectionId, int playerId, String playerName, int spawnIndex) {
        this.connectionId = connectionId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.lastProcessedInputSeq = -1;
        this.lastAcceptedInputSeq = -1;
        this.lastCorrectionSentSeq = -1;
        this.lastCorrectionSentTick = -1;
        this.spawnIndex = Math.max(0, spawnIndex);
        this.lastCorrectionX = Float.NaN;
        this.lastCorrectionY = Float.NaN;
        this.lastCorrectionVelX = Float.NaN;
        this.lastCorrectionVelY = Float.NaN;
        this.rejectedInputCount = 0;
        this.ping = 0f;
    }

    public int getConnectionId() { return connectionId; }
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getLastProcessedInputSeq() { return lastProcessedInputSeq; }
    public void setLastProcessedInputSeq(int seq) { this.lastProcessedInputSeq = seq; }
    public int getLastAcceptedInputSeq() { return lastAcceptedInputSeq; }
    public void setLastAcceptedInputSeq(int seq) { this.lastAcceptedInputSeq = seq; }
    public int getLastCorrectionSentSeq() { return lastCorrectionSentSeq; }
    public void setLastCorrectionSentSeq(int seq) { this.lastCorrectionSentSeq = seq; }
    public int getLastCorrectionSentTick() { return lastCorrectionSentTick; }
    public void setLastCorrectionSentTick(int tick) { this.lastCorrectionSentTick = tick; }
    public int getSpawnIndex() { return spawnIndex; }
    public void setSpawnIndex(int spawnIndex) { this.spawnIndex = Math.max(0, spawnIndex); }
    public float getLastCorrectionX() { return lastCorrectionX; }
    public void setLastCorrectionX(float x) { this.lastCorrectionX = x; }
    public float getLastCorrectionY() { return lastCorrectionY; }
    public void setLastCorrectionY(float y) { this.lastCorrectionY = y; }
    public float getLastCorrectionVelX() { return lastCorrectionVelX; }
    public void setLastCorrectionVelX(float velX) { this.lastCorrectionVelX = velX; }
    public float getLastCorrectionVelY() { return lastCorrectionVelY; }
    public void setLastCorrectionVelY(float velY) { this.lastCorrectionVelY = velY; }
    public int getRejectedInputCount() { return rejectedInputCount; }
    public int incrementRejectedInputCount() { return ++rejectedInputCount; }
    public float getPing() { return ping; }
    public void setPing(float ping) { this.ping = ping; }
}

package com.polsl.poiw;

public class GameInstance {
    public enum Mode { SINGLE_PLAYER, MULTIPLAYER }

    private Mode mode = Mode.SINGLE_PLAYER;
    private String serverHost = "localhost";
    private String playerName = "Player";
    private int localPlayerId = -1;

    // Settery wywoływane z MenuScreen gdy gracz wybiera tryb gry
    public void setMode(Mode mode) { this.mode = mode; }
    public void setServerHost(String host) { this.serverHost = host; }
    public void setPlayerName(String name) { this.playerName = name; }

    public boolean isMultiplayer() { return mode == Mode.MULTIPLAYER; }
}

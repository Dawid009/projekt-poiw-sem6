package com.polsl.poiw;

import com.badlogic.gdx.Gdx;
import com.polsl.poiw.engine.level.LevelDefinition;
import com.polsl.poiw.shared.protocol.NetworkProtocol;
import com.polsl.poiw.engine.level.LevelRegistry;
import com.polsl.poiw.engine.level.LevelScreen;
import com.polsl.poiw.engine.level.WorldContext;
import com.polsl.poiw.engine.net.driver.NetDriver;

/**
 * GameInstance — globalny stan gry utrzymywany przez cały czas życia aplikacji.
 * <p>
 * Odpowiada za:
 * <ul>
 *   <li>Rejestr dostępnych poziomów ({@link LevelRegistry})</li>
 *   <li>Przechodzenie między światami (travel)</li>
 *   <li>Śledzenie aktywnego poziomu</li>
 *   <li>Konfigurację sesji (tryb gry, host, nazwa gracza)</li>
 * </ul>
 * <p>
 * Travel flow:
 * <ol>
 *   <li>Kod gry wywołuje {@link #travel(String)} z levelId</li>
 *   <li>GameInstance pobiera LevelDefinition z rejestru</li>
 *   <li>LevelScreen otwiera nowy WorldContext (stary jest niszczony)</li>
 *   <li>Callback konfiguruje nowo otwarty świat (spawn gracza, itp.)</li>
 * </ol>
 */
public class GameInstance {

    private static final String TAG = "GameInstance";

    // ===== Konfiguracja sesji =====

    public enum Mode { SINGLE_PLAYER, MULTIPLAYER }

    private Mode mode = Mode.SINGLE_PLAYER;
    private String serverHost = "localhost";
    private int serverTcpPort = NetworkProtocol.DEFAULT_TCP_PORT;
    private String playerName = "Player";
    private int localPlayerId = -1;

    // ===== Networking =====

    private NetDriver netDriver;
    private boolean isServer = false;
    private boolean isConnected = false;
    private float serverTime = 0f;

    // ===== System poziomów =====

    private final LevelRegistry levelRegistry = new LevelRegistry();
    private LevelScreen levelScreen;

    /** ID aktualnie aktywnego poziomu (null = brak) */
    private String currentLevelId;

    /** Callback wywoływany po każdym travel (do konfiguracji nowo otwartego świata) */
    private LevelScreen.LevelReadyCallback travelCallback;

    // ===== Inicjalizacja =====

    /**
     * Ustawia referencję do LevelScreen — wywoływane raz z Main.
     */
    public void setLevelScreen(LevelScreen screen) {
        this.levelScreen = screen;
    }

    /**
     * Ustawia callback wywoływany po każdym travel.
     * Gameplay rejestruje tu np. spawn gracza, konfigurację kontrolerów.
     */
    public void setTravelCallback(LevelScreen.LevelReadyCallback callback) {
        this.travelCallback = callback;
    }

    // ===== Travel =====

    /**
     * Przechodzi do poziomu o podanym ID.
     * Niszczy poprzedni świat i tworzy nowy.
     *
     * @param levelId identyfikator poziomu z LevelRegistry
     */
    public void travel(String levelId) {
        travel(levelId, travelCallback);
    }

    /**
     * Przechodzi do poziomu z dedykowanym callbackiem.
     *
     * @param levelId identyfikator poziomu
     * @param callback callback po inicjalizacji (nadpisuje domyślny)
     */
    public void travel(String levelId, LevelScreen.LevelReadyCallback callback) {
        if (levelScreen == null) {
            Gdx.app.error(TAG, "LevelScreen nie został ustawiony — nie można wykonać travel");
            return;
        }

        LevelDefinition def = levelRegistry.get(levelId);
        Gdx.app.debug(TAG, "Travel: " + (currentLevelId != null ? currentLevelId : "<brak>")
            + " → " + levelId);

        currentLevelId = levelId;
        levelScreen.openLevel(def, callback);
    }

    /**
     * Stub pod przyszły networking — server travel.
     * W trybie singleplayer działa jak zwykły travel.
     */
    public void serverTravel(String levelId) {
        Gdx.app.debug(TAG, "ServerTravel: " + levelId + " (stub → local travel)");
        travel(levelId);
    }

    /**
     * Stub pod przyszły networking — client travel.
     * Klient przechodzi do poziomu wskazanego przez serwer.
     */
    public void clientTravel(String levelId, String host) {
        Gdx.app.debug(TAG, "ClientTravel: " + levelId + " @ " + host + " (stub → local travel)");
        this.serverHost = host;
        travel(levelId);
    }

    // ===== Dostęp =====

    public LevelRegistry getLevelRegistry() { return levelRegistry; }
    public String getCurrentLevelId() { return currentLevelId; }

    /** Aktywny WorldContext (z LevelScreen) */
    public WorldContext getActiveWorldContext() {
        return levelScreen != null ? levelScreen.getActiveContext() : null;
    }

    // ===== Konfiguracja sesji =====

    public void setMode(Mode mode) { this.mode = mode; }
    public void setServerHost(String host) { this.serverHost = host; }
    public void setServerTcpPort(int port) { this.serverTcpPort = port; }
    public void setPlayerName(String name) { this.playerName = name; }
    public void setLocalPlayerId(int id) { this.localPlayerId = id; }

    public Mode getMode() { return mode; }
    public String getServerHost() { return serverHost; }
    public int getServerTcpPort() { return serverTcpPort; }
    public int getServerUdpPort() { return serverTcpPort + (NetworkProtocol.DEFAULT_UDP_PORT - NetworkProtocol.DEFAULT_TCP_PORT); }
    public String getPlayerName() { return playerName; }
    public int getLocalPlayerId() { return localPlayerId; }
    public boolean isMultiplayer() { return mode == Mode.MULTIPLAYER; }

    // ===== Networking accessors =====

    public NetDriver getNetDriver() { return netDriver; }
    public void setNetDriver(NetDriver netDriver) { this.netDriver = netDriver; }
    public boolean isServer() { return isServer; }
    public void setServer(boolean server) { this.isServer = server; }
    public boolean isClient() { return !isServer && mode == Mode.MULTIPLAYER; }
    public boolean isSinglePlayer() { return mode == Mode.SINGLE_PLAYER; }
    public boolean isConnected() { return isConnected; }
    public void setConnected(boolean connected) { this.isConnected = connected; }
    public float getServerTime() { return serverTime; }
    public void setServerTime(float serverTime) { this.serverTime = serverTime; }
}


package com.polsl.poiw;

import com.badlogic.gdx.Gdx;
import com.polsl.poiw.engine.auth.AuthService;
import com.polsl.poiw.engine.level.LevelDefinition;
import com.polsl.poiw.shared.protocol.NetworkProtocol;
import com.polsl.poiw.engine.level.LevelRegistry;
import com.polsl.poiw.engine.level.LevelScreen;
import com.polsl.poiw.engine.level.WorldContext;
import com.polsl.poiw.engine.net.driver.NetDriver;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * GameInstance — globalny stan gry utrzymywany przez cały czas życia aplikacji.
 * <p>
 * Odpowiada za:
 * <ul>
 *   <li>Rejestr dostępnych poziomów ({@link LevelRegistry})</li>
 *   <li>Przechodzenie między światami (travel)</li>
 *   <li>Śledzenie aktywnego poziomu</li>
 *   <li>Konfigurację sesji (tryb gry, host, nazwa gracza)</li>
 *   <li>Zarządzanie stanem sesji sieciowej ({@link SessionState})</li>
 * </ul>
 */
public class GameInstance {

    private static final String TAG = "GameInstance";

    private final AuthService authService = new AuthService();

    // ===== Konfiguracja sesji =====

    public enum Mode { SINGLE_PLAYER, MULTIPLAYER }

    /**
     * stan sesji sieciowej — kontroluje dozwolone przejścia i guardy
     */
    public enum SessionState {
        /** brak aktywnego połączenia */
        DISCONNECTED,
        /** trwa próba połączenia z serwerem (czekanie na ServerAccept) */
        CONNECTING,
        /** połączenie potwierdzone (ServerAccept otrzymany) */
        CONNECTED,
        /** trwa zmiana mapy (server travel / client travel) */
        TRAVELLING
    }

    private Mode mode = Mode.SINGLE_PLAYER;
    private SessionState sessionState = SessionState.DISCONNECTED;
    private String serverHost = "localhost";
    private int serverTcpPort = NetworkProtocol.DEFAULT_TCP_PORT;
    private String playerName = "Player";
    private int localPlayerId = -1;

    // ===== Networking =====

    private NetDriver netDriver;
    private boolean isServer = false;
    private float serverTime = 0f;

    /** timeout dla łączenia z serwerem (ms) */
    private static final long CONNECT_TIMEOUT_MS = 10_000;
    private long connectStartTimeMs = 0;

    /** callback informujący o statusie połączenia (np. do aktualizacji UI menu) */
    private Consumer<String> connectStatusCallback;

    /** callback na błąd / odrzucenie / timeout */
    private Consumer<String> connectErrorCallback;

    /** gameplay messages received after ServerAccept but before WorldContext installs gameplay handlers */
    private final ArrayDeque<Object> pendingGameplayMessages = new ArrayDeque<>();

    /** target map received in ServerAccept; deferred until connect-phase queue has been drained */
    private String pendingConnectLevelId;

    // ===== Chat =====

    /** Chat message history — persists across level transitions */
    private final List<NetworkProtocol.ChatMessage> chatHistory = new ArrayList<>();
    private static final int MAX_CHAT_HISTORY = 50;

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

    // ===== Connect =====

    /**
     * rozpoczyna asynchroniczne łączenie z serwerem.
     * tworzy NetDriver, łączy, wysyła ClientConnect.
     * po ServerAccept → travel("game").
     * po ServerReject / timeout / disconnect → errorCallback.
     *
     * @param statusCallback callback informujący o statusie (np. "Łączenie...")
     * @param errorCallback  callback na błąd (reason string)
     */
    public void connectToServer(Consumer<String> statusCallback, Consumer<String> errorCallback) {
        if (sessionState != SessionState.DISCONNECTED) {
            Gdx.app.error(TAG, "connectToServer: nieprawidłowy stan sesji: " + sessionState);
            if (errorCallback != null) errorCallback.accept("Nieprawidłowy stan sesji: " + sessionState);
            return;
        }

        this.connectStatusCallback = statusCallback;
        this.connectErrorCallback = errorCallback;
        this.sessionState = SessionState.CONNECTING;

        if (statusCallback != null) statusCallback.accept("Łączenie z " + serverHost + "...");

        // tworzy NetDriver kliencki
        if (netDriver != null) {
            netDriver.dispose();
        }
        netDriver = new NetDriver(false);
        pendingGameplayMessages.clear();
        pendingConnectLevelId = null;

        // obsługa wiadomości w fazie CONNECTING (ServerAccept/Reject)
        netDriver.setMessageHandler((connectionId, message) -> handleConnectPhaseMessage(message));
        netDriver.setDisconnectHandler(connectionId -> {
            Gdx.app.log(TAG, "Rozłączono z serwerem w fazie łączenia");
            abortConnect("Rozłączono z serwerem");
        });

        // połącz
        boolean connected = netDriver.connectToServer(serverHost, serverTcpPort, getServerUdpPort());
        if (!connected) {
            abortConnect("Nie można połączyć z serwerem: " + serverHost + ":" + serverTcpPort);
            return;
        }

        // start timeout clock AFTER successful connect (not during blocking connect())
        this.connectStartTimeMs = System.currentTimeMillis();

        // wyślij ClientConnect
        var connect = new NetworkProtocol.ClientConnect();
        connect.playerName = playerName;
        connect.protocolVersion = NetworkProtocol.PROTOCOL_VERSION;
        netDriver.sendToServer(connect, true);

        Gdx.app.debug(TAG, "ClientConnect sent, waiting for ServerAccept...");
    }

    /**
     * obsługuje wiadomości w fazie CONNECTING — tylko ServerAccept / ServerReject
     */
    private void handleConnectPhaseMessage(Object message) {
        if (sessionState == SessionState.CONNECTED) {
            pendingGameplayMessages.add(message);
            return;
        }

        if (sessionState != SessionState.CONNECTING) return;

        Gdx.app.debug(TAG, "Connect phase message received: " + message.getClass().getSimpleName());

        if (message instanceof NetworkProtocol.ServerAccept accept) {
            localPlayerId = accept.assignedPlayerId;
            serverTime = accept.serverTime;
            sessionState = SessionState.CONNECTED;
            connectStartTimeMs = 0;
            pendingConnectLevelId = (accept.mapId != null && !accept.mapId.isBlank()) ? accept.mapId : "game";
            Gdx.app.log(TAG, "Serwer zaakceptował połączenie, playerId=" + accept.assignedPlayerId);

            if (connectStatusCallback != null) connectStatusCallback.accept("Połączono! Ładowanie...");

        } else if (message instanceof NetworkProtocol.ServerReject reject) {
            abortConnect("Serwer odrzucił: " + reject.reason);
        }
    }

    /**
     * przerywanie łączenia — czyszczenie stanu, callback z błędem
     */
    private void abortConnect(String reason) {
        Gdx.app.error(TAG, "Błąd łączenia: " + reason);
        sessionState = SessionState.DISCONNECTED;
        connectStartTimeMs = 0;

        if (netDriver != null) {
            netDriver.dispose();
            netDriver = null;
        }

        pendingGameplayMessages.clear();
        pendingConnectLevelId = null;

        if (connectErrorCallback != null) {
            connectErrorCallback.accept(reason);
        }
        connectStatusCallback = null;
        connectErrorCallback = null;
    }

    /**
     * aktualizacja co klatkę — obsługuje timeout łączenia i processMessages w fazie CONNECTING.
     * wywoływana z Main lub LevelScreen.
     */
    public void update(float delta) {
        authService.tick(delta);

        // process network messages w fazie CONNECTING (menu jest aktywne, nie WorldContext)
        if (sessionState == SessionState.CONNECTING && netDriver != null) {
            netDriver.processMessages();

            if (sessionState == SessionState.CONNECTED && pendingConnectLevelId != null) {
                String levelId = pendingConnectLevelId;
                pendingConnectLevelId = null;
                travel(levelId);
                return;
            }

            // re-check: processMessages() may have changed state via handleConnectPhaseMessage
            if (sessionState != SessionState.CONNECTING) return;

            long elapsed = System.currentTimeMillis() - connectStartTimeMs;
            if (elapsed >= CONNECT_TIMEOUT_MS) {
                abortConnect("Przekroczono czas oczekiwania na serwer (" + (CONNECT_TIMEOUT_MS / 1000) + "s)");
            }
        }
    }

    // ===== Travel =====

    /**
     * Przechodzi do poziomu o podanym ID.
     * Niszczy poprzedni świat i tworzy nowy.
     * W multiplayer: blokuje travel("game") jeśli sesja nie jest CONNECTED.
     *
     * @param levelId identyfikator poziomu z LevelRegistry
     */
    public void travel(String levelId) {
        travel(levelId, travelCallback);
    }

    /**
     * Przechodzi do poziomu z dedykowanym callbackiem.
     * Guard: w multiplayer nie pozwala wejść do gry bez potwierdzonego połączenia.
     * Guard: blokuje travel w trakcie TRAVELLING (podwójny travel).
     *
     * @param levelId identyfikator poziomu
     * @param callback callback po inicjalizacji (nadpisuje domyślny)
     */
    public void travel(String levelId, LevelScreen.LevelReadyCallback callback) {
        if (levelScreen == null) {
            Gdx.app.error(TAG, "LevelScreen nie został ustawiony — nie można wykonać travel");
            return;
        }

        // guard: blokada podwójnego travel
        if (sessionState == SessionState.TRAVELLING) {
            Gdx.app.error(TAG, "Travel zablokowany — trwa poprzedni travel");
            return;
        }

        // guard: w multiplayer nie wchodź do gry bez potwierdzonego połączenia
        if (mode == Mode.MULTIPLAYER && !"main_menu".equals(levelId) && sessionState != SessionState.CONNECTED) {
            Gdx.app.error(TAG, "Travel zablokowany — brak połączenia z serwerem (stan: " + sessionState + ")");
            return;
        }

        LevelDefinition def = levelRegistry.get(levelId);
        Gdx.app.debug(TAG, "Travel: " + (currentLevelId != null ? currentLevelId : "<brak>")
            + " → " + levelId);

        currentLevelId = levelId;
        levelScreen.openLevel(def, callback);
    }

    /**
     * server travel — serwer informuje klientów o zmianie mapy.
     * wdrożone w pełni w Fazie 7.
     */
    public void serverTravel(String levelId) {
        if (sessionState == SessionState.TRAVELLING) {
            Gdx.app.error(TAG, "ServerTravel zablokowany — trwa poprzedni travel");
            return;
        }
        sessionState = SessionState.TRAVELLING;
        Gdx.app.debug(TAG, "ServerTravel: " + levelId);
        travel(levelId);
    }

    /**
     * client travel — klient otrzymuje polecenie zmiany mapy od serwera.
     * niszczy stary WorldContext, czyści stan sieciowy, ładuje nowy świat.
     */
    public void clientTravel(String levelId, String host) {
        if (sessionState == SessionState.TRAVELLING) {
            Gdx.app.error(TAG, "ClientTravel zablokowany — trwa poprzedni travel");
            return;
        }
        sessionState = SessionState.TRAVELLING;
        Gdx.app.debug(TAG, "ClientTravel: " + levelId + " @ " + host);
        this.serverHost = host;
        travel(levelId);
    }

    /**
     * powrót do menu — czyści sesję sieciową, niszczy NetDriver, resetuje stan.
     */
    public void returnToMenu(String reason) {
        Gdx.app.log(TAG, "Powrót do menu: " + reason);
        sessionState = SessionState.DISCONNECTED;
        if (netDriver != null) {
            netDriver.dispose();
            netDriver = null;
        }
        pendingGameplayMessages.clear();
        pendingConnectLevelId = null;
        chatHistory.clear();
        mode = Mode.SINGLE_PLAYER;
        localPlayerId = -1;
        connectStatusCallback = null;
        connectErrorCallback = null;
        travel("main_menu");
    }

    // ===== Dostęp =====

    public LevelRegistry getLevelRegistry() { return levelRegistry; }
    public String getCurrentLevelId() { return currentLevelId; }
    public LevelScreen getLevelScreen() { return levelScreen; }
    public AuthService getAuthService() { return authService; }

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

    public SessionState getSessionState() { return sessionState; }
    public void setSessionState(SessionState state) { this.sessionState = state; }

    // ===== Networking accessors =====

    public NetDriver getNetDriver() { return netDriver; }
    public List<Object> drainPendingGameplayMessages() {
        List<Object> messages = new ArrayList<>(pendingGameplayMessages.size());
        while (!pendingGameplayMessages.isEmpty()) {
            messages.add(pendingGameplayMessages.removeFirst());
        }
        return messages;
    }
    public void setNetDriver(NetDriver netDriver) { this.netDriver = netDriver; }
    public boolean isServer() { return isServer; }
    public void setServer(boolean server) { this.isServer = server; }
    public boolean isClient() { return !isServer && mode == Mode.MULTIPLAYER; }
    public boolean isSinglePlayer() { return mode == Mode.SINGLE_PLAYER; }
    public boolean isConnected() { return sessionState == SessionState.CONNECTED; }
    public void setConnected(boolean connected) {
        if (connected) {
            sessionState = SessionState.CONNECTED;
        } else if (sessionState == SessionState.CONNECTED) {
            sessionState = SessionState.DISCONNECTED;
        }
    }
    public float getServerTime() { return serverTime; }
    public void setServerTime(float serverTime) { this.serverTime = serverTime; }

    // ===== Chat accessors =====

    public void addChatMessage(NetworkProtocol.ChatMessage msg) {
        chatHistory.add(msg);
        while (chatHistory.size() > MAX_CHAT_HISTORY) {
            chatHistory.removeFirst();
        }
    }

    public List<NetworkProtocol.ChatMessage> getChatHistory() {
        return Collections.unmodifiableList(chatHistory);
    }

    public void clearChatHistory() {
        chatHistory.clear();
    }

    // ===== Chat input state =====

    private static boolean chatInputActive = false;

    public static boolean isChatInputActive() {
        return chatInputActive;
    }

    public static void setChatInputActive(boolean active) {
        chatInputActive = active;
    }
}

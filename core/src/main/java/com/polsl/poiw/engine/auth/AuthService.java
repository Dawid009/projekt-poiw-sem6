package com.polsl.poiw.engine.auth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Prosta warstwa do logowania, rejestrowania i wysylania statystyk gracza.
 * Trzyma tez zapamietane dane do logowania oraz aktualna sesje.
 */
public class AuthService {

    private static final String PREFS_NAME = "poiw-auth";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_EMAIL = "email";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final float REFRESH_INTERVAL_SECONDS = 60f;
    private static final float STATS_FLUSH_INTERVAL_SECONDS = 10f;

    private String rememberedLogin;
    private String rememberedPassword;
    private String rememberedEmail;

    private ActiveSession activeSession;
    private float refreshTimer;
    private float playtimeAccumulator;
    private boolean refreshInFlight;
    private final StatsDelta pendingStats = new StatsDelta();
    private final List<Runnable> postFlushCallbacks = new ArrayList<>();
    private float statsFlushTimer;
    private boolean statsFlushInFlight;
    private int pendingStatsUserId = -1;

    /** Wczytuje zapamietane dane przy starcie serwisu. */
    public AuthService() {
        loadRememberedCredentials();
    }

    /**
     * Aktualizuje licznik czasu gry i okresowo odswieza sesje oraz statystyki.
     * To jest wywolywane raz na frame.
     */
    public void tick(float delta) {
        if (activeSession == null || activeSession.offline) {
            return;
        }

        playtimeAccumulator += delta;
        while (playtimeAccumulator >= 1f) {
            activeSession.playtimeSeconds++;
            playtimeAccumulator -= 1f;
        }

        refreshTimer += delta;
        if (refreshTimer >= REFRESH_INTERVAL_SECONDS && !refreshInFlight) {
            refreshTimer -= REFRESH_INTERVAL_SECONDS;
            sendRefresh(activeSession.userId);
        }

        statsFlushTimer += delta;
        if (statsFlushTimer >= STATS_FLUSH_INTERVAL_SECONDS && !statsFlushInFlight) {
            statsFlushTimer -= STATS_FLUSH_INTERVAL_SECONDS;
            flushPendingStats(null);
        }
    }

    /** Sprawdza, czy jakakolwiek sesja jest obecnie aktywna. */
    public boolean isAuthenticated() {
        return activeSession != null;
    }

    /** Zwraca nazwe zalogowanego gracza albo pusty napis, gdy nie ma sesji. */
    public String getCurrentUsername() {
        return activeSession != null ? activeSession.username : "";
    }

    /** Sprawdza, czy gracz jest w trybie offline. */
    public boolean isOfflineSession() {
        return activeSession != null && activeSession.offline;
    }

    /** Zwraca email zalogowanego konta. */
    public String getCurrentEmail() {
        return activeSession != null ? activeSession.email : "";
    }

    /** Zwraca laczny czas gry zapisany w aktywnej sesji. */
    public long getCurrentPlaytimeSeconds() {
        return activeSession != null ? activeSession.playtimeSeconds : 0L;
    }

    /** Zwraca id zalogowanego uzytkownika lub -1, gdy nie ma sesji. */
    public int getCurrentUserId() {
        return activeSession != null ? activeSession.userId : -1;
    }

    /** Zwraca dane logowania zapamietane w Preferences. */
    public RememberedCredentials getRememberedCredentials() {
        return new RememberedCredentials(rememberedLogin, rememberedPassword, rememberedEmail);
    }

    /** Startuje lokalna sesje offline, bez backendu. */
    public SessionSnapshot startOfflineSession() {
        activeSession = new ActiveSession(-1, "", "offline", 0L, true);
        resetSessionTimers();
        resetStatsTransportState();
        return snapshot();
    }

    /**
     * Wysyla login i haslo do backendu oraz ustawia aktywna sesje po sukcesie.
     */
    public void login(String login, String password, AuthResultListener listener) {
        String normalizedLogin = login == null ? "" : login.trim();
        String normalizedPassword = password == null ? "" : password;

        if (normalizedLogin.isBlank() || normalizedPassword.isBlank()) {
            postFailure(listener, "Podaj login i haslo.");
            return;
        }

        String body = "{\"login\":\"" + escapeJson(normalizedLogin) + "\",\"haslo\":\""
            + escapeJson(normalizedPassword) + "\"}";

        sendPost("/auth/login", body, new ResponseHandler() {
            @Override
            public void onResponse(int statusCode, String responseBody) {
                if (!isSuccess(statusCode, responseBody)) {
                    postFailure(listener, extractError(responseBody, "Logowanie nieudane."));
                    return;
                }

                Integer userId = extractInt(responseBody, "id");
                String username = extractString(responseBody, "nazwa");
                String resolvedEmail = extractString(responseBody, "email");
                Long playtime = extractLong(responseBody, "czasWGrze");

                if (userId == null || username == null || resolvedEmail == null || playtime == null) {
                    postFailure(listener, "Backend zwrocil niepelne dane logowania.");
                    return;
                }

                if (pendingStatsUserId >= 0 && pendingStatsUserId != userId) {
                    clearPendingStats();
                }

                activeSession = new ActiveSession(userId, resolvedEmail, username, playtime, false);
                resetSessionTimers();
                resetStatsTransportState();
                recordGameEntry();
                rememberSuccessfulLogin(username, normalizedPassword, resolvedEmail);
                postSuccess(listener, snapshot());
            }

            @Override
            public void onFailure(String message) {
                postFailure(listener, message);
            }
        });
    }

    /**
     * Najpierw tworzy konto, a potem od razu loguje gracza na nowe dane.
     */
    public void register(String login, String email, String password, AuthResultListener listener) {
        String normalizedLogin = login == null ? "" : login.trim();
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedPassword = password == null ? "" : password;

        if (normalizedLogin.isBlank()) {
            postFailure(listener, "Podaj login.");
            return;
        }
        if (normalizedEmail.isBlank() || normalizedPassword.isBlank()) {
            postFailure(listener, "Podaj email i haslo.");
            return;
        }

        String body = "{\"login\":\"" + escapeJson(normalizedLogin) + "\",\"email\":\""
            + escapeJson(normalizedEmail) + "\",\"haslo\":\"" + escapeJson(normalizedPassword) + "\"}";

        sendPost("/auth/register", body, new ResponseHandler() {
            @Override
            public void onResponse(int statusCode, String responseBody) {
                if (!isSuccess(statusCode, responseBody)) {
                    postFailure(listener, extractError(responseBody, "Rejestracja nieudana."));
                    return;
                }

                login(normalizedLogin, normalizedPassword, listener);
            }

            @Override
            public void onFailure(String message) {
                postFailure(listener, message);
            }
        });
    }

    /**
     * Kończy sesje lokalnie i dodatkowo wysyla logout do backendu, jesli to tryb online.
     */
    public void logout(Runnable onComplete) {
        ActiveSession sessionToClose = activeSession;
        boolean offlineSession = sessionToClose != null && sessionToClose.offline;

        if (sessionToClose == null || offlineSession) {
            finishLogout(sessionToClose, onComplete);
            return;
        }

        flushPendingStats(() -> finishLogout(sessionToClose, onComplete));
    }

    /** Rejestruje pojedyncze sciecie drzewa do statystyk. */
    public void recordTreeCut() {
        queueStatsDelta(0, 0, 0, 0, 1, 0, 0);
    }

    /** Rejestruje zabicie przeciwnika do statystyk. */
    public void recordEnemyKill() {
        queueStatsDelta(0, 0, 1, 0, 0, 0, 0);
    }

    /** Rejestruje zabicie zwierzecia do statystyk. */
    public void recordAnimalKill() {
        queueStatsDelta(0, 0, 0, 1, 0, 0, 0);
    }

    /** Dodaje do kolejki zebrane surowce. */
    public void recordCollectedResources(int quantity) {
        if (quantity > 0) {
            queueStatsDelta(0, 0, 0, 0, 0, quantity, 0);
        }
    }

    /** Dodaje do kolejki zebrane plony. */
    public void recordCollectedCrops(int quantity) {
        if (quantity > 0) {
            queueStatsDelta(0, 0, 0, 0, 0, 0, quantity);
        }
    }

    /** Pobiera aktualne statystyki gracza z backendu. */
    public void fetchCurrentStats(StatsResultListener listener) {
        ActiveSession session = activeSession;
        if (session == null || session.offline) {
            postStatsFailure(listener, "Statystyki sa dostepne tylko dla zalogowanego konta.");
            return;
        }

        flushPendingStats(() -> sendGet("/stats/" + urlEncode(session.username), new ResponseHandler() {
            @Override
            public void onResponse(int statusCode, String responseBody) {
                if (statusCode < 200 || statusCode >= 300) {
                    postStatsFailure(listener, extractError(responseBody, "Nie udalo sie pobrac statystyk."));
                    return;
                }

                PlayerStatsSnapshot stats = parsePlayerStats(responseBody);
                if (stats == null) {
                    postStatsFailure(listener, "Backend zwrocil niepelne statystyki.");
                    return;
                }

                postStatsSuccess(listener, stats);
            }

            @Override
            public void onFailure(String message) {
                postStatsFailure(listener, message);
            }
        }));
    }

    /** Sprawdza, czy sa jeszcze jakies nienadane zmiany statystyk. */
    public boolean hasPendingStats() {
        return !pendingStats.isEmpty();
    }

    /** Zapisuje wejscie do gry jako jedna z metryk statystyk. */
    private void recordGameEntry() {
        queueStatsDelta(0, 1, 0, 0, 0, 0, 0);
        flushPendingStats(null);
    }

    /** Czyści sesje lokalnie i ewentualnie wysyla logout do backendu. */
    private void finishLogout(ActiveSession sessionToClose, Runnable onComplete) {
        boolean offlineSession = sessionToClose != null && sessionToClose.offline;
        activeSession = null;
        resetSessionTimers();
        resetStatsTransportState();
        if (!offlineSession) {
            clearRememberedCredentials();
        }

        if (onComplete != null) {
            onComplete.run();
        }

        if (sessionToClose == null || offlineSession) {
            return;
        }

        String body = "{\"id\":" + sessionToClose.userId + "}";
        sendPost("/auth/logout", body, new ResponseHandler() {
            @Override
            public void onResponse(int statusCode, String responseBody) {
                if (!isSuccess(statusCode, responseBody)) {
                    Gdx.app.error("AuthService", "Logout backend failed: " + extractError(responseBody, "logout error"));
                }
            }

            @Override
            public void onFailure(String message) {
                Gdx.app.error("AuthService", "Logout request failed: " + message);
            }
        });
    }

    /** Dodaje delte statystyk do bufora, ale tylko dla zalogowanej sesji online. */
    private void queueStatsDelta(int points,
                                 int entryCount,
                                 int enemyKills,
                                 int animalKills,
                                 int treesCut,
                                 int collectedResources,
                                 int collectedCrops) {
        ActiveSession session = activeSession;
        if (session == null || session.offline) {
            return;
        }

        if (pendingStatsUserId >= 0 && pendingStatsUserId != session.userId) {
            clearPendingStats();
        }

        pendingStatsUserId = session.userId;
        pendingStats.add(points, entryCount, enemyKills, animalKills, treesCut, collectedResources, collectedCrops);
    }

    /** Wysyla zebrane statystyki do backendu, a po sukcesie odpala callbacki. */
    private void flushPendingStats(Runnable afterFlush) {
        if (afterFlush != null) {
            postFlushCallbacks.add(afterFlush);
        }

        ActiveSession session = activeSession;
        if (session == null || session.offline) {
            runPostFlushCallbacks();
            return;
        }
        if (pendingStats.isEmpty()) {
            runPostFlushCallbacks();
            return;
        }
        if (pendingStatsUserId >= 0 && pendingStatsUserId != session.userId) {
            runPostFlushCallbacks();
            return;
        }
        if (statsFlushInFlight) {
            return;
        }

        statsFlushInFlight = true;
        StatsDelta statsToSend = pendingStats.copy();
        int userId = session.userId;
        String body = buildStatsUpdateBody(userId, statsToSend);
        sendPost("/scores", body, new ResponseHandler() {
            @Override
            public void onResponse(int statusCode, String responseBody) {
                statsFlushInFlight = false;
                if (isSuccess(statusCode, responseBody)) {
                    if (pendingStatsUserId == userId) {
                        pendingStats.subtract(statsToSend);
                        if (pendingStats.isEmpty()) {
                            pendingStatsUserId = -1;
                        }
                    }
                } else {
                    Gdx.app.error("AuthService", "Stats backend failed: "
                        + extractError(responseBody, "stats flush error"));
                }

                if (!pendingStats.isEmpty() && !postFlushCallbacks.isEmpty()) {
                    flushPendingStats(null);
                    return;
                }

                runPostFlushCallbacks();
            }

            @Override
            public void onFailure(String message) {
                statsFlushInFlight = false;
                Gdx.app.error("AuthService", "Stats request failed: " + message);
                runPostFlushCallbacks();
            }
        });
    }

    /** Buduje prosty JSON z delty statystyk. */
    private String buildStatsUpdateBody(int userId, StatsDelta stats) {
        return "{\"id\":" + userId +
            ",\"punkty\":" + stats.points +
            ",\"iloscWejsc\":" + stats.entryCount +
            ",\"iloscZabitychwPrzeciwnikow\":" + stats.enemyKills +
            ",\"iloscZabitychwZwierzat\":" + stats.animalKills +
            ",\"iloscScietychDrzew\":" + stats.treesCut +
            ",\"iloscZebranychSurowcow\":" + stats.collectedResources +
            ",\"iloscZebranychPlonow\":" + stats.collectedCrops +
            "}";
    }

    /** Parsuje odpowiedz backendu ze statystykami gracza. */
    private PlayerStatsSnapshot parsePlayerStats(String json) {
        Integer userId = extractInt(json, "id");
        String username = extractString(json, "nazwa");
        Integer points = extractInt(json, "punkty");
        Integer entryCount = extractInt(json, "iloscWejsc");
        Integer enemyKills = extractInt(json, "iloscZabitychwPrzeciwnikow");
        Integer animalKills = extractInt(json, "iloscZabitychwZwierzat");
        Integer treesCut = extractInt(json, "iloscScietychDrzew");
        Integer collectedResources = extractInt(json, "iloscZebranychSurowcow");
        Integer collectedCrops = extractInt(json, "iloscZebranychPlonow");

        if (userId == null || username == null || points == null || entryCount == null
            || enemyKills == null || animalKills == null || treesCut == null
            || collectedResources == null || collectedCrops == null) {
            return null;
        }

        return new PlayerStatsSnapshot(
            userId,
            username,
            points,
            entryCount,
            enemyKills,
            animalKills,
            treesCut,
            collectedResources,
            collectedCrops
        );
    }

    /** Czyści bufor statystyk, gdy sesja zmienia sie albo sie konczy. */
    private void clearPendingStats() {
        pendingStats.clear();
        pendingStatsUserId = -1;
    }

    /** Zeruje timery odpowiedzialne za odswiezanie sesji i playtime. */
    private void resetSessionTimers() {
        refreshTimer = 0f;
        playtimeAccumulator = 0f;
        refreshInFlight = false;
    }

    /** Zeruje stan wysylania statystyk. */
    private void resetStatsTransportState() {
        statsFlushTimer = 0f;
        statsFlushInFlight = false;
        postFlushCallbacks.clear();
    }

    /** Odpala wszystkie callbacki, ktore czekaly na flush statystyk. */
    private void runPostFlushCallbacks() {
        if (postFlushCallbacks.isEmpty()) {
            return;
        }

        List<Runnable> callbacks = new ArrayList<>(postFlushCallbacks);
        postFlushCallbacks.clear();
        for (Runnable callback : callbacks) {
            if (callback != null) {
                callback.run();
            }
        }
    }

    /** Wysyla ping odswiezajacy sesje na backendzie. */
    private void sendRefresh(int userId) {
        refreshInFlight = true;
        String body = "{\"id\":" + userId + "}";

        sendPost("/auth/refresh", body, new ResponseHandler() {
            @Override
            public void onResponse(int statusCode, String responseBody) {
                refreshInFlight = false;
                if (!isSuccess(statusCode, responseBody)) {
                    Gdx.app.error("AuthService", "Refresh backend failed: " + extractError(responseBody, "refresh error"));
                }
            }

            @Override
            public void onFailure(String message) {
                refreshInFlight = false;
                Gdx.app.error("AuthService", "Refresh request failed: " + message);
            }
        });
    }

    /** Wysyla zwykly request POST. */
    private void sendPost(String path, String body, ResponseHandler handler) {
        sendRequest(Net.HttpMethods.POST, path, body, handler);
    }

    /** Wysyla request GET. */
    private void sendGet(String path, ResponseHandler handler) {
        sendRequest(Net.HttpMethods.GET, path, null, handler);
    }

    /** Buduje i wysyla request HTTP, a odpowiedz przekazuje na glowny watek. */
    private void sendRequest(String method, String path, String body, ResponseHandler handler) {
        Net.HttpRequest request = new Net.HttpRequest(method);
        request.setUrl(resolveBaseUrl() + path);
        request.setHeader("Content-Type", "application/json; charset=UTF-8");
        request.setHeader("Accept", "application/json");
        if (body != null) {
            request.setContent(body);
        }
        request.setTimeOut(5000);

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String responseBody = httpResponse.getResultAsString();
                Gdx.app.postRunnable(() -> handler.onResponse(statusCode, responseBody));
            }

            @Override
            public void failed(Throwable t) {
                String message = t != null && t.getMessage() != null
                    ? t.getMessage()
                    : "Nie udalo sie polaczyc z backendem.";
                Gdx.app.postRunnable(() -> handler.onFailure(message));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> handler.onFailure("Zadanie HTTP zostalo anulowane."));
            }
        });
    }

    /** Koduje fragment URL tak, zeby bezpiecznie przeszedl w adresie zapytania. */
    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** Zwraca prosty snapshot obecnej sesji do UI. */
    private SessionSnapshot snapshot() {
        return activeSession == null
            ? null
            : new SessionSnapshot(activeSession.userId, activeSession.email, activeSession.username, activeSession.playtimeSeconds);
    }

    /** Wczytuje dane logowania zapisane lokalnie na dysku. */
    private void loadRememberedCredentials() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        rememberedLogin = prefs.getString(KEY_LOGIN, "");
        rememberedPassword = prefs.getString(KEY_PASSWORD, "");
        rememberedEmail = prefs.getString(KEY_EMAIL, "");
    }

    /** Zapamietuje dane udanego logowania, zeby UI moglo je potem podpowiedziec. */
    private void rememberSuccessfulLogin(String login, String password, String email) {
        rememberedLogin = login;
        rememberedPassword = password;
        rememberedEmail = email;

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(KEY_LOGIN, rememberedLogin);
        prefs.putString(KEY_PASSWORD, rememberedPassword);
        prefs.putString(KEY_EMAIL, rememberedEmail);
        prefs.flush();
    }

    /** Usuwa zapisane dane logowania z Preferences. */
    private void clearRememberedCredentials() {
        rememberedLogin = "";
        rememberedPassword = "";
        rememberedEmail = "";

        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.remove(KEY_LOGIN);
        prefs.remove(KEY_EMAIL);
        prefs.remove(KEY_PASSWORD);
        prefs.flush();
    }

    /** Odsyla wynik do listenera bez sprawdzania watkow wywolujacego kodu. */
    private void postSuccess(AuthResultListener listener, SessionSnapshot snapshot) {
        if (listener != null) {
            listener.onSuccess(snapshot);
        }
    }

    /** Odsyla blad do listenera bez sprawdzania watkow wywolujacego kodu. */
    private void postFailure(AuthResultListener listener, String message) {
        if (listener != null) {
            listener.onFailure(message);
        }
    }

    /** Odsyla wynik statystyk do listenera. */
    private void postStatsSuccess(StatsResultListener listener, PlayerStatsSnapshot snapshot) {
        if (listener != null) {
            listener.onSuccess(snapshot);
        }
    }

    /** Odsyla blad pobierania statystyk do listenera. */
    private void postStatsFailure(StatsResultListener listener, String message) {
        if (listener != null) {
            listener.onFailure(message);
        }
    }

    /** Czyta bazowy adres backendu z property systemowego albo z env. */
    private String resolveBaseUrl() {
        String systemProperty = System.getProperty("poiw.auth.baseUrl");
        if (systemProperty != null && !systemProperty.isBlank()) {
            return trimTrailingSlash(systemProperty);
        }

        String envValue = System.getenv("POIW_AUTH_BASE_URL");
        if (envValue != null && !envValue.isBlank()) {
            return trimTrailingSlash(envValue);
        }

        return DEFAULT_BASE_URL;
    }

    /** Usuwa koncowy slash z URL, zeby nie dublowac go przy sklejaniu sciezki. */
    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Sprawdza, czy odpowiedz backendu wyglada jak poprawny sukces. */
    private boolean isSuccess(int statusCode, String responseBody) {
        return statusCode >= 200 && statusCode < 300 && Boolean.TRUE.equals(extractBoolean(responseBody, "ok"));
    }

    /** Pobiera pole `blad` z JSON-a, a jak sie nie da, to zwraca fallback. */
    private String extractError(String json, String fallbackMessage) {
        String error = extractString(json, "blad");
        return error != null && !error.isBlank() ? error : fallbackMessage;
    }

    /** Zwraca wartosc logiczna z prostego JSON-a. */
    private Boolean extractBoolean(String json, String key) {
        String raw = extractRawValue(json, key);
        if (raw == null) {
            return null;
        }
        return Boolean.parseBoolean(raw);
    }

    /** Zwraca liczbe calkowita z prostego JSON-a. */
    private Integer extractInt(String json, String key) {
        String raw = extractRawValue(json, key);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Zwraca liczbe typu long z prostego JSON-a. */
    private Long extractLong(String json, String key) {
        String raw = extractRawValue(json, key);
        if (raw == null) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Wyciaga zwykly napis z bardzo prostego JSON-a bez pelnego parsera. */
    private String extractString(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }

        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length() || json.charAt(valueStart) != '"') {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (int i = valueStart + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (current == '\\' && i + 1 < json.length()) {
                char escaped = json.charAt(++i);
                switch (escaped) {
                    case '\\' -> result.append('\\');
                    case '"' -> result.append('"');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    default -> result.append(escaped);
                }
                continue;
            }
            if (current == '"') {
                return result.toString();
            }
            result.append(current);
        }

        return null;
    }

    /** Wyciaga surowa wartosc pola z prostego JSON-a. */
    private String extractRawValue(String json, String key) {
        if (json == null || json.isBlank()) {
            return null;
        }

        String marker = "\"" + key + "\"";
        int keyIndex = json.indexOf(marker);
        if (keyIndex < 0) {
            return null;
        }

        int colonIndex = json.indexOf(':', keyIndex + marker.length());
        if (colonIndex < 0) {
            return null;
        }

        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length()) {
            return null;
        }

        int valueEnd = valueStart;
        while (valueEnd < json.length() && json.charAt(valueEnd) != ',' && json.charAt(valueEnd) != '}') {
            valueEnd++;
        }
        return json.substring(valueStart, valueEnd).trim();
    }

    /** Escapuje znaki specjalne, zeby JSON byl poprawny. */
    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /** Listener uzywany po logowaniu i rejestracji. */
    public interface AuthResultListener {
        void onSuccess(SessionSnapshot session);
        void onFailure(String message);
    }

    /** Listener do pobierania statystyk gracza. */
    public interface StatsResultListener {
        void onSuccess(PlayerStatsSnapshot stats);
        void onFailure(String message);
    }

    /** Lokalna kopia danych logowania zapamietywanych w prefsach. */
    public record RememberedCredentials(String login, String password, String email) {
    }

    /** Krótki snapshot sesji, ktory trafia do UI po zalogowaniu. */
    public record SessionSnapshot(int userId, String email, String username, long playtimeSeconds) {
    }

    /** Dane statystyk gracza pobierane z backendu. */
    public record PlayerStatsSnapshot(int userId,
                                      String username,
                                      int points,
                                      int entryCount,
                                      int enemyKills,
                                      int animalKills,
                                      int treesCut,
                                      int collectedResources,
                                      int collectedCrops) {
    }

    /** Prosty interfejs do obslugi odpowiedzi HTTP. */
    private interface ResponseHandler {
        void onResponse(int statusCode, String responseBody);
        void onFailure(String message);
    }

    /** Bieżąca sesja logowania i jej podstawowe dane. */
    private static final class ActiveSession {
        private final int userId;
        private final String email;
        private final String username;
        private long playtimeSeconds;
        private final boolean offline;

        private ActiveSession(int userId, String email, String username, long playtimeSeconds, boolean offline) {
            this.userId = userId;
            this.email = email;
            this.username = username;
            this.playtimeSeconds = playtimeSeconds;
            this.offline = offline;
        }
    }

    /** Bufor zmian statystyk wysylanych do backendu. */
    private static final class StatsDelta {
        private int points;
        private int entryCount;
        private int enemyKills;
        private int animalKills;
        private int treesCut;
        private int collectedResources;
        private int collectedCrops;

        private void add(int points,
                         int entryCount,
                         int enemyKills,
                         int animalKills,
                         int treesCut,
                         int collectedResources,
                         int collectedCrops) {
            this.points += Math.max(0, points);
            this.entryCount += Math.max(0, entryCount);
            this.enemyKills += Math.max(0, enemyKills);
            this.animalKills += Math.max(0, animalKills);
            this.treesCut += Math.max(0, treesCut);
            this.collectedResources += Math.max(0, collectedResources);
            this.collectedCrops += Math.max(0, collectedCrops);
        }

        private void subtract(StatsDelta other) {
            if (other == null) {
                return;
            }

            points = Math.max(0, points - other.points);
            entryCount = Math.max(0, entryCount - other.entryCount);
            enemyKills = Math.max(0, enemyKills - other.enemyKills);
            animalKills = Math.max(0, animalKills - other.animalKills);
            treesCut = Math.max(0, treesCut - other.treesCut);
            collectedResources = Math.max(0, collectedResources - other.collectedResources);
            collectedCrops = Math.max(0, collectedCrops - other.collectedCrops);
        }

        private StatsDelta copy() {
            StatsDelta copy = new StatsDelta();
            copy.points = points;
            copy.entryCount = entryCount;
            copy.enemyKills = enemyKills;
            copy.animalKills = animalKills;
            copy.treesCut = treesCut;
            copy.collectedResources = collectedResources;
            copy.collectedCrops = collectedCrops;
            return copy;
        }

        private boolean isEmpty() {
            return points == 0
                && entryCount == 0
                && enemyKills == 0
                && animalKills == 0
                && treesCut == 0
                && collectedResources == 0
                && collectedCrops == 0;
        }

        private void clear() {
            points = 0;
            entryCount = 0;
            enemyKills = 0;
            animalKills = 0;
            treesCut = 0;
            collectedResources = 0;
            collectedCrops = 0;
        }
    }
}

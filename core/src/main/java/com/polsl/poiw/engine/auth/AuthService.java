package com.polsl.poiw.engine.auth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Preferences;

public class AuthService {

    private static final String PREFS_NAME = "poiw-auth";
    private static final String KEY_LOGIN = "login";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_EMAIL = "email";
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";
    private static final float REFRESH_INTERVAL_SECONDS = 60f;

    private String rememberedLogin;
    private String rememberedPassword;
    private String rememberedEmail;

    private ActiveSession activeSession;
    private float refreshTimer;
    private float playtimeAccumulator;
    private boolean refreshInFlight;

    public AuthService() {
        loadRememberedCredentials();
    }

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
    }

    public boolean isAuthenticated() {
        return activeSession != null;
    }

    public String getCurrentUsername() {
        return activeSession != null ? activeSession.username : "";
    }

    public boolean isOfflineSession() {
        return activeSession != null && activeSession.offline;
    }

    public String getCurrentEmail() {
        return activeSession != null ? activeSession.email : "";
    }

    public long getCurrentPlaytimeSeconds() {
        return activeSession != null ? activeSession.playtimeSeconds : 0L;
    }

    public int getCurrentUserId() {
        return activeSession != null ? activeSession.userId : -1;
    }

    public RememberedCredentials getRememberedCredentials() {
        return new RememberedCredentials(rememberedLogin, rememberedPassword, rememberedEmail);
    }

    public SessionSnapshot startOfflineSession() {
        activeSession = new ActiveSession(-1, "", "offline", 0L, true);
        refreshTimer = 0f;
        playtimeAccumulator = 0f;
        refreshInFlight = false;
        return snapshot();
    }

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

                activeSession = new ActiveSession(userId, resolvedEmail, username, playtime, false);
                refreshTimer = 0f;
                playtimeAccumulator = 0f;
                rememberSuccessfulLogin(username, normalizedPassword, resolvedEmail);
                postSuccess(listener, snapshot());
            }

            @Override
            public void onFailure(String message) {
                postFailure(listener, message);
            }
        });
    }

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

    public void logout(Runnable onComplete) {
        ActiveSession sessionToClose = activeSession;
        boolean offlineSession = sessionToClose != null && sessionToClose.offline;

        activeSession = null;
        refreshTimer = 0f;
        playtimeAccumulator = 0f;
        refreshInFlight = false;
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

    private void sendPost(String path, String body, ResponseHandler handler) {
        Net.HttpRequest request = new Net.HttpRequest(Net.HttpMethods.POST);
        request.setUrl(resolveBaseUrl() + path);
        request.setHeader("Content-Type", "application/json; charset=UTF-8");
        request.setHeader("Accept", "application/json");
        request.setContent(body);
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

    private SessionSnapshot snapshot() {
        return activeSession == null
            ? null
            : new SessionSnapshot(activeSession.userId, activeSession.email, activeSession.username, activeSession.playtimeSeconds);
    }

    private void loadRememberedCredentials() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        rememberedLogin = prefs.getString(KEY_LOGIN, "");
        rememberedPassword = prefs.getString(KEY_PASSWORD, "");
        rememberedEmail = prefs.getString(KEY_EMAIL, "");
    }

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

    private void postSuccess(AuthResultListener listener, SessionSnapshot snapshot) {
        if (listener != null) {
            listener.onSuccess(snapshot);
        }
    }

    private void postFailure(AuthResultListener listener, String message) {
        if (listener != null) {
            listener.onFailure(message);
        }
    }

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

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private boolean isSuccess(int statusCode, String responseBody) {
        return statusCode >= 200 && statusCode < 300 && Boolean.TRUE.equals(extractBoolean(responseBody, "ok"));
    }

    private String extractError(String json, String fallbackMessage) {
        String error = extractString(json, "blad");
        return error != null && !error.isBlank() ? error : fallbackMessage;
    }

    private Boolean extractBoolean(String json, String key) {
        String raw = extractRawValue(json, key);
        if (raw == null) {
            return null;
        }
        return Boolean.parseBoolean(raw);
    }

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

    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    public interface AuthResultListener {
        void onSuccess(SessionSnapshot session);
        void onFailure(String message);
    }

    public record RememberedCredentials(String login, String password, String email) {
    }

    public record SessionSnapshot(int userId, String email, String username, long playtimeSeconds) {
    }

    private interface ResponseHandler {
        void onResponse(int statusCode, String responseBody);
        void onFailure(String message);
    }

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
}
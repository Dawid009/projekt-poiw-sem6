package com.polsl.poiw.backend.http;

import com.polsl.poiw.backend.model.Gracz;
import com.polsl.poiw.backend.service.UzytkownikService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

// Serwer HTTP udostepniajacy endpointy rejestracji i logowania.
// POST /auth/register — rejestracja nowego konta
// POST /auth/login    — logowanie na istniejace konto
public class AuthHttpServer {

    private static final String SCIEZKA_REGISTER = "/auth/register";
    private static final String SCIEZKA_LOGIN = "/auth/login";

    private final HttpServer server;

    // Tworzy i konfiguruje serwer HTTP autoryzacji.
    public AuthHttpServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext(SCIEZKA_REGISTER, this::handleRegister);
        this.server.createContext(SCIEZKA_LOGIN, this::handleLogin);
        this.server.setExecutor(Executors.newFixedThreadPool(4));
    }

    // Uruchamia nasluchiwanie endpointow HTTP.
    public void start() {
        server.start();
    }

    // Obsluguje POST /auth/register
    // Body: {"email":"...","nazwa":"...","haslo":"..."}
    // Odpowiedz sukces: {"ok":true,"id":1,"nazwa":"..."}
    // Odpowiedz blad:   {"ok":false,"blad":"..."}
    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda POST\"}");
            return;
        }

        String body = readBody(exchange);

        String email = parseJsonPole(body, "email");
        String nazwa = parseJsonPole(body, "nazwa");
        String haslo = parseJsonPole(body, "haslo");

        if (email == null || nazwa == null || haslo == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pola: email, nazwa, haslo\"}");
            return;
        }

        int id = UzytkownikService.zarejestruj(email, nazwa, haslo);

        if (id == -1) {
            writeJson(exchange, 409, "{\"ok\":false,\"blad\":\"Rejestracja nieudana. Sprawdz dane lub czy email nie jest juz zajety.\"}");
            return;
        }

        String odpowiedz = "{\"ok\":true,\"id\":" + id + ",\"nazwa\":\"" + escapeJson(nazwa) + "\"}";
        writeJson(exchange, 201, odpowiedz);
    }

    // Obsluguje POST /auth/login
    // Body: {"email":"...","haslo":"..."}
    // Odpowiedz sukces: {"ok":true,"id":1,"nazwa":"...","email":"..."}
    // Odpowiedz blad:   {"ok":false,"blad":"..."}
    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda POST\"}");
            return;
        }

        String body = readBody(exchange);

        String email = parseJsonPole(body, "email");
        String haslo = parseJsonPole(body, "haslo");

        if (email == null || haslo == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pola: email, haslo\"}");
            return;
        }

        Gracz gracz = UzytkownikService.zaloguj(email, haslo);

        if (gracz == null) {
            writeJson(exchange, 401, "{\"ok\":false,\"blad\":\"Bledny email lub haslo\"}");
            return;
        }

        String odpowiedz = "{\"ok\":true" +
                           ",\"id\":" + gracz.getId() +
                           ",\"nazwa\":\"" + escapeJson(gracz.getNazwa()) + "\"" +
                           ",\"email\":\"" + escapeJson(gracz.getEmail()) + "\"" +
                           "}";
        writeJson(exchange, 200, odpowiedz);
    }

    // Wczytuje pelna tresc ciala zadania HTTP jako UTF-8.
    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Proste parsowanie wartosci pola z JSON-a.
    // Dziala dla prostych ciagów znakowych (nie obsluguje zagniezdzen ani tablic).
    private String parseJsonPole(String json, String klucz) {
        if (json == null || json.isBlank()) {
            return null;
        }

        String szukany = "\"" + klucz + "\"";
        int idx = json.indexOf(szukany);
        if (idx == -1) {
            return null;
        }

        int dwukropek = json.indexOf(':', idx + szukany.length());
        if (dwukropek == -1) {
            return null;
        }

        int poczatekCudzyslow = json.indexOf('"', dwukropek + 1);
        if (poczatekCudzyslow == -1) {
            return null;
        }

        int koniecCudzyslow = json.indexOf('"', poczatekCudzyslow + 1);
        if (koniecCudzyslow == -1) {
            return null;
        }

        return json.substring(poczatekCudzyslow + 1, koniecCudzyslow);
    }

    // Escapuje znaki specjalne, aby wynik byl poprawnym JSON-em.
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Zapisuje odpowiedz JSON do klienta HTTP.
    private void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}

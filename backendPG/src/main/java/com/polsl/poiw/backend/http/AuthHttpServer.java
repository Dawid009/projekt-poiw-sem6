package com.polsl.poiw.backend.http;

import com.polsl.poiw.backend.model.Gracz;
import com.polsl.poiw.backend.model.Punkty;
import com.polsl.poiw.backend.service.PunktyService;
import com.polsl.poiw.backend.service.SesjaManager;
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
// POST /auth/register  — rejestracja: login, email, haslo
// POST /auth/login     — logowanie: login, haslo
// POST /auth/logout    — wylogowanie: zapisuje czas sesji
// POST /auth/czas      — zapis czasu w grze
// GET  /auth/profil/{login} — profil gracza z czasem i statystykami
public class AuthHttpServer {

    private static final String SCIEZKA_REGISTER = "/auth/register";
    private static final String SCIEZKA_LOGIN    = "/auth/login";
    private static final String SCIEZKA_LOGOUT   = "/auth/logout";
    private static final String SCIEZKA_REFRESH  = "/auth/refresh";
    private static final String SCIEZKA_CZAS     = "/auth/czas";
    private static final String SCIEZKA_PROFIL   = "/auth/profil";

    private final HttpServer server;

    // Tworzy wlasny serwer HTTP autoryzacji na podanym porcie.
    public AuthHttpServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext(SCIEZKA_REGISTER, this::handleRegister);
        this.server.createContext(SCIEZKA_LOGIN,    this::handleLogin);
        this.server.createContext(SCIEZKA_LOGOUT,   this::handleLogout);
        this.server.createContext(SCIEZKA_REFRESH,  this::handleRefresh);
        this.server.createContext(SCIEZKA_CZAS,     this::handleCzas);
        this.server.createContext(SCIEZKA_PROFIL,   this::handleProfil);
        this.server.setExecutor(Executors.newFixedThreadPool(4));
    }

    // Rejestruje endpointy autoryzacji na istniejacym serwerze HTTP.
    public AuthHttpServer(HttpServer existingServer) {
        this.server = existingServer;
        this.server.createContext(SCIEZKA_REGISTER, this::handleRegister);
        this.server.createContext(SCIEZKA_LOGIN,    this::handleLogin);
        this.server.createContext(SCIEZKA_LOGOUT,   this::handleLogout);
        this.server.createContext(SCIEZKA_REFRESH,  this::handleRefresh);
        this.server.createContext(SCIEZKA_CZAS,     this::handleCzas);
        this.server.createContext(SCIEZKA_PROFIL,   this::handleProfil);
    }

    // Uruchamia nasluchiwanie endpointow HTTP.
    public void start() {
        server.start();
    }

    // Obsluguje POST /auth/register
    // Body: {"login":"...","email":"...","haslo":"..."}
    // Odpowiedz sukces: {"ok":true,"id":1,"nazwa":"..."}
    // Odpowiedz blad:   {"ok":false,"blad":"..."}
    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda POST\"}");
            return;
        }

        String body = readBody(exchange);

        String login = parseJsonPole(body, "login");
        String email = parseJsonPole(body, "email");
        String haslo = parseJsonPole(body, "haslo");

        if (login == null || email == null || haslo == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pola: login, email, haslo\"}");
            return;
        }

        int id = UzytkownikService.zarejestruj(email, login, haslo);

        if (id == -2) {
            writeJson(exchange, 409, "{\"ok\":false,\"blad\":\"Login jest juz zajety. Wybierz inny login.\"}");
            return;
        }
        if (id == -1) {
            writeJson(exchange, 409, "{\"ok\":false,\"blad\":\"Rejestracja nieudana. Sprawdz czy email nie jest juz zajety lub haslo ma min. 6 znakow.\"}");
            return;
        }

        String odpowiedz = "{\"ok\":true,\"id\":" + id + ",\"nazwa\":\"" + escapeJson(login) + "\"}";
        writeJson(exchange, 201, odpowiedz);
    }

    // Obsluguje POST /auth/login
    // Body: {"login":"...","haslo":"..."}
    // Odpowiedz sukces: {"ok":true,"id":1,"nazwa":"...","email":"...","czasWGrze":0,"statystyki":{...}}
    // Odpowiedz blad:   {"ok":false,"blad":"..."}
    private void handleLogin(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda POST\"}");
            return;
        }

        String body = readBody(exchange);

        String login = parseJsonPole(body, "login");
        String haslo = parseJsonPole(body, "haslo");

        if (login == null || haslo == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pola: login, haslo\"}");
            return;
        }

        Gracz gracz = UzytkownikService.zaloguj(login, haslo);

        if (gracz == null) {
            writeJson(exchange, 401, "{\"ok\":false,\"blad\":\"Bledny login lub haslo\"}");
            return;
        }

        String statystykiJson = buildStatystykiJson(gracz.getNazwa());

        // Zarejestruj aktywna sesje — czas bedzie zapisany przy zamknieciu backendu
        SesjaManager.rozpocznij(gracz.getId());

        String odpowiedz = "{\"ok\":true" +
                           ",\"id\":" + gracz.getId() +
                           ",\"nazwa\":\"" + escapeJson(gracz.getNazwa()) + "\"" +
                           ",\"email\":\"" + escapeJson(gracz.getEmail()) + "\"" +
                           ",\"czasWGrze\":" + gracz.getCzasWGrze() +
                           ",\"statystyki\":" + statystykiJson +
                           "}";
        writeJson(exchange, 200, odpowiedz);
    }

    // Obsluguje POST /auth/logout
    // Body: {"id":1}
    // Odpowiedz sukces: {"ok":true,"czasWGrze":3600}
    // Odpowiedz blad:   {"ok":false,"blad":"..."}
    private void handleLogout(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda POST\"}");
            return;
        }

        String body = readBody(exchange);
        String idStr = parseJsonPole(body, "id");

        if (idStr == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pole: id\"}");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Pole id musi byc liczba\"}");
            return;
        }

        // Konczy sesje i zapisuje czas do bazy od razu
        SesjaManager.zakoncz(id);

        // Pobierz nowy laczny czas z bazy
        long nowyCzas = UzytkownikService.pobierzCzasWGrze(id);

        writeJson(exchange, 200, "{\"ok\":true,\"czasWGrze\":" + nowyCzas + "}");
    }

    // Obsluguje POST /auth/refresh — heartbeat od klienta, podtrzymuje aktywna sesje.
    // Body: {"id":1}
    // Odpowiedz: {"ok":true}
    // Edge case: jesli gracza nie ma w mapie aktywnych (restart serwera / krotka przerwa sieci),
    //            automatycznie tworzy nowa sesje od teraz zamiast odrzucac request.
    private void handleRefresh(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda POST\"}");
            return;
        }

        String body = readBody(exchange);
        String idStr = parseJsonPole(body, "id");

        if (idStr == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pole: id\"}");
            return;
        }

        int id;
        try {
            id = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Pole id musi byc liczba\"}");
            return;
        }

        SesjaManager.refresh(id);
        writeJson(exchange, 200, "{\"ok\":true}");
    }

    // Obsluguje POST /auth/czas
    // Body: {"id":1,"sekundy":300}
    // Odpowiedz sukces: {"ok":true,"czasWGrze":1500}
    // Odpowiedz blad:   {"ok":false,"blad":"..."}
    private void handleCzas(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda POST\"}");
            return;
        }

        String body = readBody(exchange);

        String idStr = parseJsonPole(body, "id");
        String sekundyStr = parseJsonPole(body, "sekundy");

        if (idStr == null || sekundyStr == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pola: id, sekundy\"}");
            return;
        }

        int id;
        long sekundy;
        try {
            id = Integer.parseInt(idStr);
            sekundy = Long.parseLong(sekundyStr);
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Pola id i sekundy musza byc liczbami\"}");
            return;
        }

        long nowyCzas = com.polsl.poiw.backend.service.UzytkownikService.dodajCzasWGrze(id, sekundy);

        if (nowyCzas == -1) {
            writeJson(exchange, 404, "{\"ok\":false,\"blad\":\"Nie znaleziono gracza lub bledne dane\"}");
            return;
        }

        writeJson(exchange, 200, "{\"ok\":true,\"czasWGrze\":" + nowyCzas + "}");
    }

    // Obsluguje GET /auth/profil/{login}
    // Odpowiedz: {"ok":true,"nazwa":"...","email":"...","czasWGrze":0,"statystyki":{...}}
    private void handleProfil(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"ok\":false,\"blad\":\"Dozwolona jest tylko metoda GET\"}");
            return;
        }

        String sciezka = exchange.getRequestURI().getPath();
        String prefix = SCIEZKA_PROFIL + "/";
        if (!sciezka.startsWith(prefix) || sciezka.length() <= prefix.length()) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Uzyj adresu /auth/profil/{login}\"}");
            return;
        }

        String loginParam = java.net.URLDecoder.decode(
            sciezka.substring(prefix.length()), java.nio.charset.StandardCharsets.UTF_8);

        // Wyszukaj gracza po nazwie
        Gracz gracz = UzytkownikService.znajdzPoNazwie(loginParam);
        if (gracz == null) {
            writeJson(exchange, 404, "{\"ok\":false,\"blad\":\"Nie znaleziono gracza o podanym loginie\"}");
            return;
        }

        String statystykiJson = buildStatystykiJson(gracz.getNazwa());

        String odpowiedz = "{\"ok\":true" +
                           ",\"id\":" + gracz.getId() +
                           ",\"nazwa\":\"" + escapeJson(gracz.getNazwa()) + "\"" +
                           ",\"email\":\"" + escapeJson(gracz.getEmail()) + "\"" +
                           ",\"czasWGrze\":" + gracz.getCzasWGrze() +
                           ",\"statystyki\":" + statystykiJson +
                           "}";
        writeJson(exchange, 200, odpowiedz);
    }

    // Buduje JSON ze statystykami gracza na podstawie tabeli PUNKTY.
    private String buildStatystykiJson(String nazwaGracza) {
        java.util.List<Punkty> wyniki = PunktyService.getScoresByPlayer(nazwaGracza);
        int liczbaGier = wyniki.size();
        int najlepszyWynik = wyniki.stream().mapToInt(Punkty::getPunkty).max().orElse(0);
        long sumaWynikow = wyniki.stream().mapToLong(Punkty::getPunkty).sum();

        StringBuilder sb = new StringBuilder();
        sb.append("{\"liczbaGier\":").append(liczbaGier)
          .append(",\"najlepszyWynik\":").append(najlepszyWynik)
          .append(",\"sumaWynikow\":").append(sumaWynikow)
          .append(",\"wyniki\":[");

        for (int i = 0; i < wyniki.size(); i++) {
            Punkty p = wyniki.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"punkty\":").append(p.getPunkty())
              .append(",\"data\":\"")
              .append(escapeJson(p.getCreatedAt()))
              .append("\"}")
            ;
        }
        sb.append("]}");
        return sb.toString();
    }

    // Wczytuje pelna tresc ciala zadania HTTP jako UTF-8.
    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Proste parsowanie wartosci pola z JSON-a.
    // Dziala dla prostych wartosci: ciagi znakowe oraz liczby (nie obsluguje zagniezdzen ani tablic).
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

        // Wartosc moze byc ciagiem ("...") lub liczba (bez cudzyslowow)
        int poczatekWartosci = dwukropek + 1;
        while (poczatekWartosci < json.length() && json.charAt(poczatekWartosci) == ' ') {
            poczatekWartosci++;
        }
        if (poczatekWartosci >= json.length()) {
            return null;
        }

        if (json.charAt(poczatekWartosci) == '"') {
            // Wartosc tekstowa
            int koniecCudzyslow = json.indexOf('"', poczatekWartosci + 1);
            if (koniecCudzyslow == -1) {
                return null;
            }
            return json.substring(poczatekWartosci + 1, koniecCudzyslow);
        } else {
            // Wartosc liczbowa — konczy sie przecinkiem, } lub koncem stringa
            int koniec = poczatekWartosci;
            while (koniec < json.length() && json.charAt(koniec) != ',' && json.charAt(koniec) != '}') {
                koniec++;
            }
            return json.substring(poczatekWartosci, koniec).trim();
        }
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

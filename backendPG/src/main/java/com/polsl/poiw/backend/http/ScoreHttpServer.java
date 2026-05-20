package com.polsl.poiw.backend.http;

import com.polsl.poiw.backend.model.Punkty;
import com.polsl.poiw.backend.service.PunktyService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

// Serwer HTTP udostepniajacy endpointy z wynikami i statystykami graczy.
// GET  /scores                â€” statystyki wszystkich graczy (posortowane)
// GET  /scores/{nazwaGracza} â€” statystyki konkretnego gracza
// POST /scores                â€” dodaje (kumuluje) statystyki z sesji gry
// GET  /stats                 â€” alias dla GET /scores
// GET  /stats/{nazwaGracza}  â€” alias dla GET /scores/{nazwaGracza}
public class ScoreHttpServer {
    private static final String SCIEZKA_ENDPOINTU = "/scores";
    private static final String SCIEZKA_STATYSTYK = "/stats";

    private final HttpServer server;

    // Tworzy i konfiguruje serwer HTTP
    public ScoreHttpServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext(SCIEZKA_ENDPOINTU, this::handleScores);
        this.server.createContext(SCIEZKA_STATYSTYK, this::handleStats);
        this.server.setExecutor(Executors.newFixedThreadPool(4));
    }

    // Uruchamia nasluchiwanie endpointu HTTP
    public void start() {
        server.start();
    }

    // Zwraca instancje serwera HTTP â€” umozliwia rejestracje dodatkowych kontekstow
    public HttpServer getServer() {
        return server;
    }

    // Obsluguje GET i POST /scores oraz /scores/{nazwaGracza}
    private void handleScores(HttpExchange exchange) throws IOException {
        String metoda = exchange.getRequestMethod();

        if ("POST".equalsIgnoreCase(metoda)) {
            handlePostScore(exchange);
        } else if ("GET".equalsIgnoreCase(metoda)) {
            handleGetScores(exchange);
        } else {
            writeJson(exchange, 405, "{\"blad\":\"Dozwolone metody: GET, POST\"}");
        }
    }

    // Obsluguje GET /stats oraz /stats/{nazwaGracza} (alias dla /scores)
    private void handleStats(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"blad\":\"Dozwolona jest tylko metoda GET\"}");
            return;
        }

        String sciezka = exchange.getRequestURI().getPath();
        if (SCIEZKA_STATYSTYK.equals(sciezka) || (SCIEZKA_STATYSTYK + "/").equals(sciezka)) {
            List<Punkty> wszystkie = PunktyService.getAllScoresSorted();
            writeJson(exchange, 200, buildListResponse(wszystkie));
            return;
        }

        String nazwaGracza = getPlayerNameFromPath(sciezka, SCIEZKA_STATYSTYK);
        if (nazwaGracza == null || nazwaGracza.isBlank()) {
            writeJson(exchange, 400, "{\"blad\":\"Uzyj adresu /stats/{nazwaGracza}\"}");
            return;
        }

        Punkty stats = PunktyService.getStatsByPlayerName(nazwaGracza);
        if (stats == null) {
            writeJson(exchange, 404, "{\"blad\":\"Nie znaleziono gracza o podanej nazwie\"}");
            return;
        }
        writeJson(exchange, 200, buildSingleResponse(stats));
    }

    // Obsluguje GET /scores (wszyscy) oraz /scores/{nazwaGracza}
    private void handleGetScores(HttpExchange exchange) throws IOException {
        String sciezka = exchange.getRequestURI().getPath();
        if (SCIEZKA_ENDPOINTU.equals(sciezka) || (SCIEZKA_ENDPOINTU + "/").equals(sciezka)) {
            List<Punkty> wszystkie = PunktyService.getAllScoresSorted();
            writeJson(exchange, 200, buildListResponse(wszystkie));
            return;
        }

        String nazwaGracza = getPlayerNameFromPath(sciezka, SCIEZKA_ENDPOINTU);
        if (nazwaGracza == null || nazwaGracza.isBlank()) {
            writeJson(exchange, 400, "{\"blad\":\"Uzyj adresu /scores/{nazwaGracza}\"}");
            return;
        }

        Punkty stats = PunktyService.getStatsByPlayerName(nazwaGracza);
        if (stats == null) {
            writeJson(exchange, 404, "{\"blad\":\"Nie znaleziono gracza o podanej nazwie\"}");
            return;
        }
        writeJson(exchange, 200, buildSingleResponse(stats));
    }

    // Obsluguje POST /scores
    // Body: {"id":1,"punkty":500,"iloscWejsc":1,"iloscZabitychwPrzeciwnikow":2,
    //        "iloscZabitychwZwierzat":0,"iloscScietychDrzew":5,
    //        "iloscZebranychSurowcow":10,"iloscZebranychPlonow":3}
    // Odpowiedz sukces: {"ok":true}
    // Odpowiedz blad:   {"ok":false,"blad":"..."}
    private void handlePostScore(HttpExchange exchange) throws IOException {
        String body = readBody(exchange);

        String idStr                        = parseJsonPole(body, "id");
        String punktyStr                    = parseJsonPole(body, "punkty");
        String wejsciaStr                   = parseJsonPole(body, "iloscWejsc");
        String przeciwnikowStr              = parseJsonPole(body, "iloscZabitychwPrzeciwnikow");
        String zwierzatStr                  = parseJsonPole(body, "iloscZabitychwZwierzat");
        String drzewStr                     = parseJsonPole(body, "iloscScietychDrzew");
        String surowcowStr                  = parseJsonPole(body, "iloscZebranychSurowcow");
        String plonowStr                    = parseJsonPole(body, "iloscZebranychPlonow");

        if (idStr == null || punktyStr == null) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wymagane pola: id, punkty\"}");
            return;
        }

        int id, punkty, iloscWejsc, iloscPrzeciwnikow, iloscZwierzat, iloscDrzew, iloscSurowcow, iloscPlonow;
        try {
            id               = Integer.parseInt(idStr);
            punkty           = Integer.parseInt(punktyStr);
            iloscWejsc       = wejsciaStr       != null ? Integer.parseInt(wejsciaStr)       : 0;
            iloscPrzeciwnikow = przeciwnikowStr  != null ? Integer.parseInt(przeciwnikowStr)  : 0;
            iloscZwierzat    = zwierzatStr       != null ? Integer.parseInt(zwierzatStr)       : 0;
            iloscDrzew       = drzewStr          != null ? Integer.parseInt(drzewStr)          : 0;
            iloscSurowcow    = surowcowStr       != null ? Integer.parseInt(surowcowStr)       : 0;
            iloscPlonow      = plonowStr         != null ? Integer.parseInt(plonowStr)         : 0;
        } catch (NumberFormatException e) {
            writeJson(exchange, 400, "{\"ok\":false,\"blad\":\"Wartosci liczbowe sa niepoprawne\"}");
            return;
        }

        boolean ok = PunktyService.addScore(id, punkty, iloscWejsc, iloscPrzeciwnikow,
                                            iloscZwierzat, iloscDrzew, iloscSurowcow, iloscPlonow);

        if (ok) {
            writeJson(exchange, 200, "{\"ok\":true}");
        } else {
            writeJson(exchange, 404, "{\"ok\":false,\"blad\":\"Nie znaleziono gracza o podanym id\"}");
        }
    }

    // Pobiera nazwe gracza ze sciezki /{prefix}/{nazwaGracza}
    private String getPlayerNameFromPath(String sciezka, String prefix) {
        if (sciezka == null || sciezka.isBlank()) {
            return null;
        }

        String poczatek = prefix + "/";
        if (!sciezka.startsWith(poczatek)) {
            return null;
        }

        String zakodowanaNazwa = sciezka.substring(poczatek.length());
        if (zakodowanaNazwa.isBlank()) {
            return null;
        }

        return URLDecoder.decode(zakodowanaNazwa, StandardCharsets.UTF_8);
    }

    // Buduje odpowiedz JSON z lista statystyk graczy
    private String buildListResponse(List<Punkty> lista) {
        StringBuilder json = new StringBuilder();
        json.append("{\"count\":").append(lista.size()).append(",\"scores\":[");

        for (int i = 0; i < lista.size(); i++) {
            if (i > 0) json.append(",");
            json.append(buildSingleResponse(lista.get(i)));
        }

        json.append("]}");
        return json.toString();
    }

    // Buduje JSON dla jednego wiersza statystyk gracza
    private String buildSingleResponse(Punkty p) {
        return "{" +
            "\"id\":" + p.getId() + "," +
            "\"nazwa\":\"" + escapeJson(p.getNazwa()) + "\"," +
            "\"punkty\":" + p.getPunkty() + "," +
            "\"iloscWejsc\":" + p.getIloscWejsc() + "," +
            "\"iloscZabitychwPrzeciwnikow\":" + p.getIloscZabitychwPrzeciwnikow() + "," +
            "\"iloscZabitychwZwierzat\":" + p.getIloscZabitychwZwierzat() + "," +
            "\"iloscScietychDrzew\":" + p.getIloscScietychDrzew() + "," +
            "\"iloscZebranychSurowcow\":" + p.getIloscZebranychSurowcow() + "," +
            "\"iloscZebranychPlonow\":" + p.getIloscZebranychPlonow() +
            "}";
    }

    // Proste parsowanie wartosci pola z JSON-a (ciagi i liczby, bez zagniezdzen).
    private String parseJsonPole(String json, String klucz) {
        if (json == null || json.isBlank()) return null;
        String szukany = "\"" + klucz + "\"";
        int idx = json.indexOf(szukany);
        if (idx == -1) return null;
        int dwukropek = json.indexOf(':', idx + szukany.length());
        if (dwukropek == -1) return null;
        int poczatek = dwukropek + 1;
        while (poczatek < json.length() && json.charAt(poczatek) == ' ') poczatek++;
        if (poczatek >= json.length()) return null;
        if (json.charAt(poczatek) == '"') {
            int koniec = json.indexOf('"', poczatek + 1);
            if (koniec == -1) return null;
            return json.substring(poczatek + 1, koniec);
        } else {
            int koniec = poczatek;
            while (koniec < json.length() && json.charAt(koniec) != ',' && json.charAt(koniec) != '}') koniec++;
            return json.substring(poczatek, koniec).trim();
        }
    }

    // Wczytuje pelna tresc ciala zadania HTTP jako UTF-8.
    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // Escapuje znaki specjalne, aby wynik byl poprawnym JSON-em
    private String escapeJson(String input) {
        if (input == null) return "";
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Zapisuje odpowiedz JSON do klienta HTTP
    private void writeJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}

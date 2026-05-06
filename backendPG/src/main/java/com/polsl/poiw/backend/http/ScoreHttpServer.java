package com.polsl.poiw.backend.http;

import com.polsl.poiw.backend.model.Punkty;
import com.polsl.poiw.backend.service.PunktyService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

// Serwer HTTP udostepniajacy endpoint z wynikami gracza
public class ScoreHttpServer {
    private static final String SCIEZKA_ENDPOINTU = "/scores";

    private final HttpServer server;

    // Tworzy i konfiguruje serwer HTTP
    public ScoreHttpServer(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        this.server.createContext(SCIEZKA_ENDPOINTU, this::handleGetScoresByPlayer);
        this.server.setExecutor(Executors.newFixedThreadPool(4));
    }

    // Uruchamia nasluchiwanie endpointu HTTP
    public void start() {
        server.start();
    }

    // Zwraca instancje serwera HTTP — umozliwia rejestracje dodatkowych kontekstow
    public HttpServer getServer() {
        return server;
    }

    // Obsluguje GET /scores (wszystkie wyniki) oraz /scores/{nazwaGracza}
    private void handleGetScoresByPlayer(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            writeJson(exchange, 405, "{\"blad\":\"Dozwolona jest tylko metoda GET\"}");
            return;
        }

        String sciezka = exchange.getRequestURI().getPath();
        if (SCIEZKA_ENDPOINTU.equals(sciezka) || (SCIEZKA_ENDPOINTU + "/").equals(sciezka)) {
            List<Punkty> wszystkieWyniki = PunktyService.getAllScoresSorted();
            String odpowiedz = buildScoresResponse(null, wszystkieWyniki);
            writeJson(exchange, 200, odpowiedz);
            return;
        }

        String nazwaGracza = getPlayerNameFromPath(sciezka);

        if (nazwaGracza == null || nazwaGracza.isBlank()) {
            writeJson(exchange, 400, "{\"blad\":\"Uzyj adresu /scores/{nazwaGracza}\"}");
            return;
        }

        List<Punkty> wyniki = PunktyService.getScoresByPlayer(nazwaGracza);
        String odpowiedz = buildScoresResponse(nazwaGracza, wyniki);
        writeJson(exchange, 200, odpowiedz);
    }

    // Pobiera nazwe gracza ze sciezki /scores/{nazwaGracza}
    private String getPlayerNameFromPath(String sciezka) {
        if (sciezka == null || sciezka.isBlank()) {
            return null;
        }

        if (!sciezka.startsWith(SCIEZKA_ENDPOINTU + "/")) {
            return null;
        }

        String zakodowanaNazwa = sciezka.substring((SCIEZKA_ENDPOINTU + "/").length());
        if (zakodowanaNazwa.isBlank()) {
            return null;
        }

        return URLDecoder.decode(zakodowanaNazwa, StandardCharsets.UTF_8);
    }

    // Buduje odpowiedz JSON zawierajaca wyniki wskazanego gracza
    private String buildScoresResponse(String nazwaGracza, List<Punkty> wyniki) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        if (nazwaGracza != null && !nazwaGracza.isBlank()) {
            json.append("\"nazwaGracza\":\"").append(escapeJson(nazwaGracza)).append("\",");
        }
        json.append("\"count\":").append(wyniki.size()).append(",");
        json.append("\"scores\":[");

        for (int i = 0; i < wyniki.size(); i++) {
            Punkty p = wyniki.get(i);
            if (i > 0) {
                json.append(",");
            }

            json.append("{")
                .append("\"id\":").append(p.getId()).append(",")
                .append("\"nazwaGracza\":\"").append(escapeJson(p.getNazwaGracza())).append("\",")
                .append("\"punkty\":").append(p.getPunkty()).append(",")
                .append("\"createdAt\":\"").append(escapeJson(p.getCreatedAt())).append("\"")
                .append("}");
        }

        json.append("]}");
        return json.toString();
    }

    // Escapuje znaki specjalne, aby wynik byl poprawnym JSON-em
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
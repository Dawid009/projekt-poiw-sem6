package com.polsl.poiw.backend;

import com.polsl.poiw.backend.config.DatabaseConfig;
import com.polsl.poiw.backend.http.AuthHttpServer;
import com.polsl.poiw.backend.http.ScoreHttpServer;
import com.polsl.poiw.backend.service.PunktyService;
import com.polsl.poiw.backend.service.SesjaManager;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;

public class BackendLauncher {

    public static void main(String[] args) {
        System.out.println("Uruchamianie Bazy\n");

        // 1. Inicjalizacja bazy danych (tabele PUNKTY i GRACZE)
        DatabaseConfig.initializeDatabase();

        System.out.println("\nZapisywanie wynikow\n");

        // 2. Zapisywanie wyników
        /*PunktyService.addScore("Gracz1", 1500);
        PunktyService.addScore("Gracz2", 2300);
        PunktyService.addScore("Gracz1", 1800);
        PunktyService.addScore("Gracz3", 900);*/

        // 3. Wyświetlenie wszystkich wyników posortowanych
        /*System.out.println("\nWszystkie wyniki (posortowane od najlepszego)");
        PunktyService.getAllScoresSorted().forEach(System.out::println);*/

        // 4. Wyświetlenie TOP 2
        System.out.println("\nTOP 2 wyniki");
        PunktyService.getTopScores(2).forEach(System.out::println);

        // 5. Statystyki
        System.out.println("\nStatystyki");
        System.out.println("\nCalkowita liczba wynikow: " + PunktyService.getScoreCount());

        // 6. Serwer HTTP (port 8080) 
        try {
            int port = 8080;
            ScoreHttpServer scoreServer = new ScoreHttpServer(port);
            new AuthHttpServer(scoreServer.getServer()); // rejestruje /auth/* na tym samym serwerze
            scoreServer.start();
            SesjaManager.uruchomSprawdzanie(60); // sprawdza timeout sesji co 60s (timeout=150s)
            System.out.println("\nHTTP endpoint uruchomiony na porcie " + port + ":");
            System.out.println("  GET  http://localhost:" + port + "/scores");
            System.out.println("  GET  http://localhost:" + port + "/scores/{nazwaGracza}");
            System.out.println("  POST http://localhost:" + port + "/auth/register");
            System.out.println("  POST http://localhost:" + port + "/auth/login");
            System.out.println("  POST http://localhost:" + port + "/auth/logout");
            System.out.println("  POST http://localhost:" + port + "/auth/refresh  (heartbeat co 60s)");
            System.out.println("  POST http://localhost:" + port + "/auth/czas");

            // Przy zamknieciu backendu (Ctrl+C) zapisz czas wszystkich aktywnych sesji
            final HttpServer httpServer = scoreServer.getServer();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nZamykanie backendu — zapisywanie czasu aktywnych sesji...");
                // Tylko zapisuje czas (jak auto-zapis)
                // Sesje sa zamykane wylacznie przez /auth/logout.
                SesjaManager.autoZapis();
                httpServer.stop(0);
                System.out.println("Backend zamkniety.");
            }));
        } catch (IOException e) {
            System.err.println("Nie udalo sie uruchomic serwera HTTP: " + e.getMessage());
        }
    }
}

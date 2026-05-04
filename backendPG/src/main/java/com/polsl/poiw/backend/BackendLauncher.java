package com.polsl.poiw.backend;

import com.polsl.poiw.backend.config.DatabaseConfig;
import com.polsl.poiw.backend.http.AuthHttpServer;
import com.polsl.poiw.backend.http.ScoreHttpServer;
import com.polsl.poiw.backend.service.PunktyService;

import java.io.IOException;

public class BackendLauncher {

    public static void main(String[] args) {
        System.out.println("Uruchamianie Bazy\n");

        // 1. Inicjalizacja bazy danych (tabele PUNKTY i GRACZE)
        DatabaseConfig.initializeDatabase();

        System.out.println("\nZapisywanie wynikow\n");

        // 2. Zapisywanie wyników
        PunktyService.addScore("Gracz1", 1500);
        PunktyService.addScore("Gracz2", 2300);
        PunktyService.addScore("Gracz1", 1800);
        PunktyService.addScore("Gracz3", 900);

        // 3. Wyświetlenie wszystkich wyników posortowanych
        System.out.println("\nWszystkie wyniki (posortowane od najlepszego)");
        PunktyService.getAllScoresSorted().forEach(System.out::println);

        // 4. Wyświetlenie TOP 2
        System.out.println("\nTOP 2 wyniki");
        PunktyService.getTopScores(2).forEach(System.out::println);

        // 5. Statystyki
        System.out.println("\nStatystyki");
        System.out.println("\nCalkowita liczba wynikow: " + PunktyService.getScoreCount());

        // 6. Serwer wynikow (port 8080)
        try {
            int port = 8080;
            ScoreHttpServer scoreServer = new ScoreHttpServer(port);
            scoreServer.start();
            System.out.println("\nHTTP endpoint uruchomiony: GET /scores oraz GET /scores/{nazwaGracza}");
            System.out.println("Przyklad (wszystkie): http://localhost:" + port + "/scores");
            System.out.println("Przyklad (gracz): http://localhost:" + port + "/scores/Gracz1");
        } catch (IOException e) {
            System.err.println("Nie udalo sie uruchomic serwera wynikow: " + e.getMessage());
        }

        // 7. Serwer autoryzacji (port 8081)
        try {
            int authPort = 8081;
            AuthHttpServer authServer = new AuthHttpServer(authPort);
            authServer.start();
            System.out.println("\nHTTP endpoint autoryzacji uruchomiony:");
            System.out.println("  POST http://localhost:" + authPort + "/auth/register");
            System.out.println("  POST http://localhost:" + authPort + "/auth/login");
        } catch (IOException e) {
            System.err.println("Nie udalo sie uruchomic serwera autoryzacji: " + e.getMessage());
        }
    }
}

package com.polsl.poiw.backend;
import com.polsl.poiw.backend.config.DatabaseConfig;
import com.polsl.poiw.backend.service.PunktyService;

public class BackendLauncher {

    public static void main(String[] args) {
        System.out.println("Uruchamianie Bazy\n");

        // 1. Inicjalizacja bazy danych
        DatabaseConfig.initializeDatabase();
        
        System.out.println("\nZapisywanie wyników\n");
        
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
        System.out.println("\nCałkowita liczba wyników: " + PunktyService.getScoreCount());
        
        
    }
}

package com.polsl.poiw.backend.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Przechowuje aktywne sesje graczy w pamieci (od momentu logowania).
// Przy zamknieciu backendu zapiszWszystkie() utrwala czas w bazie.
public final class SesjaManager {

    // Klucz: id gracza, Wartosc: moment logowania
    private static final Map<Integer, Instant> aktywne = new ConcurrentHashMap<>();

    private SesjaManager() {}

    // Rejestruje sesje gracza — wywolywane przy logowaniu.
    public static void rozpocznij(int id) {
        aktywne.put(id, Instant.now());
        System.out.println("SesjaManager: sesja rozpoczeta dla gracza ID " + id);
    }

    // Konczy sesje gracza i zapisuje czas do bazy — wywolywane przy wylogowaniu.
    public static void zakoncz(int id) {
        Instant start = aktywne.remove(id);
        if (start == null) return;
        long sekundy = Instant.now().getEpochSecond() - start.getEpochSecond();
        if (sekundy > 0) {
            UzytkownikService.dodajCzasWGrze(id, sekundy);
            System.out.println("SesjaManager: zapisano " + sekundy + "s dla gracza ID " + id);
        }
    }

    // Konczy wszystkie aktywne sesje i zapisuje czas — wywolywane przy shutdown backendu.
    public static void zapiszWszystkie() {
        if (aktywne.isEmpty()) {
            System.out.println("SesjaManager: brak aktywnych sesji do zapisania.");
            return;
        }
        System.out.println("SesjaManager: zapisywanie " + aktywne.size() + " aktywnych sesji...");
        // kopiujemy klucze zeby uniknac ConcurrentModificationException
        for (int id : aktywne.keySet().toArray(new Integer[0])) {
            zakoncz(id);
        }
        System.out.println("SesjaManager: wszystkie sesje zapisane.");
    }

    public static int liczbaAktywnych() {
        return aktywne.size();
    }
}

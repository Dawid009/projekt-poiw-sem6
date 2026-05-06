package com.polsl.poiw.backend.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Przechowuje aktywne sesje graczy.
// Klient wysyla heartbeat co minute na /auth/refresh.
// Jezeli heartbeat nie dotrze przez TIMEOUT_SEKUND, sesja jest zamykana automatycznie.
public final class SesjaManager {

    // Dane pojedynczej sesji
    public static class SesjaInfo {
        public final Instant start;          // moment startu sesji (login lub reconnect)
        public volatile Instant lastRefresh; // ostatni heartbeat od klienta

        public SesjaInfo(Instant start) {
            this.start = start;
            this.lastRefresh = start;
        }
    }

    // Klucz: id gracza, Wartosc: dane sesji
    private static final Map<Integer, SesjaInfo> aktywne = new ConcurrentHashMap<>();

    // Sesja jest zamykana automatycznie jesli brak refresha przez tyle sekund.
    // 150s = 2.5 minuty - margines na jednominutowy refresh klienta
    private static final long TIMEOUT_SEKUND = 150;

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "SesjaChecker");
        t.setDaemon(true);
        return t;
    });

    private SesjaManager() {}

    // Rejestruje sesje gracza — wywolywane przy logowaniu.
    public static void rozpocznij(int id) {
        aktywne.put(id, new SesjaInfo(Instant.now()));
        System.out.println("SesjaManager: sesja rozpoczeta dla gracza ID " + id);
    }

    // Heartbeat od klienta — aktualizuje czas ostatniego kontaktu.
    // Edge case: jesli gracza nie ma w mapie (restart serwera / krotka przerwa w sieci),
    // dodaje nowa sesje od teraz zamiast odrzucac request.
    public static void refresh(int id) {
        aktywne.compute(id, (k, sesja) -> {
            if (sesja == null) {
                System.out.println("SesjaManager: reconnect gracza ID " + id + " — nowa sesja od teraz");
                return new SesjaInfo(Instant.now());
            }
            sesja.lastRefresh = Instant.now();
            return sesja;
        });
    }

    // Konczy sesje gracza i zapisuje pelny czas — wywolywane przy wylogowaniu przez /auth/logout.
    public static void zakoncz(int id) {
        SesjaInfo sesja = aktywne.remove(id);
        if (sesja == null) return;
        long sekundy = Instant.now().getEpochSecond() - sesja.start.getEpochSecond();
        if (sekundy > 0) {
            UzytkownikService.dodajCzasWGrze(id, sekundy);
            System.out.println("SesjaManager: wylogowanie ID " + id + ", zapisano " + sekundy + "s");
        }
    }

    // Sprawdza wszystkie sesje i zamyka te bez refresha od TIMEOUT_SEKUND.
    // Zapisuje czas do ostatniego refresha (potwierdzony czas aktywnosci klienta).
    // Wywolywane co ~60s przez scheduler.
    public static void sprawdzTimeout() {
        if (aktywne.isEmpty()) return;
        Instant teraz = Instant.now();
        for (Map.Entry<Integer, SesjaInfo> wpis : aktywne.entrySet()) {
            int id = wpis.getKey();
            SesjaInfo sesja = wpis.getValue();
            long odRefresh = teraz.getEpochSecond() - sesja.lastRefresh.getEpochSecond();
            if (odRefresh > TIMEOUT_SEKUND) {
                aktywne.remove(id);
                long sekundy = sesja.lastRefresh.getEpochSecond() - sesja.start.getEpochSecond();
                if (sekundy > 0) {
                    UzytkownikService.dodajCzasWGrze(id, sekundy);
                }
                System.out.println("SesjaManager: timeout ID " + id
                        + " (brak refresh od " + odRefresh + "s), zapisano " + sekundy + "s");
            }
        }
    }

    // Zapisuje czas do ostatniego refresha dla wszystkich sesji — wywolywane przy shutdown.
    // NIE usuwa sesji z mapy (serwer moze byc zrestartowany, klient dostanie reconnect przy nastepnym refresh).
    public static void autoZapis() {
        if (aktywne.isEmpty()) return;
        System.out.println("SesjaManager: shutdown — zapisywanie " + aktywne.size() + " sesji...");
        for (Map.Entry<Integer, SesjaInfo> wpis : aktywne.entrySet()) {
            int id = wpis.getKey();
            SesjaInfo sesja = wpis.getValue();
            long sekundy = sesja.lastRefresh.getEpochSecond() - sesja.start.getEpochSecond();
            if (sekundy > 0) {
                UzytkownikService.dodajCzasWGrze(id, sekundy);
                System.out.println("SesjaManager: zapisano " + sekundy + "s dla gracza ID " + id);
            }
        }
    }

    // Uruchamia automatyczne sprawdzanie timeoutu co podana liczbe sekund.
    public static void uruchomSprawdzanie(long co) {
        scheduler.scheduleAtFixedRate(SesjaManager::sprawdzTimeout, co, co, TimeUnit.SECONDS);
        System.out.println("SesjaManager: sprawdzanie aktywne co " + co + "s (timeout=" + TIMEOUT_SEKUND + "s)");
    }

    public static int liczbaAktywnych() {
        return aktywne.size();
    }
}

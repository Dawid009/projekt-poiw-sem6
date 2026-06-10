package com.polsl.poiw.backend.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.polsl.poiw.backend.config.DatabaseConfig;
import com.polsl.poiw.backend.model.Punkty;

// Serwis do zarzadzania statystykami graczy.
// Tabela PUNKTY jest w relacji 1:1 z GRACZE â€” jeden wiersz na gracza, kumulowane statystyki.
public class PunktyService {

    private static final String SELECT_COLS =
            "SELECT id, nazwa, \"punkty\", \"iloscWejsc\", \"iloscZabitychwPrzeciwnikow\", " +
            "\"iloscZabitychwZwierzat\", \"iloscScietychDrzew\", " +
            "\"iloscZebranychSurowcow\", \"iloscZebranychPlonow\" FROM PUNKTY";

    // Tworzy wiersz statystyk dla nowo zarejestrowanego gracza (wszystkie liczniki = 0).
    public static boolean initPunktyGracza(int id, String nazwa) {
        String sql = "INSERT INTO PUNKTY (id, nazwa) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.setString(2, nazwa);
            pstmt.executeUpdate();
            System.out.println("Zainicjalizowano statystyki gracza: " + nazwa + " (ID: " + id + ")");
            return true;

        } catch (SQLException e) {
            System.err.println("Blad przy inicjalizacji statystyk gracza: " + e.getMessage());
            return false;
        }
    }

    // Dodaje (kumuluje) statystyki z jednej sesji gry do wiersza gracza.
    // Jesli wiersz nie istnieje (np. stare konto), tworzy go jako UPSERT.
    public static boolean addScore(int id, int punkty,
                                   int iloscWejsc, int iloscZabitychwPrzeciwnikow,
                                   int iloscZabitychwZwierzat, int iloscScietychDrzew,
                                   int iloscZebranychSurowcow, int iloscZebranychPlonow) {
        String sql =
            "INSERT INTO PUNKTY (id, nazwa, \"punkty\", \"iloscWejsc\", " +
            "\"iloscZabitychwPrzeciwnikow\", \"iloscZabitychwZwierzat\", " +
            "\"iloscScietychDrzew\", \"iloscZebranychSurowcow\", \"iloscZebranychPlonow\") " +
            "SELECT ?, g.nazwa, ?, ?, ?, ?, ?, ?, ? FROM GRACZE g WHERE g.id = ? " +
            "ON CONFLICT (id) DO UPDATE SET " +
            "\"punkty\" = PUNKTY.\"punkty\" + EXCLUDED.\"punkty\", " +
            "\"iloscWejsc\" = PUNKTY.\"iloscWejsc\" + EXCLUDED.\"iloscWejsc\", " +
            "\"iloscZabitychwPrzeciwnikow\" = PUNKTY.\"iloscZabitychwPrzeciwnikow\" + EXCLUDED.\"iloscZabitychwPrzeciwnikow\", " +
            "\"iloscZabitychwZwierzat\" = PUNKTY.\"iloscZabitychwZwierzat\" + EXCLUDED.\"iloscZabitychwZwierzat\", " +
            "\"iloscScietychDrzew\" = PUNKTY.\"iloscScietychDrzew\" + EXCLUDED.\"iloscScietychDrzew\", " +
            "\"iloscZebranychSurowcow\" = PUNKTY.\"iloscZebranychSurowcow\" + EXCLUDED.\"iloscZebranychSurowcow\", " +
            "\"iloscZebranychPlonow\" = PUNKTY.\"iloscZebranychPlonow\" + EXCLUDED.\"iloscZebranychPlonow\"";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            pstmt.setInt(2, punkty);
            pstmt.setInt(3, iloscWejsc);
            pstmt.setInt(4, iloscZabitychwPrzeciwnikow);
            pstmt.setInt(5, iloscZabitychwZwierzat);
            pstmt.setInt(6, iloscScietychDrzew);
            pstmt.setInt(7, iloscZebranychSurowcow);
            pstmt.setInt(8, iloscZebranychPlonow);
            pstmt.setInt(9, id);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Statystyki zaktualizowane: graczId=" + id + " +" + punkty + " pkt");
                return true;
            }
            System.err.println("Blad: nie znaleziono gracza ID " + id + " w tabeli GRACZE");
            return false;

        } catch (SQLException e) {
            System.err.println("Blad przy zapisywaniu statystyk: " + e.getMessage());
            return false;
        }
    }

    // Pobiera statystyki wszystkich graczy posortowane malejaco po punktach.
    public static List<Punkty> getAllScoresSorted() {
        List<Punkty> scores = new ArrayList<>();
        String sql = SELECT_COLS + " ORDER BY \"punkty\" DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                scores.add(mapRow(rs));
            }

        } catch (SQLException e) {
            System.err.println("Blad przy pobieraniu statystyk: " + e.getMessage());
        }

        return scores;
    }

    // Pobiera statystyki wszystkich graczy.
    public static List<Punkty> getAllScores() {
        return getAllScoresSorted();
    }

    // Pobiera TOP N graczy wedlug punktow — LIMIT w SQL.
    public static List<Punkty> getTopScores(int limit) {
        List<Punkty> scores = new ArrayList<>();
        String sql = SELECT_COLS + " ORDER BY \"punkty\" DESC LIMIT ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    scores.add(mapRow(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy pobieraniu TOP " + limit + " wynikow: " + e.getMessage());
        }

        return scores;
    }

    // Pobiera statystyki konkretnego gracza po nazwie.
    public static Punkty getStatsByPlayerName(String nazwa) {
        String sql = SELECT_COLS + " WHERE nazwa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nazwa);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy pobieraniu statystyk gracza: " + e.getMessage());
        }

        return null;
    }

    // Alias dla kompatybilnosci zwraca statystyki gracza jako liste (max 1 element).
    public static List<Punkty> getScoresByPlayer(String nazwa) {
        List<Punkty> wynik = new ArrayList<>();
        Punkty p = getStatsByPlayerName(nazwa);
        if (p != null) wynik.add(p);
        return wynik;
    }

    // Pobiera statystyki wszystkich graczy (alias dla getAllScoresSorted).
    public static List<Punkty> getAllPlayersStats() {
        return getAllScoresSorted();
    }

    // Pobiera liczbe graczy z wierszem w tabeli PUNKTY.
    public static long getScoreCount() {
        String sql = "SELECT COUNT(*) AS count FROM PUNKTY";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            if (rs.next()) {
                return rs.getLong("count");
            }

        } catch (SQLException e) {
            System.err.println("Blad przy liczeniu wierszy PUNKTY: " + e.getMessage());
        }

        return 0;
    }

    // Usuwa wiersz statystyk gracza.
    public static boolean deleteScore(int id) {
        String sql = "DELETE FROM PUNKTY WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Statystyki usuniete: graczId=" + id);
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Blad przy usuwaniu statystyk: " + e.getMessage());
            return false;
        }
    }

    // Aktualizuje liczbe punktow gracza (nadpisuje, nie kumuluje).
    public static boolean updateScore(int id, int punkty) {
        String sql = "UPDATE PUNKTY SET \"punkty\" = ? WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, punkty);
            pstmt.setInt(2, id);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                System.out.println("Punkty zaktualizowane: graczId=" + id + " -> " + punkty + " pkt");
                return true;
            }
            return false;

        } catch (SQLException e) {
            System.err.println("Blad przy aktualizacji punktow: " + e.getMessage());
            return false;
        }
    }

    // Mapuje wiersz ResultSet na obiekt Punkty.
    private static Punkty mapRow(ResultSet rs) throws SQLException {
        return new Punkty(
            rs.getInt("id"),
            rs.getString("nazwa"),
            rs.getInt("punkty"),
            rs.getInt("iloscWejsc"),
            rs.getInt("iloscZabitychwPrzeciwnikow"),
            rs.getInt("iloscZabitychwZwierzat"),
            rs.getInt("iloscScietychDrzew"),
            rs.getInt("iloscZebranychSurowcow"),
            rs.getInt("iloscZebranychPlonow")
        );
    }
}

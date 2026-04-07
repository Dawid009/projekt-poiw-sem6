package com.polsl.poiw.backend.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.polsl.poiw.backend.config.DatabaseConfig;
import com.polsl.poiw.backend.model.Punkty;

/**
 * Serwis do zarzadzania punktami gracza
 * Obsluguje wszystkie operacje CRUD na tabeli PUNKTY
 */
public class PunktyService {
    
    /**
     * Dodaje nowy wynik do bazy danych
     * @param nazwaGracza Nazwa gracza
     * @param punkty Liczba zdobytych punktow
     * @return true jesli sie powiodlo, false w przypadku bledu
     */
    public static boolean addScore(String nazwaGracza, int punkty) {
        String sql = "INSERT INTO PUNKTY (\"nazwaGracza\", \"punkty\") VALUES (?, ?)";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nazwaGracza);
            pstmt.setInt(2, punkty);
            int rowsInserted = pstmt.executeUpdate();
            
            if (rowsInserted > 0) {
                System.out.println("Wynik zapisany: " + nazwaGracza + " - " + punkty + " pkt");
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println(" Blad przy zapisywaniu wyniku: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Pobiera wszystkie wyniki z bazy
     * @return Lista wszystkich wynikow
     */
    public static List<Punkty> getAllScores() {
        List<Punkty> scores = new ArrayList<>();
        String sql = "SELECT id, \"nazwaGracza\", \"punkty\", \"dataUtworzenia\" FROM PUNKTY";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Punkty punkt = new Punkty(
                    rs.getInt("id"),
                    rs.getString("nazwaGracza"),
                    rs.getInt("punkty"),
                    rs.getTimestamp("dataUtworzenia").toString()
                );
                scores.add(punkt);
            }
            
        } catch (SQLException e) {
            System.err.println("Blad przy pobieraniu wynikow: " + e.getMessage());
        }
        
        return scores;
    }
    
    /**
     * Pobiera wyniki posortowane malejaco (najlepsze pierwsze)
     * @return Lista wynikow posortowana
     */
    public static List<Punkty> getAllScoresSorted() {
        List<Punkty> scores = new ArrayList<>();
        String sql = "SELECT id, \"nazwaGracza\", \"punkty\", \"dataUtworzenia\" FROM PUNKTY ORDER BY \"punkty\" DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                Punkty punkt = new Punkty(
                    rs.getInt("id"),
                    rs.getString("nazwaGracza"),
                    rs.getInt("punkty"),
                    rs.getTimestamp("dataUtworzenia").toString()
                );
                scores.add(punkt);
            }
            
        } catch (SQLException e) {
            System.err.println(" Blad przy pobieraniu wynikow: " + e.getMessage());
        }
        
        return scores;
    }
    
    /**
     * Pobiera TOP N najlepszych wynikow
     * @param limit Liczba wynikow do pobrania
     * @return Lista top wynikow
     */
    public static List<Punkty> getTopScores(int limit) {
        List<Punkty> scores = getAllScoresSorted();
        return scores.stream().limit(limit).toList();
    }
    
    /**
     * Pobiera wszystkie wyniki konkretnego gracza
     * @param nazwaGracza Nazwa gracza
     * @return Lista wynikow gracza
     */
    public static List<Punkty> getScoresByPlayer(String nazwaGracza) {
        List<Punkty> scores = new ArrayList<>();
        String sql = "SELECT id, \"nazwaGracza\", \"punkty\", \"dataUtworzenia\" FROM PUNKTY WHERE \"nazwaGracza\" = ? ORDER BY \"dataUtworzenia\" DESC";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nazwaGracza);            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Punkty punkt = new Punkty(
                        rs.getInt("id"),
                        rs.getString("nazwaGracza"),
                        rs.getInt("punkty"),
                        rs.getTimestamp("dataUtworzenia").toString()
                    );
                    scores.add(punkt);
                }
            }
            
        } catch (SQLException e) {
            System.err.println(" Blad przy pobieraniu wynikow gracza: " + e.getMessage());
        }
        
        return scores;
    }
    
    /**
     * Pobiera liczbe wszystkich wynikow
     * @return Liczba rekordow w bazie
     */
    public static long getScoreCount() {
        String sql = "SELECT COUNT(*) as count FROM PUNKTY";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getLong("count");
            }
            
        } catch (SQLException e) {
            System.err.println(" Blad przy liczeniu wynkow: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Usuwa wynik z bazy
     * @param id ID wyniku do usuniecia
     * @return true jesli sie powiodlo
     */
    public static boolean deleteScore(int id) {
        String sql = "DELETE FROM PUNKTY WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            
            if (rowsDeleted > 0) {
                System.out.println(" Wynik usuniety: ID " + id);
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println(" Blad przy usuwaniu wyniku: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Aktualizuje punkty dla wyniku
     * @param id ID wyniku
     * @param punkty Nowa liczba punktow
     * @return true jesli sie powiodlo
     */
    public static boolean updateScore(int id, int punkty) {
        String sql = "UPDATE PUNKTY SET \"punkty\" = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, punkty);
            pstmt.setInt(2, id);
            int rowsUpdated = pstmt.executeUpdate();
            
            if (rowsUpdated > 0) {
                System.out.println(" Wynik zaktualizowany: ID " + id + " -> " + punkty + " pkt");
                return true;
            }
            return false;
            
        } catch (SQLException e) {
            System.err.println(" Blad przy aktualizacji wyniku: " + e.getMessage());
            return false;
        }
    }
}

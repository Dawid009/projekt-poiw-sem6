package com.polsl.poiw.backend.service;

import com.polsl.poiw.backend.auth.PasswordHasher;
import com.polsl.poiw.backend.config.DatabaseConfig;
import com.polsl.poiw.backend.model.Gracz;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// Serwis do zarzadzania kontami graczy.
// Obsluguje rejestracje, logowanie i weryfikacje danych.
public class UzytkownikService {

    // Rejestruje nowe konto gracza.
    // Zwraca ID nowo utworzonego rekordu lub -1 w przypadku bledu.
    public static int zarejestruj(String email, String nazwa, String haslo) {
        if (!walidujEmail(email)) {
            System.err.println("Rejestracja nieudana: niepoprawny format emaila - " + email);
            return -1;
        }
        if (nazwa == null || nazwa.isBlank()) {
            System.err.println("Rejestracja nieudana: pusta nazwa gracza");
            return -1;
        }
        if (haslo == null || haslo.length() < 6) {
            System.err.println("Rejestracja nieudana: haslo za krotkie (min. 6 znakow)");
            return -1;
        }
        if (emailZajety(email)) {
            System.err.println("Rejestracja nieudana: email juz zajety - " + email);
            return -1;
        }
        if (nazwaZajeta(nazwa)) {
            System.err.println("Rejestracja nieudana: login juz zajety - " + nazwa);
            return -2;
        }

        String sol = PasswordHasher.generujSol();
        String skrotypowane = PasswordHasher.hashujHaslo(haslo, sol);

        String sql = "INSERT INTO GRACZE (email, nazwa, sol, haslo) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, email.trim().toLowerCase());
            pstmt.setString(2, nazwa.trim());
            pstmt.setString(3, sol);
            pstmt.setString(4, skrotypowane);
            pstmt.executeUpdate();

            try (ResultSet klucze = pstmt.getGeneratedKeys()) {
                if (klucze.next()) {
                    int id = klucze.getInt(1);
                    System.out.println("Zarejestrowano gracza: " + nazwa + " (" + email + ") - ID: " + id);
                    return id;
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy rejestracji gracza: " + e.getMessage());
        }

        return -1;
    }

    // Loguje gracza na podstawie loginu (nazwy) i hasla.
    // Zwraca obiekt Gracz jesli dane sa poprawne, lub null przy blednych danych.
    public static Gracz zaloguj(String login, String haslo) {
        if (login == null || haslo == null) {
            return null;
        }

        String sql = "SELECT id, email, nazwa, sol, haslo, \"czasWGrze\", \"dataRejestracji\" FROM GRACZE WHERE nazwa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, login.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String zapisanySol = rs.getString("sol");
                    String zapisanyHash = rs.getString("haslo");

                    if (PasswordHasher.weryfikujHaslo(haslo, zapisanySol, zapisanyHash)) {
                        Gracz gracz = new Gracz(
                            rs.getInt("id"),
                            rs.getString("email"),
                            rs.getString("nazwa"),
                            rs.getTimestamp("dataRejestracji").toString(),
                            rs.getLong("czasWGrze")
                        );
                        System.out.println("Zalogowano gracza: " + gracz.getNazwa() + " (ID: " + gracz.getId() + ", czas w grze: " + gracz.getCzasWGrze() + "s)");
                        return gracz;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy logowaniu gracza: " + e.getMessage());
        }

        System.out.println("Nieudana proba logowania dla loginu: " + login);
        return null;
    }

    // Dodaje sekundy do laczonego czasu w grze dla danego gracza.
    // Operacja atomowa — bezpieczna przy rownoczesnych sesjach.
    // Zwraca nowy laczny czas w sekundach lub -1 przy bledzie.
    public static long dodajCzasWGrze(int id, long sekundy) {
        if (sekundy <= 0) {
            return -1;
        }

        String sqlUpdate = "UPDATE GRACZE SET \"czasWGrze\" = \"czasWGrze\" + ? WHERE id = ?";
        String sqlSelect = "SELECT \"czasWGrze\" FROM GRACZE WHERE id = ?";

        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sqlUpdate)) {
                pstmt.setLong(1, sekundy);
                pstmt.setInt(2, id);
                int zaktualizowane = pstmt.executeUpdate();
                if (zaktualizowane == 0) {
                    System.err.println("Blad przy zapisie czasu: nie znaleziono gracza ID " + id);
                    return -1;
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sqlSelect)) {
                pstmt.setInt(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        long nowyczas = rs.getLong("czasWGrze");
                        System.out.println("Czas w grze zaktualizowany: gracz ID " + id + " -> " + nowyczas + "s (+" + sekundy + "s)");
                        return nowyczas;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy aktualizacji czasu w grze: " + e.getMessage());
        }

        return -1;
    }

    // Sprawdza czy podany email jest juz zajety w bazie.
    public static boolean emailZajety(String email) {
        String sql = "SELECT COUNT(*) FROM GRACZE WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy sprawdzaniu emaila: " + e.getMessage());
        }

        return false;
    }

    // Wyszukuje gracza po nazwie (loginie). Zwraca Gracz lub null jesli nie znaleziono.
    public static Gracz znajdzPoNazwie(String nazwa) {
        if (nazwa == null || nazwa.isBlank()) return null;

        String sql = "SELECT id, email, nazwa, \"czasWGrze\", \"dataRejestracji\" FROM GRACZE WHERE nazwa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nazwa.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Gracz(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("nazwa"),
                        rs.getTimestamp("dataRejestracji").toString(),
                        rs.getLong("czasWGrze")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy wyszukiwaniu gracza po nazwie: " + e.getMessage());
        }

        return null;
    }

    // Sprawdza czy podana nazwa gracza (login) jest juz zajeta w bazie.
    public static boolean nazwaZajeta(String nazwa) {
        String sql = "SELECT COUNT(*) FROM GRACZE WHERE nazwa = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, nazwa.trim());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy sprawdzaniu nazwy gracza: " + e.getMessage());
        }

        return false;
    }

    // Sprawdza poprawnosc formatu emaila.
    private static boolean walidujEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return false;
        }
        int dot = email.lastIndexOf('.');
        return dot > at + 1 && dot < email.length() - 1;
    }
}

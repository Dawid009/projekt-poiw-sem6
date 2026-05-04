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

    // Loguje gracza na podstawie emaila i hasla.
    // Zwraca obiekt Gracz jesli dane sa poprawne, lub null przy blednych danych.
    public static Gracz zaloguj(String email, String haslo) {
        if (email == null || haslo == null) {
            return null;
        }

        String sql = "SELECT id, email, nazwa, sol, haslo, \"dataRejestracji\" FROM GRACZE WHERE email = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email.trim().toLowerCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String zapisanySol = rs.getString("sol");
                    String zapisanyHash = rs.getString("haslo");

                    if (PasswordHasher.weryfikujHaslo(haslo, zapisanySol, zapisanyHash)) {
                        Gracz gracz = new Gracz(
                            rs.getInt("id"),
                            rs.getString("email"),
                            rs.getString("nazwa"),
                            rs.getTimestamp("dataRejestracji").toString()
                        );
                        System.out.println("Zalogowano gracza: " + gracz.getNazwa() + " (ID: " + gracz.getId() + ")");
                        return gracz;
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Blad przy logowaniu gracza: " + e.getMessage());
        }

        System.out.println("Nieudana proba logowania dla emaila: " + email);
        return null;
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

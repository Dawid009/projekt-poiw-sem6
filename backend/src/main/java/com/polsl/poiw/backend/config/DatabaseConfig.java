package com.polsl.poiw.backend.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final String DB_URL = "jdbc:h2:./database/game_db";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static void initializeDatabase() {
        // Przykładowy SQL tworzący tabelę wyników
        String createTableSQL = "CREATE TABLE IF NOT EXISTS PUNKTY (" +
                                "id IDENTITY PRIMARY KEY, " +
                                "\"nazwaGracza\" VARCHAR(255) NOT NULL, " +
                                "\"punkty\" INT DEFAULT 0, " +
                                "\"dataUtworzenia\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ");";

        try (Connection conn = getConnection(); 
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            System.out.println("Baza danych utworzona albo zaaktualizowana");
            
        } catch (SQLException e) {
            System.err.println("Błąd podczas inicjalizacji bazy" + e.getMessage());
            e.printStackTrace();
        }
    }
}
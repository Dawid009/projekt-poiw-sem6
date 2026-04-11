package com.polsl.poiw.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConfig {
    private static final Properties dbProperties = loadDatabaseProperties();

    private static Properties loadDatabaseProperties() {
        Properties props = new Properties();
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new RuntimeException("Nie mozna znalezc pliku db.properties w zasobach");
            }
            props.load(input);
            System.out.println("Zaladowano wlasciwosci bazy danych:");
            System.out.println("  URL: " + props.getProperty("db.url"));
            System.out.println("  Uzytkownik: " + props.getProperty("db.username"));
            System.out.println("  Haslo: " + (props.getProperty("db.password") != null ? "***" : "null"));
        } catch (IOException e) {
            throw new RuntimeException("Blad podczas ladowania wlasciwosci bazy danych", e);
        }
        return props;
    }

    private static final String DB_URL = dbProperties.getProperty("db.url");
    private static final String DB_USER = dbProperties.getProperty("db.username");
    private static final String DB_PASS = dbProperties.getProperty("db.password");

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static void initializeDatabase() {
        // Przykładowy SQL tworzący tabelę wyników
        String createTableSQL = "CREATE TABLE IF NOT EXISTS PUNKTY (" +
                                "id SERIAL PRIMARY KEY, " +
                                "\"nazwaGracza\" VARCHAR(255) NOT NULL, " +
                                "\"punkty\" INT DEFAULT 0, " +
                                "\"dataUtworzenia\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                ");";

        try (Connection conn = getConnection(); 
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(createTableSQL);
            System.out.println("Baza danych utworzona albo zaktualizowana");
            
        } catch (SQLException e) {
            System.err.println("Blad podczas inicjalizacji bazy" + e.getMessage());
            e.printStackTrace();
        }
    }
}
package com.polsl.poiw.backend.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConfig {
    private static final String DB_CONFIG_FILE = "/db.properties";
    private static final String DEFAULT_DB_URL = "jdbc:postgresql://localhost:5432/poiw";
    private static final String DEFAULT_DB_USER = "poiwuser1";
    private static final String DEFAULT_DB_PASS = "password";

    private static String DB_URL = DEFAULT_DB_URL;
    private static String DB_USER = DEFAULT_DB_USER;
    private static String DB_PASS = DEFAULT_DB_PASS;

    static {
        Properties properties = new Properties();
        try (InputStream stream = DatabaseConfig.class.getResourceAsStream(DB_CONFIG_FILE)) {
            if (stream != null) {
                properties.load(stream);
                DB_URL = properties.getProperty("db.url", DEFAULT_DB_URL);
                DB_USER = properties.getProperty("db.user", DEFAULT_DB_USER);
                DB_PASS = properties.getProperty("db.password", DEFAULT_DB_PASS);
            } else {
                System.out.println("Plik konfiguracyjny db.properties nie znaleziony. Używane wartości domyślne.");
            }
        } catch (IOException e) {
            System.err.println("Nie udało się wczytać pliku db.properties: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static void initializeDatabase() {
        // Tabela kont graczy — email unikalny, haslo hashowane SHA-256 z sola
        String createGraczeSQL = "CREATE TABLE IF NOT EXISTS GRACZE (" +
                                 "id SERIAL PRIMARY KEY, " +
                                 "email VARCHAR(255) NOT NULL UNIQUE, " +
                                 "nazwa VARCHAR(100) NOT NULL, " +
                                 "sol VARCHAR(64) NOT NULL, " +
                                 "haslo VARCHAR(64) NOT NULL, " +
                                 "\"czasWGrze\" BIGINT DEFAULT 0, " +
                                 "\"dataRejestracji\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                                 ");";

       

        // Migracja: unikalna nazwa gracza (login) — warunek dla logowania po loginie
        String migrujUnikatNazwySQL = "DO $$ BEGIN " +
            "IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'gracze_nazwa_unique') THEN " +
            "ALTER TABLE GRACZE ADD CONSTRAINT gracze_nazwa_unique UNIQUE (nazwa); " +
            "END IF; END $$;";

       
        // Tabela statystyk — relacja 1:1 z GRACZE (id = id gracza)
        String createPunktySQL = "CREATE TABLE IF NOT EXISTS PUNKTY (" +
                                 "id INT PRIMARY KEY REFERENCES GRACZE(id), " +
                                 "nazwa VARCHAR(100) NOT NULL, " +
                                 "\"punkty\" INT DEFAULT 0, " +
                                 "\"iloscWejsc\" INT DEFAULT 0, " +
                                 "\"iloscZabitychwPrzeciwnikow\" INT DEFAULT 0, " +
                                 "\"iloscZabitychwZwierzat\" INT DEFAULT 0, " +
                                 "\"iloscScietychDrzew\" INT DEFAULT 0, " +
                                 "\"iloscZebranychSurowcow\" INT DEFAULT 0, " +
                                 "\"iloscZebranychPlonow\" INT DEFAULT 0" +
                                 ");";

        // Inicjalizacja brakujacych wierszy PUNKTY dla juz istniejacych graczy
        String initBrakujacychPunktySQL = "INSERT INTO PUNKTY (id, nazwa) " +
            "SELECT g.id, g.nazwa FROM GRACZE g " +
            "WHERE NOT EXISTS (SELECT 1 FROM PUNKTY p WHERE p.id = g.id);";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createGraczeSQL);
            try {
                stmt.execute(migrujUnikatNazwySQL);
            } catch (SQLException eMigr) {
                System.err.println("Migracja unique nazwy: " + eMigr.getMessage());
            }
            
            stmt.execute(createPunktySQL);           // tworzy nowa PUNKTY (1:1 z GRACZE)
            stmt.execute(initBrakujacychPunktySQL);  // uzupelnia wiersze dla istniejacych graczy

            System.out.println("Baza danych utworzona albo zaaktualizowana");

        } catch (SQLException e) {
            System.err.println("Błąd podczas inicjalizacji bazy: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
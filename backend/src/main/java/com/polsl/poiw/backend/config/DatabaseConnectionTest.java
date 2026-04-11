package com.polsl.poiw.backend.config;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnectionTest {
    public static void main(String[] args) {
        System.out.println("test polaczenia z baza danych");

        try {
            Connection conn = DatabaseConfig.getConnection();
            System.out.println("polaczono z baza danych");
            System.out.println("URL bazy danych: " + conn.getMetaData().getURL());
            System.out.println("uzytkownik bazy danych: " + conn.getMetaData().getUserName());
            conn.close();
            System.out.println("polaczenie zamkniete pomyslnie");
        } catch (SQLException e) {
            System.err.println(" blad: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
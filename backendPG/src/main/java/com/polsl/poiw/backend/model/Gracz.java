package com.polsl.poiw.backend.model;

// Model gracza - reprezentuje konto uzytkownika zarejestrowanego w systemie.
public class Gracz {
    private int id;
    private String email;
    private String nazwa;
    private String dataRejestracji;

    public Gracz(int id, String email, String nazwa, String dataRejestracji) {
        this.id = id;
        this.email = email;
        this.nazwa = nazwa;
        this.dataRejestracji = dataRejestracji;
    }

    public Gracz(String email, String nazwa) {
        this.email = email;
        this.nazwa = nazwa;
    }

    // Gettery i Settery

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }

    public String getDataRejestracji() {
        return dataRejestracji;
    }

    public void setDataRejestracji(String dataRejestracji) {
        this.dataRejestracji = dataRejestracji;
    }

    @Override
    public String toString() {
        return String.format("[ID: %d] %s (%s) - data rej.: %s",
                id, nazwa, email, dataRejestracji);
    }
}

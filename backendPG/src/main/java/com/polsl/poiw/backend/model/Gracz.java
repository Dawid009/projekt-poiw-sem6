package com.polsl.poiw.backend.model;

// Model gracza - reprezentuje konto uzytkownika zarejestrowanego w systemie.
public class Gracz {
    private int id;
    private String email;
    private String nazwa;
    private String dataRejestracji;
    // Laczny czas spedzony w grze w sekundach (akumulowany miedzy sesjami)
    private long czasWGrze;

    public Gracz(int id, String email, String nazwa, String dataRejestracji, long czasWGrze) {
        this.id = id;
        this.email = email;
        this.nazwa = nazwa;
        this.dataRejestracji = dataRejestracji;
        this.czasWGrze = czasWGrze;
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

    public long getCzasWGrze() {
        return czasWGrze;
    }

    public void setCzasWGrze(long czasWGrze) {
        this.czasWGrze = czasWGrze;
    }

    @Override
    public String toString() {
        long godz = czasWGrze / 3600;
        long min = (czasWGrze % 3600) / 60;
        long sek = czasWGrze % 60;
        return String.format("[ID: %d] %s (%s) - czas w grze: %02d:%02d:%02d - data rej.: %s",
                id, nazwa, email, godz, min, sek, dataRejestracji);
    }
}

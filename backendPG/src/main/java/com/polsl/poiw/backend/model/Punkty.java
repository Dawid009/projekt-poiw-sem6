package com.polsl.poiw.backend.model;

// Model statystyk gracza â€” relacja 1:1 z tabela GRACZE.
// id i nazwa sa identyczne z odpowiadajacym rekordem w GRACZE.
public class Punkty {
    // id gracza (klucz glowny = klucz obcy do GRACZE)
    private int id;
    // nazwa gracza (zdublowana z GRACZE dla szybkiego odczytu)
    private String nazwa;
    private int punkty;
    private int iloscWejsc;
    private int iloscZabitychwPrzeciwnikow;
    private int iloscZabitychwZwierzat;
    private int iloscScietychDrzew;
    private int iloscZebranychSurowcow;
    private int iloscZebranychPlonow;

    // Konstruktor pelny â€” uzywany przy odczycie z bazy
    public Punkty(int id, String nazwa, int punkty,
                  int iloscWejsc, int iloscZabitychwPrzeciwnikow, int iloscZabitychwZwierzat,
                  int iloscScietychDrzew, int iloscZebranychSurowcow, int iloscZebranychPlonow) {
        this.id = id;
        this.nazwa = nazwa;
        this.punkty = punkty;
        this.iloscWejsc = iloscWejsc;
        this.iloscZabitychwPrzeciwnikow = iloscZabitychwPrzeciwnikow;
        this.iloscZabitychwZwierzat = iloscZabitychwZwierzat;
        this.iloscScietychDrzew = iloscScietychDrzew;
        this.iloscZebranychSurowcow = iloscZebranychSurowcow;
        this.iloscZebranychPlonow = iloscZebranychPlonow;
    }

    // Konstruktor inicjalny â€” uzywany przy tworzeniu wiersza po rejestracji gracza
    public Punkty(int id, String nazwa) {
        this.id = id;
        this.nazwa = nazwa;
    }

    // Gettery i Settery

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNazwa() {
        return nazwa;
    }

    public void setNazwa(String nazwa) {
        this.nazwa = nazwa;
    }

    public int getPunkty() {
        return punkty;
    }

    public void setPunkty(int punkty) {
        this.punkty = punkty;
    }

    public int getIloscWejsc() {
        return iloscWejsc;
    }

    public void setIloscWejsc(int iloscWejsc) {
        this.iloscWejsc = iloscWejsc;
    }

    public int getIloscZabitychwPrzeciwnikow() {
        return iloscZabitychwPrzeciwnikow;
    }

    public void setIloscZabitychwPrzeciwnikow(int iloscZabitychwPrzeciwnikow) {
        this.iloscZabitychwPrzeciwnikow = iloscZabitychwPrzeciwnikow;
    }

    public int getIloscZabitychwZwierzat() {
        return iloscZabitychwZwierzat;
    }

    public void setIloscZabitychwZwierzat(int iloscZabitychwZwierzat) {
        this.iloscZabitychwZwierzat = iloscZabitychwZwierzat;
    }

    public int getIloscScietychDrzew() {
        return iloscScietychDrzew;
    }

    public void setIloscScietychDrzew(int iloscScietychDrzew) {
        this.iloscScietychDrzew = iloscScietychDrzew;
    }

    public int getIloscZebranychSurowcow() {
        return iloscZebranychSurowcow;
    }

    public void setIloscZebranychSurowcow(int iloscZebranychSurowcow) {
        this.iloscZebranychSurowcow = iloscZebranychSurowcow;
    }

    public int getIloscZebranychPlonow() {
        return iloscZebranychPlonow;
    }

    public void setIloscZebranychPlonow(int iloscZebranychPlonow) {
        this.iloscZebranychPlonow = iloscZebranychPlonow;
    }

    @Override
    public String toString() {
        return String.format(
                "[ID: %d] %s -> %d pkt | wejscia: %d, przeciwnicy: %d, zwierzeta: %d, " +
                "drzewa: %d, surowce: %d, plony: %d",
                id, nazwa, punkty, iloscWejsc, iloscZabitychwPrzeciwnikow, iloscZabitychwZwierzat,
                iloscScietychDrzew, iloscZebranychSurowcow, iloscZebranychPlonow);
    }
}

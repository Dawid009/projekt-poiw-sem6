package com.polsl.poiw.backend.model;

/**
 * Model wynikó gry - reprezentuje pojedynczy wynik gracza
 */
public class Punkty {
    private int id;
    private String nazwaGracza;
    private int punkty;
    private String createdAt;
    
    public Punkty(int id, String nazwaGracza, int punkty, String createdAt) {
        this.id = id;
        this.nazwaGracza = nazwaGracza;
        this.punkty = punkty;
        this.createdAt = createdAt;
    }
    
    public Punkty(String nazwaGracza, int punkty) {
        this.nazwaGracza = nazwaGracza;
        this.punkty = punkty;
    }
    
    // Gettery i Settery
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public String getNazwaGracza() {
        return nazwaGracza;
    }
    
    public void setNazwaGracza(String nazwaGracza) {
        this.nazwaGracza = nazwaGracza;
    }
    
    public int getPunkty() {
        return punkty;
    }
    
    public void setPunkty(int punkty) {
        this.punkty = punkty;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return String.format("[ID: %d] %s -> %d pkt (data: %s)", 
                id, nazwaGracza, punkty, createdAt);
    }
}

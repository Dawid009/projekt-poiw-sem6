package com.polsl.poiw.backend.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

// Narzedzie do hashowania hasel — SHA-256 z losowa sola.
// Sol i hash przechowywane sa jako ciagi hexadecymalne.
public class PasswordHasher {

    private static final int SOL_DLUGOSC_BAJTOW = 32; // 32 bajty = 256 bitow soli
    private static final String ALGORYTM = "SHA-256";

    private PasswordHasher() {
    }

    // Generuje kryptograficznie bezpieczna losowa sol (64 znaki hex).
    public static String generujSol() {
        byte[] sol = new byte[SOL_DLUGOSC_BAJTOW];
        new SecureRandom().nextBytes(sol);
        return HexFormat.of().formatHex(sol);
    }

    // Hashuje haslo polaczone z sola algorytmem SHA-256.
    // Zwraca hash jako 64-znakowy ciag hexadecymalny.
    public static String hashujHaslo(String haslo, String sol) {
        try {
            MessageDigest md = MessageDigest.getInstance(ALGORYTM);
            md.update(HexFormat.of().parseHex(sol));
            byte[] hash = md.digest(haslo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorytm " + ALGORYTM + " niedostepny", e);
        }
    }

    // Weryfikuje haslo przez porownanie z zapisanym hashem i sola.
    // Zwraca true jesli haslo jest poprawne.
    public static boolean weryfikujHaslo(String haslo, String sol, String zapisanyHash) {
        String hash = hashujHaslo(haslo, sol);
        return hash.equals(zapisanyHash);
    }
}

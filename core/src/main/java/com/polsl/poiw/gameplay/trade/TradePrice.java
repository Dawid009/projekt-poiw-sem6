package com.polsl.poiw.gameplay.trade;

/**
 * Prosta reprezentacja ceny w trzech nominałach monet.
 * Używana przez oferty handlowe i logikę kupna/sprzedaży.
 */
public record TradePrice(int goldCoins, int silverCoins, int bronzeCoins) {
    public static final int GOLD_TO_BRONZE = 100;
    public static final int SILVER_TO_BRONZE = 10;

    public static final TradePrice ZERO = new TradePrice(0, 0, 0);

    public TradePrice {
        goldCoins = Math.max(0, goldCoins);
        silverCoins = Math.max(0, silverCoins);
        bronzeCoins = Math.max(0, bronzeCoins);
    }

    /** Zwraca cenę przeliczoną na najniższy nominał. */
    public int toBronzeValue() {
        return goldCoins * GOLD_TO_BRONZE + silverCoins * SILVER_TO_BRONZE + bronzeCoins;
    }

    /** Sprawdza, czy cena jest równa zero. */
    public boolean isZero() {
        return goldCoins <= 0 && silverCoins <= 0 && bronzeCoins <= 0;
    }

    /** Zwraca skrócony zapis ceny używany w UI handlu. */
    public String toDisplayString() {
        if (isZero()) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();
        if (goldCoins > 0) {
            builder.append(goldCoins).append(" zl");
        }
        if (silverCoins > 0) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(silverCoins).append(" sr");
        }
        if (bronzeCoins > 0) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(bronzeCoins).append(" br");
        }
        return builder.toString();
    }

    /** Tworzy nową cenę bezpośrednio z liczby monet każdego typu. */
    public static TradePrice of(int goldCoins, int silverCoins, int bronzeCoins) {
        return new TradePrice(goldCoins, silverCoins, bronzeCoins);
    }

    /** Rozbija wartość w miedziakach na złote, srebrne i brązowe monety. */
    public static TradePrice fromBronzeValue(int bronzeValue) {
        int normalized = Math.max(0, bronzeValue);
        int gold = normalized / GOLD_TO_BRONZE;
        normalized %= GOLD_TO_BRONZE;
        int silver = normalized / SILVER_TO_BRONZE;
        normalized %= SILVER_TO_BRONZE;
        return new TradePrice(gold, silver, normalized);
    }
}

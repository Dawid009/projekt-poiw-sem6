package com.polsl.poiw.gameplay.trade;

public record TradePrice(int goldCoins, int silverCoins, int bronzeCoins) {
    public static final int GOLD_TO_BRONZE = 100;
    public static final int SILVER_TO_BRONZE = 10;

    public static final TradePrice ZERO = new TradePrice(0, 0, 0);

    public TradePrice {
        goldCoins = Math.max(0, goldCoins);
        silverCoins = Math.max(0, silverCoins);
        bronzeCoins = Math.max(0, bronzeCoins);
    }

    public int toBronzeValue() {
        return goldCoins * GOLD_TO_BRONZE + silverCoins * SILVER_TO_BRONZE + bronzeCoins;
    }

    public boolean isZero() {
        return goldCoins <= 0 && silverCoins <= 0 && bronzeCoins <= 0;
    }

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

    public static TradePrice of(int goldCoins, int silverCoins, int bronzeCoins) {
        return new TradePrice(goldCoins, silverCoins, bronzeCoins);
    }

    public static TradePrice fromBronzeValue(int bronzeValue) {
        int normalized = Math.max(0, bronzeValue);
        int gold = normalized / GOLD_TO_BRONZE;
        normalized %= GOLD_TO_BRONZE;
        int silver = normalized / SILVER_TO_BRONZE;
        normalized %= SILVER_TO_BRONZE;
        return new TradePrice(gold, silver, normalized);
    }
}

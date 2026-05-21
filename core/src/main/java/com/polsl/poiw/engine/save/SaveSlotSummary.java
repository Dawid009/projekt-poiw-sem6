package com.polsl.poiw.engine.save;

/**
 * Krótkie informacje o slocie używane na ekranie wyboru zapisu.
 */
public record SaveSlotSummary(
    int slotIndex,
    boolean occupied,
    float totalPlayTimeSeconds,
    long lastPlayedEpochMillis
) {
}
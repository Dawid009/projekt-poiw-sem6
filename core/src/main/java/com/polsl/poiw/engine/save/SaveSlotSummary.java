package com.polsl.poiw.engine.save;

public record SaveSlotSummary(
    int slotIndex,
    boolean occupied,
    float totalPlayTimeSeconds,
    long lastPlayedEpochMillis
) {
}
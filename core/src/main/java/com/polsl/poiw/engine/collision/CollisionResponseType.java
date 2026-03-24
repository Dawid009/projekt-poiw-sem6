package com.polsl.poiw.engine.collision;

/** Podstawowe Response na kolizje */
public enum CollisionResponseType {
    IGNORE,  //Ignoruje
    OVERLAP, //Overlap - wywołuje eventy ale nie blokuje fizycznie
    BLOCK //Block - blokuje fizycznie
}

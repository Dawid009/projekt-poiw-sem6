package com.polsl.poiw.engine.collision;

/** Kanały kolizji - służą ro rozpoznawania kolizji między obiektami.
 * Można dodać nowe jeśli będą potrzebne
 */

public enum CollisionChannel {
    DEFAULT,        // Domyślny kanał (wszystko koliduje ze wszystkim)
    PLAYER,         // Gracze
    ENEMY,          // Wrogowie
    PROJECTILE,     // Pociski
    TRIGGER,        // Triggery (zmiana mapy, cutscena, area damage)
    ITEM,           // Itemy na ziemi (do zebrania)
    ENVIRONMENT     // Ściany, podłoga, przeszkody (z Tiled — warstwa "collision")
}

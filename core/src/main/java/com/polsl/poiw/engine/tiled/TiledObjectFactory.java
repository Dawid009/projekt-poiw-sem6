package com.polsl.poiw.engine.tiled;

import com.badlogic.gdx.maps.MapObject;
import com.polsl.poiw.engine.actor.Actor;

/**
 * Kontrakt między parserem mapy a gameplayem.
 * Implementacja tłumaczy obiekt z Tiled na konkretnego aktora gry.
 */
@FunctionalInterface
public interface TiledObjectFactory {

    /**
     * Tworzy Actora na podstawie obiektu z mapy Tiled.
     * Implementacja może od razu zespawnować aktora w świecie albo zwrócić `null`.
     *
     * @param type wartość "type" z Custom Properties obiektu w Tiled
     * @param mapObject obiekt z LibGDX TiledMap (pozycja, properties)
     * @return nowy Actor (lub null jeśli typ nieobsługiwany)
     */
    Actor createFromMapObject(String type, MapObject mapObject);
}

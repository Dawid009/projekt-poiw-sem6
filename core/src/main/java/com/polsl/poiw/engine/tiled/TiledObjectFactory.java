package com.polsl.poiw.engine.tiled;

import com.badlogic.gdx.maps.MapObject;
import com.polsl.poiw.engine.actor.Actor;

/**
 * INTERFEJS: Fabryka tworząca Actorów z obiektów Tiled.
 */
@FunctionalInterface
public interface TiledObjectFactory {

    /**
     * Tworzy Actora na podstawie obiektu z mapy Tiled.
     *
     * @param type wartość "type" z Custom Properties obiektu w Tiled
     * @param mapObject obiekt z LibGDX TiledMap (pozycja, properties)
     * @return nowy Actor (lub null jeśli typ nieobsługiwany)
     */
    Actor createFromMapObject(String type, MapObject mapObject);
}

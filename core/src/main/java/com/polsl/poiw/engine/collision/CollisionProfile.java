package com.polsl.poiw.engine.collision;

import java.util.EnumMap;
import java.util.Map;

public class CollisionProfile {

    /** Do jakiego kanału należy ten obiekt */
    private final CollisionChannel objectType;

    /** Mapa: kanał → jak reagujemy */
    private final Map<CollisionChannel, CollisionResponseType> responses;

    public CollisionProfile(CollisionChannel objectType) {
        this.objectType = objectType;
        this.responses = new EnumMap<>(CollisionChannel.class);
        // Domyślnie: wszystko BLOCK
        for (CollisionChannel ch : CollisionChannel.values()) {
            responses.put(ch, CollisionResponseType.BLOCK);
        }
    }

    /** Ustawia odpowiedź na dany kanał */
    public CollisionProfile setResponse(CollisionChannel channel, CollisionResponseType response) {
        responses.put(channel, response);
        return this;
    }

    public CollisionResponseType getResponseTo(CollisionChannel other) {
        return responses.getOrDefault(other, CollisionResponseType.BLOCK);
    }

    public CollisionChannel getObjectType() { return objectType; }

    // ===== PREDEFINIOWANE PROFILE =====

    /** Gracz: blokuje ściany i wrogów, overlap z triggerami i itemami, przenika graczy */
    public static final CollisionProfile PLAYER = new CollisionProfile(CollisionChannel.PLAYER)
        .setResponse(CollisionChannel.ENEMY, CollisionResponseType.BLOCK)
        .setResponse(CollisionChannel.ENVIRONMENT, CollisionResponseType.BLOCK)
        .setResponse(CollisionChannel.TRIGGER, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.ITEM, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.PLAYER, CollisionResponseType.OVERLAP);

    /** Środowisko (ściany, domy, pnie drzew): blokuje gracza/wroga/pociski, ignoruje triggery/itemy */
    public static final CollisionProfile ENVIRONMENT = new CollisionProfile(CollisionChannel.ENVIRONMENT)
        .setResponse(CollisionChannel.PLAYER, CollisionResponseType.BLOCK)
        .setResponse(CollisionChannel.ENEMY, CollisionResponseType.BLOCK)
        .setResponse(CollisionChannel.PROJECTILE, CollisionResponseType.BLOCK)
        .setResponse(CollisionChannel.TRIGGER, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.ITEM, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.ENVIRONMENT, CollisionResponseType.IGNORE);

    /** Trigger (pułapki, strefy zmiany mapy): overlap ze wszystkim, nie blokuje fizycznie */
    public static final CollisionProfile TRIGGER = new CollisionProfile(CollisionChannel.TRIGGER)
        .setResponse(CollisionChannel.PLAYER, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.ENEMY, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.PROJECTILE, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.ENVIRONMENT, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.TRIGGER, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.ITEM, CollisionResponseType.IGNORE);

    /** Item (przedmioty na ziemi): overlap z graczem, ignoruje resztę */
    public static final CollisionProfile ITEM = new CollisionProfile(CollisionChannel.ITEM)
        .setResponse(CollisionChannel.PLAYER, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.ENEMY, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.PROJECTILE, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.ENVIRONMENT, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.TRIGGER, CollisionResponseType.IGNORE)
        .setResponse(CollisionChannel.ITEM, CollisionResponseType.IGNORE);


}

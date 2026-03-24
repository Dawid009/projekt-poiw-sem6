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

    // ===== TUTAJ UTWORZYć PREDEFINIOWANE PROFILE  =====

    /** Np dla gracza, później inne.*/
    public static final CollisionProfile PLAYER = new CollisionProfile(CollisionChannel.PLAYER)
        .setResponse(CollisionChannel.ENEMY, CollisionResponseType.BLOCK)
        .setResponse(CollisionChannel.ENVIRONMENT, CollisionResponseType.BLOCK)
        .setResponse(CollisionChannel.TRIGGER, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.ITEM, CollisionResponseType.OVERLAP)
        .setResponse(CollisionChannel.PLAYER, CollisionResponseType.OVERLAP); // gracze przenikają się


}

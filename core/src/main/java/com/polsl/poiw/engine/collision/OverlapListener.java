package com.polsl.poiw.engine.collision;

import com.polsl.poiw.engine.actor.Actor;

public interface OverlapListener {

    /**
     * Wywoływane gdy inny Actor zaczyna przenikać przez kształt kolizji.
     * @param self nasz Actor (na którym jest ten listener)
     * @param other Actor, który w nas wszedł
     * @param result szczegóły kolizji (punkt kontaktu, normalna)
     */
    void onBeginOverlap(Actor self, Actor other, CollisionResult result);

    /**
     * Wywoływane gdy inny Actor przestaje przenikać przez nasz kształt kolizji.
     */
    void onEndOverlap(Actor self, Actor other);
}

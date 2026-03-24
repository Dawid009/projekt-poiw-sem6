package com.polsl.poiw.engine.actor;


/** Podstawowy interfejs określający kształt komponentu.
 * Komponent - fragment logiki który można przypiąć danemu aktorowi
 */
public interface ActorComponent extends com.badlogic.ashley.core.Component {

    /** Właściciel (Actor) tego komponentu */
    Actor getOwner();
    void setOwner(Actor owner);

    /** Wywoływane po dodaniu komponentu do Actora i po beginPlay() Actora */
    void initialize();

    /** Wywoływane przed usunięciem komponentu lub przed endPlay() Actora */
    void dispose();

    /** Wywoływane co klatkę (delta = czas od ostatniej klatki w sekundach) */
    void tick(float delta);

    /**
     * Czy ten komponent jest replikowany przez sieć?
     * P2 (Network) sprawdza tę flagę i jeśli true — synchronizuje dane.
     */
    boolean isReplicated();
}

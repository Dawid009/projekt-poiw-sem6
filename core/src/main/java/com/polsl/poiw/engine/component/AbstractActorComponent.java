package com.polsl.poiw.engine.component;

import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.ActorComponent;

public abstract class AbstractActorComponent implements ActorComponent {

    // Referencja do Actora, który posiada ten komponent
    private Actor owner;

    // Czy ten komponent jest replikowany przez sieć
    private boolean replicated = false;

    @Override
    public Actor getOwner() { return owner; }

    @Override
    public void setOwner(Actor owner) { this.owner = owner; }

    @Override
    public void initialize() {
        // Domyślnie puste — subklasy override'ują w razie potrzeby
    }

    @Override
    public void dispose() {
        // Domyślnie puste — subklasy override'ują (np. usunięcie Box2D body)
    }

    @Override
    public void tick(float delta) {
        // Domyślnie puste — subklasy override'ują
    }

    @Override
    public boolean isReplicated() { return replicated; }

    /** Ustawia flagę replikacji */
    protected void setReplicated(boolean replicated) {
        this.replicated = replicated;
    }

    /**
     * Oznacza property jako "zmienione",
     * że musi wysłać tę wartość do klientów.
     *
     * Wywoływaj to PO każdej zmianie @Replicated pola:
     *   this.hp -= damage;
     *   markDirty("hp");
     *
     * @param propertyName nazwa pola (musi pasować do @Replicated field name)
     */
    protected void markDirty(String propertyName) {
        // Logika dirty-tracking zostanie dodana później.
    }
}

package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.collision.CollisionResult;
import com.polsl.poiw.engine.collision.OverlapListener;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.gameplay.character.PlayerCharacter;

import java.util.HashSet;
import java.util.Set;

/**
 * Trigger — niewidoczna strefa z mapy Tiled (warstwa "trigger").
 * Obsługuje obrażenia — jeśli gracz stoi w strefie, traci HP co sekundę.
 */
public class TriggerActor extends AbstractActor implements OverlapListener {

    private String triggerName;

    /** Obrażenia zadawane graczowi na sekundę (0 = brak obrażeń) */
    private float damagePerSecond = 1f;

    /** Aktory aktualnie w strefie triggera */
    private final Set<Actor> overlappingActors = new HashSet<>();

    /**
     * Konfiguruje trigger z danymi z Tiled.
     *
     * @param name      nazwa triggera (z Tiled, np. "trap_trigger")
     * @param halfW     połowa szerokości strefy w metrach
     * @param halfH     połowa wysokości strefy w metrach
     */
    public void configure(String name, float halfW, float halfH) {
        this.triggerName = name;

        // TransformComponent — single source of truth dla pozycji Actora.
        // Pozycja startowa ustawiana przez GameWorld.spawnActor() → Actor.setPosition().
        addComponent(new TransformComponent(
            new Vector2(),
            0,
            new Vector2(halfW * 2f, halfH * 2f)
        ));

        // Kolizja — sensor, nie blokuje ruchu
        BoxCollisionComponent collision = new BoxCollisionComponent(
            CollisionProfile.TRIGGER, halfW, halfH
        );
        collision.addOverlapListener(this);
        addComponent(collision);
    }

    @Override
    public void beginPlay() {
        super.beginPlay();
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        // Zadawaj obrażenia graczom przebywającym w strefie
        if (damagePerSecond > 0f) {
            for (Actor actor : overlappingActors) {
                if (actor instanceof PlayerCharacter player && player.isAlive()) {
                    player.applyDamage(damagePerSecond * delta);
                }
            }
        }
    }

    // ===== Overlap Events =====

    @Override
    public void onBeginOverlap(Actor self, Actor other, CollisionResult result) {
        Gdx.app.debug("TriggerActor",
            "Trigger '" + triggerName + "' activated by Actor #" + other.getActorId());
        overlappingActors.add(other);
    }

    @Override
    public void onEndOverlap(Actor self, Actor other) {
        Gdx.app.debug("TriggerActor",
            "Trigger '" + triggerName + "' deactivated by Actor #" + other.getActorId());
        overlappingActors.remove(other);
    }

    // ===== Konfiguracja =====

    public void setDamagePerSecond(float dps) { this.damagePerSecond = dps; }
    public float getDamagePerSecond() { return damagePerSecond; }

    public String getTriggerName() { return triggerName; }
}

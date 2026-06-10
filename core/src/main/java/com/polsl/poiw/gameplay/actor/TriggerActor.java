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

/** Niewidoczna strefa z mapy, np. pułapka albo obszar zdarzenia. */
public class TriggerActor extends AbstractActor implements OverlapListener {

    private String triggerName;

    private float damagePerSecond = 0f;
    private final Set<Actor> overlappingActors = new HashSet<>();
    public void configure(String name, float halfW, float halfH) {
        this.triggerName = name;

        addComponent(new TransformComponent(
            new Vector2(),
            0,
            new Vector2(halfW * 2f, halfH * 2f)
        ));

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
        if (!hasAuthority()) return;

        if (damagePerSecond > 0f) {
            for (Actor actor : overlappingActors) {
                if (actor instanceof PlayerCharacter player && player.isAlive()) {
                    player.applyDamage(damagePerSecond * delta);
                }
            }
        }
    }

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

    public void setDamagePerSecond(float dps) { this.damagePerSecond = dps; }
    public float getDamagePerSecond() { return damagePerSecond; }

    public String getTriggerName() { return triggerName; }
}

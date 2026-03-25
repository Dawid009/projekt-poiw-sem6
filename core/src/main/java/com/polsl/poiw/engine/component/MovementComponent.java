package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

/**
 * Komponent ruchu — przechowuje prędkość maksymalną i kierunek.
 * isRooted = true → Actor nie może się ruszać (np. podczas ataku, stun).
 */
public class MovementComponent extends AbstractActorComponent {
    public static final ComponentMapper<MovementComponent> MAPPER = ComponentMapper.getFor(MovementComponent.class);

    private float maxSpeed;
    private final Vector2 direction;
    private boolean rooted;

    public MovementComponent(float maxSpeed) {
        this.maxSpeed = maxSpeed;
        this.direction = new Vector2();
    }

    public float getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(float maxSpeed) { this.maxSpeed = maxSpeed; }
    public Vector2 getDirection() { return direction; }
    public boolean isRooted() { return rooted; }
    public void setRooted(boolean rooted) { this.rooted = rooted; }
}

package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

/**
 * Komponent ruchu — przechowuje prędkość maksymalną i kierunek.
 * isRooted = true → Actor nie może się ruszać (np. podczas ataku, stun).
 */
public class MovementComponent extends AbstractActorComponent {
    public static final ComponentMapper<MovementComponent> MAPPER = ComponentMapper.getFor(MovementComponent.class);
    public static final float DEFAULT_SPEED_MULTIPLIER = 1f;
    public static final float SPRINT_SPEED_MULTIPLIER = 1.5f;

    private float maxSpeed;
    private final Vector2 direction;
    private boolean rooted;
    private boolean sprinting;
    private float speedMultiplier = DEFAULT_SPEED_MULTIPLIER;

    public MovementComponent(float maxSpeed) {
        this.maxSpeed = maxSpeed;
        this.direction = new Vector2();
    }

    public float getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(float maxSpeed) { this.maxSpeed = maxSpeed; }
    public float getEffectiveMaxSpeed() { return maxSpeed * speedMultiplier; }
    public Vector2 getDirection() { return direction; }
    public boolean isRooted() { return rooted; }
    public void setRooted(boolean rooted) { this.rooted = rooted; }
    public boolean isSprinting() { return sprinting; }
    public float getSpeedMultiplier() { return speedMultiplier; }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
        this.speedMultiplier = sprinting ? SPRINT_SPEED_MULTIPLIER : DEFAULT_SPEED_MULTIPLIER;
    }

    public void setSpeedMultiplier(float speedMultiplier) {
        this.speedMultiplier = Math.max(0f, speedMultiplier);
        this.sprinting = this.speedMultiplier > DEFAULT_SPEED_MULTIPLIER + 0.001f;
    }
}

package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

/**
 * Komponent animacji gracza — przechowuje animacje idle/walk dla 4 kierunków.
 */
public class PlayerAnimationComponent extends AbstractActorComponent {
    public static final ComponentMapper<PlayerAnimationComponent> MAPPER =
        ComponentMapper.getFor(PlayerAnimationComponent.class);

    public enum Direction {
        DOWN,
        LEFT,
        RIGHT,
        UP
    }

    private static final float WALK_FRAME_DURATION = 0.10f;
    private static final float IDLE_FRAME_DURATION = 0.16f;
    private static final float MOVE_EPSILON = 0.001f;

    private final Animation<TextureRegion> idleDown;
    private final Animation<TextureRegion> idleLeft;
    private final Animation<TextureRegion> idleRight;
    private final Animation<TextureRegion> idleUp;
    private final Animation<TextureRegion> walkDown;
    private final Animation<TextureRegion> walkLeft;
    private final Animation<TextureRegion> walkRight;
    private final Animation<TextureRegion> walkUp;

    private Direction facingDirection = Direction.DOWN;
    private boolean moving = false;
    private float stateTime = 0f;

    public PlayerAnimationComponent(TextureAtlas atlas) {
        this.idleDown = createLoopAnimation(atlas, "player/idle_down", IDLE_FRAME_DURATION);
        this.idleLeft = createLoopAnimation(atlas, "player/idle_left", IDLE_FRAME_DURATION);
        this.idleRight = createLoopAnimation(atlas, "player/idle_right", IDLE_FRAME_DURATION);
        this.idleUp = createLoopAnimation(atlas, "player/idle_up", IDLE_FRAME_DURATION);
        this.walkDown = createLoopAnimation(atlas, "player/walk_down", WALK_FRAME_DURATION);
        this.walkLeft = createLoopAnimation(atlas, "player/walk_left", WALK_FRAME_DURATION);
        this.walkRight = createLoopAnimation(atlas, "player/walk_right", WALK_FRAME_DURATION);
        this.walkUp = createLoopAnimation(atlas, "player/walk_up", WALK_FRAME_DURATION);
    }

    public void update(Vector2 direction, float delta) {
        boolean currentlyMoving = direction != null && !direction.isZero(MOVE_EPSILON);
        Direction resolvedDirection = resolveDirection(direction);

        if (resolvedDirection != facingDirection || currentlyMoving != moving) {
            facingDirection = resolvedDirection;
            moving = currentlyMoving;
            stateTime = 0f;
        } else {
            stateTime += delta;
        }
    }

    public TextureRegion getCurrentFrame() {
        return getCurrentAnimation().getKeyFrame(stateTime);
    }

    private Animation<TextureRegion> getCurrentAnimation() {
        return switch (facingDirection) {
            case DOWN -> moving ? walkDown : idleDown;
            case LEFT -> moving ? walkLeft : idleLeft;
            case RIGHT -> moving ? walkRight : idleRight;
            case UP -> moving ? walkUp : idleUp;
        };
    }

    private Direction resolveDirection(Vector2 direction) {
        if (direction == null || direction.isZero(MOVE_EPSILON)) {
            return facingDirection;
        }

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            return direction.x < 0f ? Direction.LEFT : Direction.RIGHT;
        }

        return direction.y < 0f ? Direction.DOWN : Direction.UP;
    }

    private Animation<TextureRegion> createLoopAnimation(TextureAtlas atlas, String regionName, float frameDuration) {
        var frames = atlas.findRegions(regionName);
        if (frames == null || frames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek animacji: " + regionName);
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }
}
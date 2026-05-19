package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class CreatureAnimationComponent extends AbstractActorComponent {
    public static final ComponentMapper<CreatureAnimationComponent> MAPPER =
        ComponentMapper.getFor(CreatureAnimationComponent.class);

    private static final float WALK_FRAME_DURATION = 0.20f;
    private static final float MOVE_EPSILON = 0.001f;
    private static final float DAMAGE_FLASH_DURATION = 0.12f;

    private final TextureRegion idleLeft;
    private final Animation<TextureRegion> walkLeft;
    private final boolean sourceFacesRight;

    private boolean facingRight;
    private boolean moving;
    private float stateTime;
    private float damageFlashRemaining;

    public CreatureAnimationComponent(TextureAtlas atlas, String idleRegionName, String walkRegionName) {
        this(atlas, idleRegionName, walkRegionName, false);
    }

    public CreatureAnimationComponent(TextureAtlas atlas,
                                      String idleRegionName,
                                      String walkRegionName,
                                      boolean sourceFacesRight) {
        this.idleLeft = findFrames(atlas, idleRegionName).first();
        this.walkLeft = createLoopAnimation(findFrames(atlas, walkRegionName), WALK_FRAME_DURATION);
        this.sourceFacesRight = sourceFacesRight;
    }

    public void update(Vector2 direction, float delta) {
        boolean currentlyMoving = direction != null && !direction.isZero(MOVE_EPSILON);
        boolean shouldFaceRight = currentlyMoving ? resolveFacingRight(direction) : facingRight;

        if (shouldFaceRight != facingRight || currentlyMoving != moving) {
            facingRight = shouldFaceRight;
            moving = currentlyMoving;
            stateTime = 0f;
        } else {
            stateTime += delta;
        }
    }

    public TextureRegion getCurrentFrame() {
        return moving ? walkLeft.getKeyFrame(stateTime) : idleLeft;
    }

    public boolean isFacingRight() {
        return facingRight;
    }

    public boolean shouldFlipHorizontally() {
        return sourceFacesRight ? !facingRight : facingRight;
    }

    public void triggerDamageFlash() {
        damageFlashRemaining = DAMAGE_FLASH_DURATION;
    }

    public void tickDamageFlash(float delta) {
        if (damageFlashRemaining > 0f) {
            damageFlashRemaining = Math.max(0f, damageFlashRemaining - delta);
        }
    }

    public boolean isDamageFlashActive() {
        return damageFlashRemaining > 0f;
    }

    private boolean resolveFacingRight(Vector2 direction) {
        if (direction == null || direction.isZero(MOVE_EPSILON)) {
            return facingRight;
        }

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            return direction.x > 0f;
        }

        return direction.y > 0f;
    }

    private Animation<TextureRegion> createLoopAnimation(Array<TextureAtlas.AtlasRegion> frames, float frameDuration) {
        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    private Array<TextureAtlas.AtlasRegion> findFrames(TextureAtlas atlas, String regionName) {
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(regionName);
        if (frames == null || frames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek animacji: " + regionName);
        }

        if (frames.size == 1) {
            for (int suffix = 1; ; suffix++) {
                TextureAtlas.AtlasRegion extraFrame = atlas.findRegion(regionName + suffix);
                if (extraFrame == null) {
                    break;
                }
                frames.add(extraFrame);
            }
        }

        return frames;
    }
}
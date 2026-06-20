package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

/**
 * Bardzo prosty komponent animacji NPC.
 * Przechowuje tylko idle i przesuwa czas animacji w petli.
 */
public class NpcAnimationComponent extends AbstractActorComponent {
    public static final ComponentMapper<NpcAnimationComponent> MAPPER =
        ComponentMapper.getFor(NpcAnimationComponent.class);

    private static final float IDLE_FRAME_DURATION = 0.22f;

    private final Animation<TextureRegion> idleAnimation;
    private float stateTime;

    /** Wczytuje jedna zapetleną animację NPC z atlasu. */
    public NpcAnimationComponent(TextureAtlas atlas, String idleRegionName) {
        Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(idleRegionName);
        if (frames == null || frames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek NPC: " + idleRegionName);
        }

        idleAnimation = new Animation<>(IDLE_FRAME_DURATION, frames, Animation.PlayMode.LOOP);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);
    }

    /** Przesuwa licznik czasu animacji do przodu. */
    public void update(float delta) {
        stateTime += Math.max(0f, delta);
    }

    /** Zwraca aktualną klatke idle NPC. */
    public TextureRegion getCurrentFrame() {
        return idleAnimation.getKeyFrame(stateTime);
    }
}

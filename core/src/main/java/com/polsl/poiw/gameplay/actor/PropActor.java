package com.polsl.poiw.gameplay.actor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.SpriteComponent;
import com.polsl.poiw.engine.component.TransformComponent;

/**
 * Statyczny obiekt środowiska z Tiled — dom, skrzynia, drzewo.
 */
public class PropActor extends AbstractActor {

    public void configure(TextureRegion region, float sizeW, float sizeH,
                          float collHalfW, float collHalfH, Vector2 collOffset,
                          float sortOffsetY, int zOrder) {
        addComponent(new TransformComponent(
            new Vector2(),
            zOrder,
            new Vector2(sizeW, sizeH),
            new Vector2(1f, 1f),
            0f,
            sortOffsetY
        ));

        addComponent(new SpriteComponent(region, Color.WHITE.cpy()));

        if (collHalfW > 0f && collHalfH > 0f) {
            addComponent(new BoxCollisionComponent(
                CollisionProfile.ENVIRONMENT,
                collHalfW,
                collHalfH,
                collOffset
            ));
        }
    }

    @Override
    public void beginPlay() {
        super.beginPlay();
    }
}

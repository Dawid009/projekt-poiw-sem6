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

    /**
     * Konfiguruje PropActor z danymi z Tiled.
     */
    public void configure(TextureRegion region, float sizeW, float sizeH,
                          float collHalfW, float collHalfH, Vector2 collOffset,
                          float sortOffsetY, int zOrder) {

        // Pozycja i rozmiar w świecie
        addComponent(new TransformComponent(
            new Vector2(getPosition()),
            zOrder,
            new Vector2(sizeW, sizeH),
            new Vector2(1f, 1f),
            0f,
            sortOffsetY
        ));

        // Grafika
        addComponent(new SpriteComponent(region, Color.WHITE.cpy()));

        // Kolizja (opcjonalna — tylko jeśli tile ma collision objectgroup w .tsx)
        if (collHalfW > 0 && collHalfH > 0) {
            addComponent(new BoxCollisionComponent(
                CollisionProfile.ENVIRONMENT, collHalfW, collHalfH, collOffset
            ));
        }
    }

    @Override
    public void beginPlay() {
        super.beginPlay();
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.getPosition().set(getPosition());
        }
    }
}

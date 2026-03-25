package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Komponent graficzny — przechowuje TextureRegion do rysowania przez RenderSystem.
 * Używany razem z TransformComponent (pozycja + rozmiar).
 */
public class SpriteComponent extends AbstractActorComponent {
    public static final ComponentMapper<SpriteComponent> MAPPER = ComponentMapper.getFor(SpriteComponent.class);

    private TextureRegion region;
    private final Color color;

    public SpriteComponent(TextureRegion region) {
        this(region, Color.WHITE.cpy());
    }

    public SpriteComponent(TextureRegion region, Color color) {
        this.region = region;
        this.color = color;
    }

    public TextureRegion getRegion() { return region; }
    public void setRegion(TextureRegion region) { this.region = region; }
    public Color getColor() { return color; }
}

package com.polsl.poiw.engine.collision;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

/**
 * Komponent kolizji w kształcie prostokąta (Box).
 * Obsługuje opcjonalny offset — przesunięcie kształtu względem centrum body.
 * Przydatne gdy np. kolizja gracza jest na nogach, a nie na środku sprite'a.
 */
public class BoxCollisionComponent extends CollisionComponent {
    public static final ComponentMapper<BoxCollisionComponent> MAPPER =
        ComponentMapper.getFor(BoxCollisionComponent.class);

    private final float halfWidth;   // połowa szerokości w metrach
    private final float halfHeight;  // połowa wysokości w metrach
    private final Vector2 offset;    // przesunięcie kształtu względem centrum body (w metrach)

    public BoxCollisionComponent(CollisionProfile profile, float halfWidth, float halfHeight) {
        this(profile, halfWidth, halfHeight, Vector2.Zero);
    }

    public BoxCollisionComponent(CollisionProfile profile, float halfWidth, float halfHeight, Vector2 offset) {
        super(profile);
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.offset = new Vector2(offset);
    }

    @Override
    protected Shape createShape() {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfWidth, halfHeight, offset, 0f);
        return shape;
    }

    public float getHalfWidth() { return halfWidth; }
    public float getHalfHeight() { return halfHeight; }
    public Vector2 getOffset() { return offset; }
}

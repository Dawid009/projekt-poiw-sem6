package com.polsl.poiw.engine.collision;
import com.badlogic.gdx.physics.box2d.*;


/** Podstawowy komponent kolizji (Box) */
public class BoxCollisionComponent extends CollisionComponent {
    private float halfWidth;   // połowa szerokości w metrach
    private float halfHeight;  // połowa wysokości w metrach

    public BoxCollisionComponent(CollisionProfile profile, float halfWidth, float halfHeight) {
        super(profile);
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
    }

    @Override
    protected Shape createShape() {
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(halfWidth, halfHeight);
        return shape;
    }
}

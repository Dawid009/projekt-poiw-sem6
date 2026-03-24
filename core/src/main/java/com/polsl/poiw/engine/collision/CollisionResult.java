package com.polsl.poiw.engine.collision;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.WorldManifold;

public class CollisionResult {
    private final com.badlogic.gdx.math.Vector2 contactPoint;
    private final com.badlogic.gdx.math.Vector2 contactNormal;

    public CollisionResult(Contact contact) {
        WorldManifold manifold = contact.getWorldManifold();
        this.contactPoint = manifold.getPoints()[0] != null
            ? new Vector2(manifold.getPoints()[0]) : new Vector2();
        this.contactNormal = new Vector2(manifold.getNormal());
    }

    // gettery...
}

package com.polsl.poiw.engine.collision;

import com.badlogic.gdx.physics.box2d.*;
import com.polsl.poiw.engine.component.AbstractActorComponent;

import java.util.ArrayList;
import java.util.List;

public abstract class CollisionComponent extends AbstractActorComponent {

    /** Profil kolizji (kto z kim koliduje i jak) */
    protected CollisionProfile profile;

    /** Box2D body i fixture — tworzone w initialize() */
    protected Body body;
    protected Fixture fixture;

    /** Czy generować overlap events (false = cichy, nie wywołuje OverlapListener) */
    protected boolean generateOverlapEvents = true;

    /** Lista listenerów na overlap events */
    private final List<OverlapListener> overlapListeners = new ArrayList<>();

    /** Czy kolizja jest aktywna (false = wyłączona, np. po śmierci) */
    private boolean enabled = true;

    public CollisionComponent(CollisionProfile profile) {
        this.profile = profile;
    }

    /**
     * Subklasy MUSZĄ zaimplementować tę metodę.
     * Tworzy kształt kolizji (Box2D Shape) o odpowiednim rozmiarze.
     */
    protected abstract Shape createShape();

    @Override
    public void initialize() {
        // Tworzenie Box2D body nastąpi w GameWorld.spawnActor()
        // bo potrzeba referencji do Box2D World
    }

    /**
     * Tworzy Box2D Body i Fixture.
     * Wywoływane przez GameWorld po dodaniu Actora do świata.
     * @param box2dWorld świat fizyki Box2D
     */
    public void createBody(World box2dWorld) {
        BodyDef bodyDef = new BodyDef();

        // Typ body zależy od profilu:
        // ENVIRONMENT → STATIC (ściany nie się nie ruszają)
        // TRIGGER → STATIC (trigger nie się nie rusza)
        // Reszta → DYNAMIC (gracz, wróg, pocisk — poruszają się)
        if (profile.getObjectType() == CollisionChannel.ENVIRONMENT
            || profile.getObjectType() == CollisionChannel.TRIGGER) {
            bodyDef.type = BodyDef.BodyType.StaticBody;
        } else {
            bodyDef.type = BodyDef.BodyType.DynamicBody;
        }

        bodyDef.position.set(getOwner().getPosition());
        bodyDef.fixedRotation = true; // nie obracamy body

        body = box2dWorld.createBody(bodyDef);
        body.setUserData(getOwner()); // pozwala odzyskać Actora z Body

        // Tworzy kształt (prostokąt, koło itd)
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = createShape();

        // TRIGGER i OVERLAP to "sensory" — nie blokują fizycznie
        if (profile.getObjectType() == CollisionChannel.TRIGGER
            || profile.getObjectType() == CollisionChannel.ITEM) {
            fixtureDef.isSensor = true;
        }

        fixture = body.createFixture(fixtureDef);
        fixtureDef.shape.dispose(); // shape nie jest już potrzebny po createFixture
    }

    @Override
    public void dispose() {
        if (body != null) {
            body.getWorld().destroyBody(body);
            body = null;
        }
    }

    // ===== Overlap Listeners =====

    public void addOverlapListener(OverlapListener listener) {
        overlapListeners.add(listener);
    }

    /** Wywoływane przez CollisionSystem */
    public void notifyBeginOverlap(com.polsl.poiw.engine.actor.Actor other, CollisionResult result) {
        if (!enabled || !generateOverlapEvents) return;
        for (OverlapListener l : overlapListeners) {
            l.onBeginOverlap(getOwner(), other, result);
        }
    }

    /** Wywoływane przez CollisionSystem */
    public void notifyEndOverlap(com.polsl.poiw.engine.actor.Actor other) {
        if (!enabled || !generateOverlapEvents) return;
        for (OverlapListener l : overlapListeners) {
            l.onEndOverlap(getOwner(), other);
        }
    }

    // ===== gettery/settery =====

    public CollisionProfile getProfile() { return profile; }
    public Body getBody() { return body; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}

package com.polsl.poiw.engine.collision;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.polsl.poiw.engine.component.AbstractActorComponent;
import com.polsl.poiw.engine.component.TransformComponent;

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

    /** Opcjonalne wymuszenie typu body niezależnie od CollisionProfile. */
    private BodyDef.BodyType bodyTypeOverride;

    /** Poprzednia i aktualna pozycja body — do interpolacji renderingu między krokami fizyki */
    private final Vector2 previousBodyPosition = new Vector2();
    private final Vector2 currentBodyPosition = new Vector2();

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
     * <p>
     * Body position = centrum sprite'a (Actor.position + halfSize).
     * Komponent sam odczytuje TransformComponent z Ownera — nie wymaga
     * zewnętrznego przekazywania offsetów (wzorzec self-contained component jak w UE).
     *
     * @param box2dWorld świat fizyki Box2D
     */
    public void createBody(World box2dWorld) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = resolveBodyType();

        // Body position = centrum sprite'a
        // Actor.position = lewy-dolny róg sprite'a (konwencja LibGDX)
        // Body center = position + (size / 2)
        Vector2 actorPos = getOwner().getPosition();
        TransformComponent transform = getOwner().getComponent(TransformComponent.class);
        if (transform != null) {
            Vector2 size = transform.getSize();
            bodyDef.position.set(
                actorPos.x + size.x * 0.5f,
                actorPos.y + size.y * 0.5f
            );
        } else {
            bodyDef.position.set(actorPos);
        }

        bodyDef.fixedRotation = true; // nie obracamy body
        bodyDef.gravityScale = 0f;    // top-down — brak grawitacji na body

        body = box2dWorld.createBody(bodyDef);
        body.setUserData(getOwner()); // pozwala odzyskać Actora z Body

        // Tworzy kształt (prostokąt, koło itd)
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = createShape();

        // TRIGGER i ITEM to "sensory" — nie blokują fizycznie
        if (profile.getObjectType() == CollisionChannel.TRIGGER
            || profile.getObjectType() == CollisionChannel.ITEM) {
            fixtureDef.isSensor = true;
        }

        fixture = body.createFixture(fixtureDef);
        fixture.setUserData(this); // Pozwala CollisionSystem odczytać CollisionComponent z Fixture
        fixtureDef.shape.dispose(); // shape nie jest już potrzebny po createFixture

        previousBodyPosition.set(body.getPosition());
        currentBodyPosition.set(body.getPosition());
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
    public void setBodyTypeOverride(BodyDef.BodyType bodyTypeOverride) {
        this.bodyTypeOverride = bodyTypeOverride;
        if (body != null && bodyTypeOverride != null) {
            body.setType(bodyTypeOverride);
        }
    }

    /** Wywoływane tuż przed krokiem fizyki. */
    public void capturePreviousBodyPosition() {
        if (body != null) {
            previousBodyPosition.set(currentBodyPosition);
        }
    }

    /** Wywoływane zaraz po kroku fizyki. */
    public void captureCurrentBodyPosition() {
        if (body != null) {
            currentBodyPosition.set(body.getPosition());
        }
    }

    /** Zwraca interpolowaną pozycję body dla aktualnej klatki renderingu. */
    public Vector2 getInterpolatedBodyPosition(float alpha, Vector2 out) {
        return out.set(previousBodyPosition).lerp(currentBodyPosition, alpha);
    }

    private BodyDef.BodyType resolveBodyType() {
        if (bodyTypeOverride != null) {
            return bodyTypeOverride;
        }

        if (profile.getObjectType() == CollisionChannel.ENVIRONMENT
            || profile.getObjectType() == CollisionChannel.TRIGGER) {
            return BodyDef.BodyType.StaticBody;
        }

        return BodyDef.BodyType.DynamicBody;
    }
}

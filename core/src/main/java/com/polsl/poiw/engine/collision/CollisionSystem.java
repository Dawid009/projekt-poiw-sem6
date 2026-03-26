package com.polsl.poiw.engine.collision;

import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.gdx.physics.box2d.*;
import com.polsl.poiw.engine.actor.Actor;

/**
 * System kolizji — tłumaczy zdarzenia Box2D na overlap eventy Actorów.
 * <p>
 * Implementuje {@link ContactListener} z Box2D.
 * Przy beginContact / endContact odczytuje {@link CollisionProfile} z obu Actorów
 * i decyduje czy wywołać overlap event na {@link CollisionComponent}.
 * <p>
 * Rejestruje się jako ContactListener na Box2D World.
 */
public class CollisionSystem extends EntitySystem implements ContactListener {

    private final World box2dWorld;

    public CollisionSystem(World box2dWorld) {
        super(2); // Niskii priorytet — przed ruchem
        this.box2dWorld = box2dWorld;
        box2dWorld.setContactListener(this);
    }

    @Override
    public void update(float deltaTime) {
        // Box2D step jest robiony w GameWorld.update()
        // Ten system istnieje głównie jako ContactListener
    }

    // ===== Box2D ContactListener =====

    @Override
    public void beginContact(Contact contact) {
        ActorPair pair = extractActors(contact);
        if (pair == null) return;

        CollisionResult result = new CollisionResult(contact);

        // Sprawdź odpowiedź A na B
        CollisionResponseType responseAtoB = pair.collisionA.getProfile()
            .getResponseTo(pair.collisionB.getProfile().getObjectType());

        // Sprawdź odpowiedź B na A
        CollisionResponseType responseBtoA = pair.collisionB.getProfile()
            .getResponseTo(pair.collisionA.getProfile().getObjectType());

        // Overlap events — gdy co najmniej jedna strona reaguje OVERLAP
        if (responseAtoB == CollisionResponseType.OVERLAP) {
            pair.collisionA.notifyBeginOverlap(pair.actorB, result);
        }
        if (responseBtoA == CollisionResponseType.OVERLAP) {
            pair.collisionB.notifyBeginOverlap(pair.actorA, result);
        }
    }

    @Override
    public void endContact(Contact contact) {
        ActorPair pair = extractActors(contact);
        if (pair == null) return;

        CollisionResponseType responseAtoB = pair.collisionA.getProfile()
            .getResponseTo(pair.collisionB.getProfile().getObjectType());
        CollisionResponseType responseBtoA = pair.collisionB.getProfile()
            .getResponseTo(pair.collisionA.getProfile().getObjectType());

        if (responseAtoB == CollisionResponseType.OVERLAP) {
            pair.collisionA.notifyEndOverlap(pair.actorB);
        }
        if (responseBtoA == CollisionResponseType.OVERLAP) {
            pair.collisionB.notifyEndOverlap(pair.actorA);
        }
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
        // Sprawdź czy kolizja powinna być BLOCK czy IGNORE
        ActorPair pair = extractActors(contact);
        if (pair == null) return;

        CollisionResponseType responseAtoB = pair.collisionA.getProfile()
            .getResponseTo(pair.collisionB.getProfile().getObjectType());
        CollisionResponseType responseBtoA = pair.collisionB.getProfile()
            .getResponseTo(pair.collisionA.getProfile().getObjectType());

        // Jeśli którakolwiek strona ignoruje — wyłącz fizyczny kontakt
        if (responseAtoB == CollisionResponseType.IGNORE || responseBtoA == CollisionResponseType.IGNORE) {
            contact.setEnabled(false);
            return;
        }

        // Jeśli obie strony to OVERLAP — wyłącz fizyczną blokadę (ale overlap events nadal działają)
        if (responseAtoB == CollisionResponseType.OVERLAP && responseBtoA == CollisionResponseType.OVERLAP) {
            contact.setEnabled(false);
        }

        // W przeciwnym razie (BLOCK) — Box2D domyślnie blokuje fizycznie
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        // Nie potrzebujemy
    }

    // ===== Helpers =====

    /**
     * Wyciąga parę Actorów z Box2D Contact.
     * Zwraca null jeśli któryś Body nie ma przypisanego Actora (np. surowe collision body z mapy).
     */
    private ActorPair extractActors(Contact contact) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();

        Object udA = bodyA.getUserData();
        Object udB = bodyB.getUserData();

        if (!(udA instanceof Actor actorA) || !(udB instanceof Actor actorB)) {
            return null;
        }

        CollisionComponent collA = actorA.getComponent(BoxCollisionComponent.class);
        CollisionComponent collB = actorB.getComponent(BoxCollisionComponent.class);

        if (collA == null || collB == null) return null;
        if (!collA.isEnabled() || !collB.isEnabled()) return null;

        return new ActorPair(actorA, actorB, collA, collB);
    }

    /** Pomocnicza klasa na parę aktorów z kontaktu */
    private record ActorPair(Actor actorA, Actor actorB,
                             CollisionComponent collisionA, CollisionComponent collisionB) {}
}

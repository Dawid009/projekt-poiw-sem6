package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.TransformComponent;

/**
 * System ruchu — obsługuje dwa tryby:
 * <ul>
 *   <li><b>Z fizyką:</b> jeśli entity ma {@link CollisionComponent} z body → ustawia linearVelocity
 *       na Box2D body, a następnie synchronizuje pozycję body → TransformComponent.</li>
 *   <li><b>Bez fizyki:</b> bezpośrednio modyfikuje TransformComponent.position (fallback).</li>
 * </ul>
 */
public class MovementSystem extends IteratingSystem {
    private static final Vector2 TMP = new Vector2();

    public MovementSystem() {
        super(Family.all(TransformComponent.class, MovementComponent.class).get(), 10);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        MovementComponent move = MovementComponent.MAPPER.get(entity);
        TransformComponent transform = TransformComponent.MAPPER.get(entity);

        // Pobierz Body z CollisionComponent przez Actora
        // (Actor jest na Body.userData, CollisionComponent może być dowolnym podtypem)
        Body body = findBody(transform);

        if (move.isRooted()) {
            if (body != null) {
                body.setLinearVelocity(0, 0);
            }
            return;
        }

        if (body != null) {
            // === Tryb fizyczny: velocity na Box2D body ===
            if (move.getDirection().isZero()) {
                body.setLinearVelocity(0, 0);
            } else {
                TMP.set(move.getDirection()).nor();
                float speed = move.getMaxSpeed();
                body.setLinearVelocity(TMP.x * speed, TMP.y * speed);
            }

            // Synchronizuj pozycję body → TransformComponent
            // Body center = lewy dolny róg sprite + połowa rozmiaru
            Vector2 bodyPos = body.getPosition();
            Vector2 size = transform.getSize();
            transform.getPosition().set(
                bodyPos.x - size.x * 0.5f,
                bodyPos.y - size.y * 0.5f
            );
        } else {
            // === Tryb bezpośredni (bez Box2D body) ===
            if (move.getDirection().isZero()) return;

            TMP.set(move.getDirection()).nor();
            float speed = move.getMaxSpeed();
            Vector2 pos = transform.getPosition();
            pos.x += TMP.x * speed * deltaTime;
            pos.y += TMP.y * speed * deltaTime;
        }
    }

    /**
     * Znajduje Box2D Body powiązane z Actorem przez CollisionComponent.
     * Używa Actor.getComponentByType() aby znaleźć dowolny podtyp CollisionComponent.
     */
    private Body findBody(TransformComponent transform) {
        Actor owner = transform.getOwner();
        if (owner == null) return null;
        CollisionComponent collision = owner.getComponentByType(CollisionComponent.class);
        return (collision != null) ? collision.getBody() : null;
    }
}

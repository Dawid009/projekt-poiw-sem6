package com.polsl.poiw.engine.system;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.systems.IteratingSystem;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.NetRole;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.component.KnockbackComponent;
import com.polsl.poiw.engine.component.MovementComponent;
import com.polsl.poiw.engine.component.TransformComponent;

/**
 * System ruchu — obsługuje dwa tryby:
 * <ul>
 *   <li><b>Z fizyką:</b> jeśli entity ma {@link CollisionComponent} z body → ustawia linearVelocity
 *       na Box2D body, a następnie synchronizuje pozycję body → TransformComponent.
 *       Używa interpolacji między krokami fizyki aby wyeliminować jitter.</li>
 *   <li><b>Bez fizyki:</b> bezpośrednio modyfikuje TransformComponent.position (fallback).</li>
 * </ul>
 */
public class MovementSystem extends IteratingSystem {
    private static final float KNOCKBACK_CONTROL_SCALE = 0.22f;

    private static final Vector2 TMP = new Vector2();
    private static final Vector2 TMP_KNOCKBACK = new Vector2();
    private static final Vector2 TMP_VELOCITY = new Vector2();
    private static final Vector2 TMP_BODY_POS = new Vector2();

    public MovementSystem() {
        super(Family.all(TransformComponent.class, MovementComponent.class).get(), 10);
    }

    @Override
    protected void processEntity(Entity entity, float deltaTime) {
        MovementComponent move = MovementComponent.MAPPER.get(entity);
        TransformComponent transform = TransformComponent.MAPPER.get(entity);
        KnockbackComponent knockback = KnockbackComponent.MAPPER.get(entity);
        Actor actor = transform.getOwner();

        CollisionComponent collision = findCollision(transform);
        Body body = collision != null ? collision.getBody() : null;

        TMP_KNOCKBACK.setZero();
        boolean knockbackActive = false;
        float knockbackLift = 0f;
        boolean simulatedProxy = actor != null && actor.getNetRole() == NetRole.SIMULATED_PROXY;
        if (knockback != null) {
            knockback.tick(deltaTime);
            if (knockback.isActive()) {
                TMP_KNOCKBACK.set(knockback.getVelocity());
                knockbackActive = true;
            }
            if (!simulatedProxy) {
                knockbackLift = knockback.getLiftOffsetY();
            }
        }
        transform.setRenderOffset(0f, knockbackLift);

        // SIMULATED_PROXY - position managed by InterpolationSystem, do not modify
        if (simulatedProxy) {
            return;
        }

        if (body != null) {
            // === Tryb fizyczny: velocity na Box2D body ===
            TMP_VELOCITY.setZero();
            if (!move.isRooted() && !move.getDirection().isZero()) {
                TMP.set(move.getDirection()).nor();
                float speed = move.getMaxSpeed();
                if (knockbackActive) {
                    speed *= KNOCKBACK_CONTROL_SCALE;
                }
                TMP_VELOCITY.set(TMP).scl(speed);
            }
            if (knockbackActive) {
                TMP_VELOCITY.add(TMP_KNOCKBACK);
            }
            body.setLinearVelocity(TMP_VELOCITY);

            // Synchronizacja pozycji body → TransformComponent.
            // Używamy prawidłowej interpolacji między poprzednim i aktualnym stanem Box2D,
            // bez ekstrapolacji po velocity — dzięki temu przy blokującej kolizji nie ma
            // przeskoku w ścianę i snap-backu.
            Vector2 size = transform.getSize();

            float alpha = 0f;
            Actor owner = transform.getOwner();
            if (owner != null && owner.getWorld() != null) {
                alpha = owner.getWorld().getPhysicsAlpha();
            }

            Vector2 interpolatedPos = collision.getInterpolatedBodyPosition(alpha, TMP_BODY_POS);
            float renderX = interpolatedPos.x - size.x * 0.5f;
            float renderY = interpolatedPos.y - size.y * 0.5f;
            transform.getPosition().set(renderX, renderY);
        } else {
            // === Tryb bezpośredni (bez Box2D body) ===
            Vector2 pos = transform.getPosition();
            TMP_VELOCITY.setZero();
            if (!move.isRooted() && !move.getDirection().isZero()) {
                TMP.set(move.getDirection()).nor();
                float speed = move.getMaxSpeed();
                if (knockbackActive) {
                    speed *= KNOCKBACK_CONTROL_SCALE;
                }
                TMP_VELOCITY.set(TMP).scl(speed);
            }
            if (knockbackActive) {
                TMP_VELOCITY.add(TMP_KNOCKBACK);
            }
            if (TMP_VELOCITY.isZero(0.0001f)) {
                return;
            }
            pos.x += TMP_VELOCITY.x * deltaTime;
            pos.y += TMP_VELOCITY.y * deltaTime;
        }
    }

    /**
     * Znajduje Box2D Body powiązane z Actorem przez CollisionComponent.
     * Używa Actor.getComponentByType() aby znaleźć dowolny podtyp CollisionComponent.
     */
    private CollisionComponent findCollision(TransformComponent transform) {
        Actor owner = transform.getOwner();
        if (owner == null) return null;
        return owner.getComponentByType(CollisionComponent.class);
    }
}

package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;

/**
 * Prosty komponent odrzutu po trafieniu.
 * Trzyma dodatkową prędkość, która przez chwile pcha aktora w bok i lekko go podbija.
 */
public class KnockbackComponent extends AbstractActorComponent {
    public static final ComponentMapper<KnockbackComponent> MAPPER =
        ComponentMapper.getFor(KnockbackComponent.class);

    private static final float DEFAULT_DAMPING = 8.5f;
    private static final float DEFAULT_ARC_DURATION = 0.24f;
    private static final float MAX_KNOCKBACK_SPEED = 3.8f;
    private static final float MIN_ACTIVE_SPEED = 0.05f;

    private final Vector2 velocity = new Vector2();
    private final Vector2 tmpDirection = new Vector2();
    private float damping = DEFAULT_DAMPING;
    private float liftTime = DEFAULT_ARC_DURATION;
    private float liftDuration = DEFAULT_ARC_DURATION;
    private float liftHeight;

    @Replicated
    private float impulseDirX;

    @Replicated
    private float impulseDirY;

    @Replicated
    private float impulseStrength;

    @Replicated
    @RepNotify("onImpulseChanged")
    private int impulseCounter;

    public KnockbackComponent() {
        setReplicated(true);
    }

    /**
     * Zadaje odrzut w podanym kierunku.
     * Używane np. po otrzymaniu damage przez gracza albo potwora.
     */
    public void apply(Vector2 direction, float strength) {
        if (direction == null || direction.isZero(0.001f) || strength <= 0f) {
            return;
        }

        tmpDirection.set(direction).nor();
        startImpulse(tmpDirection, strength);

        impulseDirX = tmpDirection.x;
        impulseDirY = tmpDirection.y;
        impulseStrength = strength;
        impulseCounter += 1;
        markDirty("impulseCounter");
    }

    /** Oslabia odrzut z uplywem czasu. */
    public void tick(float delta) {
        if (!velocity.isZero(MIN_ACTIVE_SPEED)) {
            float decay = Math.max(0f, 1f - damping * delta);
            velocity.scl(decay);
            if (velocity.len2() <= MIN_ACTIVE_SPEED * MIN_ACTIVE_SPEED) {
                velocity.setZero();
            }
        } else {
            velocity.setZero();
        }

        if (liftTime < liftDuration) {
            liftTime = Math.min(liftDuration, liftTime + delta);
        }
    }

    /** Zwraca `true`, gdy odrzut nadal realnie dziala. */
    public boolean isActive() {
        return !velocity.isZero(MIN_ACTIVE_SPEED);
    }

    /** Aktualna dodatkowa predkosc odrzutu. */
    public Vector2 getVelocity() {
        return velocity;
    }

    /** Zwraca pionowe podbicie, ktore ladnie sprzedaje efekt trafienia. */
    public float getLiftOffsetY() {
        if (liftTime >= liftDuration || liftDuration <= 0f || liftHeight <= 0f) {
            return 0f;
        }

        float progress = liftTime / liftDuration;
        return 4f * liftHeight * progress * (1f - progress);
    }

    /** Ustawia tempo gaszenia odrzutu. */
    public void setDamping(float damping) {
        if (damping > 0f) {
            this.damping = damping;
        }
    }

    /** Odtwarza odrzut po stronie klienta na podstawie zreplikowanych danych. */
    @SuppressWarnings("unused")
    private void onImpulseChanged() {
        if (getOwner() != null && getOwner().hasAuthority()) {
            return;
        }

        tmpDirection.set(impulseDirX, impulseDirY);
        if (tmpDirection.isZero(0.001f) || impulseStrength <= 0f) {
            return;
        }

        startImpulse(tmpDirection, impulseStrength);
    }

    /** Dodaje wewnetrzna predkosc i pilnuje, zeby nie byla zbyt duza. */
    private void startImpulse(Vector2 direction, float strength) {
        tmpDirection.set(direction).nor().scl(strength);
        velocity.add(tmpDirection);
        float speed = velocity.len();
        if (speed > MAX_KNOCKBACK_SPEED) {
            velocity.scl(MAX_KNOCKBACK_SPEED / speed);
        }

        liftDuration = DEFAULT_ARC_DURATION + Math.min(0.08f, strength * 0.015f);
        liftHeight = Math.max(liftHeight * 0.55f, 0.12f + Math.min(0.12f, strength * 0.03f));
        liftTime = 0f;
    }
}

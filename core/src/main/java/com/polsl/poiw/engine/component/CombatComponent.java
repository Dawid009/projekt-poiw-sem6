package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;

/**
 * Komponent walki melee.
 * Przechowuje parametry ataku, cooldown i ostatni kierunek postaci.
 */
public class CombatComponent extends AbstractActorComponent {
    public static final ComponentMapper<CombatComponent> MAPPER =
        ComponentMapper.getFor(CombatComponent.class);

    private static final float MOVE_EPSILON = 0.001f;

    private final boolean attackEnabled;
    private final int minDamage;
    private final int maxDamage;
    private final float attackCooldown;
    private final float attackDuration;
    private final float hitboxLifetime;
    private final float verticalHitboxWidth;
    private final float verticalHitboxHeight;
    private final float horizontalHitboxWidth;
    private final float horizontalHitboxHeight;
    private final float attackReach;

    @Replicated
    @RepNotify("onCombatStateChanged")
    private int facingDirectionOrdinal = PlayerAnimationComponent.Direction.DOWN.ordinal();

    private boolean attackRequested;

    @Replicated
    @RepNotify("onCombatStateChanged")
    private boolean attacking;

    @Replicated
    @RepNotify("onCombatStateChanged")
    private int attackCounter;

    private transient int consumedAttackCounter;

    private float cooldownRemaining;
    private float attackRemaining;

    public CombatComponent(boolean attackEnabled,
                           int minDamage,
                           int maxDamage,
                           float attackCooldown,
                           float attackDuration,
                           float hitboxLifetime,
                           float verticalHitboxWidth,
                           float verticalHitboxHeight,
                           float horizontalHitboxWidth,
                           float horizontalHitboxHeight,
                           float attackReach) {
                setReplicated(true);
        this.attackEnabled = attackEnabled;
        this.minDamage = minDamage;
        this.maxDamage = maxDamage;
        this.attackCooldown = attackCooldown;
        this.attackDuration = attackDuration;
        this.hitboxLifetime = hitboxLifetime;
        this.verticalHitboxWidth = verticalHitboxWidth;
        this.verticalHitboxHeight = verticalHitboxHeight;
        this.horizontalHitboxWidth = horizontalHitboxWidth;
        this.horizontalHitboxHeight = horizontalHitboxHeight;
        this.attackReach = attackReach;
    }

    public static CombatComponent createPlayerMelee() {
        // Proporcje hitboxa bazują na attack_sensor_* z assets/maps/objects.tsx.
        return new CombatComponent(
            true,
            5,
            15,
            0.35f,
            0.30f,
            0.14f,
            32f / 16f,
            15f / 16f,
            15f / 16f,
            32f / 16f,
            1.15f
        );
    }

    /** Tworzy pasywny target, który może przyjmować obrażenia, ale sam nie atakuje. */
    public static CombatComponent createPassiveTarget() {
        return new CombatComponent(false, 0, 0, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
    }

    /** Zgłasza chęć ataku, która zostanie obsłużona później przez system walki. */
    public void requestAttack() {
        if (attackEnabled) {
            attackRequested = true;
        }
    }

    public boolean consumeAttackRequest() {
        boolean requested = attackRequested;
        attackRequested = false;
        return requested;
    }

    public boolean canStartAttack() {
        return attackEnabled && !attacking && cooldownRemaining <= 0f;
    }

    /** Startuje atak i uzbraja licznik używany do jednorazowego spawnu hitboxa. */
    public void startAttack() {
        attackCounter += 1;
        markDirty("attackCounter");
        setAttacking(true);
        attackRemaining = attackDuration;
        cooldownRemaining = attackCooldown;
    }

    /** Odswieża czas trwania ataku i cooldown bez dodatkowej logiki wejścia. */
    public void tickTimers(float delta) {
        if (cooldownRemaining > 0f) {
            cooldownRemaining = Math.max(0f, cooldownRemaining - delta);
        }

        if (attacking) {
            attackRemaining = Math.max(0f, attackRemaining - delta);
            if (attackRemaining <= 0f) {
                setAttacking(false);
            }
        }
    }

    /** Zapamiętuje ostatni sensowny kierunek ruchu, żeby atak miał poprawny zwrot. */
    public void updateFacing(Vector2 direction) {
        if (direction == null || direction.isZero(MOVE_EPSILON)) {
            return;
        }

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            setFacingDirection(direction.x < 0f
                ? PlayerAnimationComponent.Direction.LEFT
                : PlayerAnimationComponent.Direction.RIGHT);
            return;
        }

        setFacingDirection(direction.y < 0f
            ? PlayerAnimationComponent.Direction.DOWN
            : PlayerAnimationComponent.Direction.UP);
    }

    public int rollDamage() {
        return MathUtils.random(minDamage, maxDamage);
    }

    public boolean isAttacking() {
        return attacking;
    }

    /** Zwraca `true` tylko raz dla każdego nowo rozpoczętego ataku. */
    public boolean consumeAttackTrigger() {
        if (attackCounter <= consumedAttackCounter) {
            return false;
        }

        consumedAttackCounter = attackCounter;
        return true;
    }

    public boolean isAttackEnabled() {
        return attackEnabled;
    }

    public PlayerAnimationComponent.Direction getFacingDirection() {
        PlayerAnimationComponent.Direction[] values = PlayerAnimationComponent.Direction.values();
        int clampedOrdinal = Math.max(0, Math.min(facingDirectionOrdinal, values.length - 1));
        return values[clampedOrdinal];
    }

    public float getAttackDuration() {
        return attackDuration;
    }

    public float getHitboxLifetime() {
        return hitboxLifetime;
    }

    public float getAttackReach() {
        return attackReach;
    }

    public float getHitboxWidth() {
        return switch (getFacingDirection()) {
            case DOWN, UP -> verticalHitboxWidth;
            case LEFT, RIGHT -> horizontalHitboxWidth;
        };
    }

    public float getHitboxHeight() {
        return switch (getFacingDirection()) {
            case DOWN, UP -> verticalHitboxHeight;
            case LEFT, RIGHT -> horizontalHitboxHeight;
        };
    }

    private void setFacingDirection(PlayerAnimationComponent.Direction direction) {
        if (direction == null) {
            return;
        }

        int ordinal = direction.ordinal();
        if (facingDirectionOrdinal == ordinal) {
            return;
        }

        facingDirectionOrdinal = ordinal;
        markDirty("facingDirectionOrdinal");
    }

    private void setAttacking(boolean attacking) {
        if (this.attacking == attacking) {
            return;
        }

        this.attacking = attacking;
        markDirty("attacking");
    }

    @SuppressWarnings("unused")
    private void onCombatStateChanged() {
    }
}
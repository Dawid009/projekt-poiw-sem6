package com.polsl.poiw.engine.component;

import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;

/**
 * komponent zdrowia — replicated.
 * serwer modyfikuje HP (applyDamage/heal), zmiany replikowane do klientów.
 * klient binduje UI do {@link #getHealthProperty()} / {@link #getMaxHealthProperty()}.
 */
public class HealthComponent extends AbstractActorComponent {

    @Replicated
    @RepNotify("onHealthChanged")
    private float currentHealth;

    @Replicated
    @RepNotify("onMaxHealthChanged")
    private float maxHealth;

    @Replicated
    private int lastDamageOwnerId;

    /** observable property — bridge do UI (aktualizowane przez @RepNotify na kliencie) */
    private final transient PropertyBinding<Float> healthProperty;
    private final transient PropertyBinding<Float> maxHealthProperty;

    public HealthComponent() {
        this(100f, 100f);
    }

    public HealthComponent(float maxHealth, float currentHealth) {
        setReplicated(true);
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
        this.lastDamageOwnerId = -1;
        this.healthProperty = new PropertyBinding<>(currentHealth);
        this.maxHealthProperty = new PropertyBinding<>(maxHealth);
    }

    // ===== Modyfikacja zdrowia (server-only) =====

    /**
     * zadaje obrażenia — tylko na serwerze (authority)
     */
    public void applyDamage(float amount) {
        applyDamage(amount, -1);
    }

    public void applyDamage(float amount, int damageOwnerId) {
        if (getOwner() != null && !getOwner().hasAuthority()) return;
        if (amount <= 0f || currentHealth <= 0f) {
            return;
        }

        float nextHealth = Math.max(0f, currentHealth - amount);
        if (nextHealth >= currentHealth) {
            return;
        }

        setLastDamageOwnerId(damageOwnerId);
        setCurrentHealth(nextHealth);

        DamageReactionComponent damageReaction = getOwner() != null
            ? getOwner().getComponent(DamageReactionComponent.class)
            : null;
        if (damageReaction != null) {
            damageReaction.triggerReaction();
        }
    }

    /**
     * leczy — tylko na serwerze (authority)
     */
    public void heal(float amount) {
        if (getOwner() != null && !getOwner().hasAuthority()) return;
        setCurrentHealth(Math.min(maxHealth, currentHealth + amount));
    }

    public boolean isAlive() {
        return currentHealth > 0f;
    }

    /** Przywraca pełny stan zdrowia z zapisu bez zostawiania starego źródła obrażeń. */
    public void restoreState(float maxHealth, float currentHealth) {
        float normalizedMaxHealth = Math.max(1f, maxHealth);
        float normalizedCurrentHealth = Math.max(0f, Math.min(normalizedMaxHealth, currentHealth));
        setMaxHealth(normalizedMaxHealth);
        setCurrentHealth(normalizedCurrentHealth);
        setLastDamageOwnerId(-1);
    }

    // ===== Settery (dirty tracking) =====

    private void setCurrentHealth(float value) {
        this.currentHealth = value;
        markDirty("currentHealth");
        healthProperty.set(value);
    }

    private void setMaxHealth(float value) {
        this.maxHealth = value;
        markDirty("maxHealth");
        maxHealthProperty.set(value);
    }

    private void setLastDamageOwnerId(int value) {
        this.lastDamageOwnerId = value;
        markDirty("lastDamageOwnerId");
    }

    // ===== @RepNotify callbacks (called on client after replication apply) =====

    @SuppressWarnings("unused")
    public void onHealthChanged() {
        healthProperty.set(currentHealth);
    }

    @SuppressWarnings("unused")
    public void onMaxHealthChanged() {
        maxHealthProperty.set(maxHealth);
    }

    // ===== Gettery =====

    public float getCurrentHealth() { return currentHealth; }
    public float getMaxHealth() { return maxHealth; }
    public int getLastDamageOwnerId() { return lastDamageOwnerId; }

    /** observable HP — binduj do UI */
    public PropertyBinding<Float> getHealthProperty() { return healthProperty; }

    /** observable max HP — binduj do UI */
    public PropertyBinding<Float> getMaxHealthProperty() { return maxHealthProperty; }
}

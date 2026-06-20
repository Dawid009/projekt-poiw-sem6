package com.polsl.poiw.engine.component;

import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;

/**
 * Przechowuje zdrowie aktora i pilnuje, żeby stan byl taki sam po stronie klienta i serwera.
 * To tutaj trafia damage, heal i reset zdrowia po respawnie.
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

    /** Zadaje obrazenia bez przypisywania zrodla ataku. */
    public void applyDamage(float amount) {
        applyDamage(amount, -1);
    }

    /**
     * Zadaje obrazenia i zapamietuje, kto byl ich zrodlem.
     * Przydaje sie to do statystyk i do wykrywania, kto dobil przeciwnika.
     */
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

    /** Leczy aktora, ale nigdy ponad jego maksymalne HP. */
    public void heal(float amount) {
        if (getOwner() != null && !getOwner().hasAuthority()) return;
        setCurrentHealth(Math.min(maxHealth, currentHealth + amount));
    }

    /** Zwraca `true`, gdy postac nadal zyje. */
    public boolean isAlive() {
        return currentHealth > 0f;
    }

    /** Przywraca zdrowie do nowego stanu, np. po loadzie albo respawnie. */
    public void restoreState(float maxHealth, float currentHealth) {
        float normalizedMaxHealth = Math.max(1f, maxHealth);
        float normalizedCurrentHealth = Math.max(0f, Math.min(normalizedMaxHealth, currentHealth));
        setMaxHealth(normalizedMaxHealth);
        setCurrentHealth(normalizedCurrentHealth);
        setLastDamageOwnerId(-1);
    }

    /** Ustawia aktualne HP i od razu oznacza komponent jako zmieniony do replikacji. */
    private void setCurrentHealth(float value) {
        this.currentHealth = value;
        markDirty("currentHealth");
        healthProperty.set(value);
    }

    /** Ustawia maksymalne HP i odswieza bindowalna wartosc dla UI. */
    private void setMaxHealth(float value) {
        this.maxHealth = value;
        markDirty("maxHealth");
        maxHealthProperty.set(value);
    }

    /** Zapisuje ID aktora, ktory ostatnio zadal obrazenia. */
    private void setLastDamageOwnerId(int value) {
        this.lastDamageOwnerId = value;
        markDirty("lastDamageOwnerId");
    }

    @SuppressWarnings("unused")
    public void onHealthChanged() {
        healthProperty.set(currentHealth);
    }

    @SuppressWarnings("unused")
    public void onMaxHealthChanged() {
        maxHealthProperty.set(maxHealth);
    }

    /** Aktualne HP widziane przez logike gry. */
    public float getCurrentHealth() { return currentHealth; }
    /** Maksymalne HP postaci. */
    public float getMaxHealth() { return maxHealth; }
    /** ID zrodla ostatniego damage. */
    public int getLastDamageOwnerId() { return lastDamageOwnerId; }

    /** Bindowalna wartosc HP do HUD-u. */
    public PropertyBinding<Float> getHealthProperty() { return healthProperty; }

    /** Bindowalna wartosc maksymalnego HP do HUD-u. */
    public PropertyBinding<Float> getMaxHealthProperty() { return maxHealthProperty; }
}

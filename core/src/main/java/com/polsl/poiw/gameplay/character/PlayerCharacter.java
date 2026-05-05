package com.polsl.poiw.gameplay.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.collision.BoxCollisionComponent;
import com.polsl.poiw.engine.collision.CollisionProfile;
import com.polsl.poiw.engine.component.*;

/**
 * Postać gracza — podstawowy Actor z komponentami ruchu, grafiki, kamery i kolizji.
 * zdrowie zarządzane przez {@link HealthComponent} — replicated, serwer autorytatywny.
 */
public class PlayerCharacter extends AbstractActor {

    /** Prędkość gracza w metrach/s */
    private static final float PLAYER_SPEED = 3.5f;

    /** Maksymalne i początkowe HP */
    private static final float MAX_HEALTH = 100f;

    /** Klucz regionu w atlasie */
    private static final String PLAYER_REGION = "player/idle_down";

    /** Rozmiar sprite'a w pikselach (32x32) */
    private static final float SPRITE_PX = 32f;

    public PlayerCharacter() {
        // components are added in configure() or configureServer()
    }

    /**
     * server configuration (headless) — without sprites and camera.
     * adds TransformComponent, MovementComponent, ControllerComponent, BoxCollisionComponent, HealthComponent.
     */
    public void configureServer() {
        float sizeW = SPRITE_PX / 16f;
        float sizeH = SPRITE_PX / 16f;

        addComponent(new TransformComponent(
            new Vector2(), 1, new Vector2(sizeW, sizeH)
        ));
        addComponent(new MovementComponent(PLAYER_SPEED));
        addComponent(new ControllerComponent());
        addComponent(new HealthComponent(MAX_HEALTH, MAX_HEALTH));

        float ppm = 16f;
        float collHalfW = 9f / 2f / ppm;
        float collHalfH = 5f / 2f / ppm;
        float offsetX = (11f + 4.5f - 16f) / ppm;
        float offsetY = -((18f + 2.5f - 16f) / ppm);
        addComponent(new BoxCollisionComponent(
            CollisionProfile.PLAYER, collHalfW, collHalfH, new Vector2(offsetX, offsetY)
        ));
    }

    /**
     * Konfiguruje gracza z podanym atlasem (klient).
     * Wywoływane po stworzeniu, ale przed beginPlay().
     */
    public void configure(TextureAtlas atlas) {
        TextureRegion region = atlas.findRegion(PLAYER_REGION);
        if (region == null) {
            throw new RuntimeException("Nie znaleziono regionu: " + PLAYER_REGION + " w atlasie");
        }

        float sizeW = SPRITE_PX * Main.UNIT_SCALE;
        float sizeH = SPRITE_PX * Main.UNIT_SCALE;

        addComponent(new TransformComponent(
            new Vector2(), 1, new Vector2(sizeW, sizeH)
        ));
        addComponent(new SpriteComponent(region, Color.WHITE.cpy()));


        // Animacje idle/walk zależne od kierunku i ruchu
        addComponent(new PlayerAnimationComponent(atlas));

        // Movement component - opisuje aktualny ruch i jego parametry
        addComponent(new MovementComponent(PLAYER_SPEED));
        addComponent(new CameraFollowComponent());
        addComponent(new ControllerComponent());
        addComponent(new HealthComponent(MAX_HEALTH, MAX_HEALTH));

        // Kolizja gracza — kształt z objects.tsx: x=11,y=18,w=9,h=5 px (sprite 32x32)
        float ppm = 16f;
        float collHalfW = 9f / 2f / ppm;
        float collHalfH = 5f / 2f / ppm;
        float offsetX = (11f + 4.5f - 16f) / ppm;
        float offsetY = -((18f + 2.5f - 16f) / ppm);
        addComponent(new BoxCollisionComponent(
            CollisionProfile.PLAYER, collHalfW, collHalfH, new Vector2(offsetX, offsetY)
        ));

        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.setSortOffsetY(sizeH / 2f + offsetY - collHalfH);
        }
    }

    @Override
    public void beginPlay() {
        super.beginPlay();
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
    }

    // ===== System zdrowia (delegacja do HealthComponent) =====

    /** Zadaje obrażenia graczowi — deleguje do HealthComponent */
    public void applyDamage(float amount) {
        HealthComponent hc = getComponent(HealthComponent.class);
        if (hc != null) hc.applyDamage(amount);
    }

    /** Leczy gracza — deleguje do HealthComponent */
    public void heal(float amount) {
        HealthComponent hc = getComponent(HealthComponent.class);
        if (hc != null) hc.heal(amount);
    }

    public boolean isAlive() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null && hc.isAlive();
    }

    /** Obserwowalne HP — binduj do UI (bridge do HealthComponent) */
    public PropertyBinding<Float> getHealth() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null ? hc.getHealthProperty() : new PropertyBinding<>(0f);
    }

    /** Obserwowalne max HP — binduj do UI (bridge do HealthComponent) */
    public PropertyBinding<Float> getMaxHealth() {
        HealthComponent hc = getComponent(HealthComponent.class);
        return hc != null ? hc.getMaxHealthProperty() : new PropertyBinding<>(0f);
    }
}

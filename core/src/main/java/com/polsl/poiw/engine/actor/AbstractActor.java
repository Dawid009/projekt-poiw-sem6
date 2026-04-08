package com.polsl.poiw.engine.actor;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.component.TransformComponent;
import com.polsl.poiw.engine.world.GameWorld;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractActor implements Actor {

    // Unikalny identyfikator (generowany przez ActorIdGenerator)
    private final int actorId;

    // Wewnętrzny Ashley Entity — przechowuje te same komponenty co Actor
    private final Entity ashleyEntity;

    // Mapa komponentów: typ klasy → instancja komponentu
    // LinkedHashMap zachowuje kolejność dodawania
    private final Map<Class<? extends ActorComponent>, ActorComponent> components;

    // Fallback pozycja — używana TYLKO gdy Actor nie ma jeszcze TransformComponent.
    // Po dodaniu TransformComponent, getPosition()/setPosition() delegują do niego.
    private final Vector2 fallbackPosition;

    // Rola sieciowa
    private NetRole netRole;

    // ID gracza-właściciela (-1 = brak właściciela, np. wróg kontrolowany przez serwer)
    private int ownerId;

    // Referencja do świata gry
    private GameWorld world;

    // Czas życia (jeśli > 0, Actor zostanie zniszczony po upływie tego czasu)
    private float lifeSpan;
    private float lifeSpanTimer;

    public AbstractActor() {
        this.actorId = ActorIdGenerator.next();
        this.ashleyEntity = new Entity();
        this.components = new LinkedHashMap<>();
        this.fallbackPosition = new Vector2();
        this.netRole = NetRole.NONE;
        this.ownerId = -1;
        this.lifeSpan = -1f;
    }

    @Override
    public <T extends ActorComponent> T addComponent(T component) {
        component.setOwner(this);
        components.put(component.getClass(), component);
        ashleyEntity.add(component);

        return component;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ActorComponent> T getComponent(Class<T> type) {
        return (T) components.get(type);
    }

    @Override
    public boolean hasComponent(Class<? extends ActorComponent> type) {
        return components.containsKey(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends ActorComponent> T getComponentByType(Class<T> baseType) {
        // Szukaj komponentu, którego klasa jest subklasą baseType
        for (ActorComponent comp : components.values()) {
            if (baseType.isInstance(comp)) {
                return (T) comp;
            }
        }
        return null;
    }

    @Override
    public void removeComponent(Class<? extends ActorComponent> type) {
        ActorComponent removed = components.remove(type);
        if (removed != null) {
            removed.dispose();
            ashleyEntity.remove(type); // usuwamy z Ashley Entity
        }
    }

    @Override
    public void beginPlay() {
        // Wywołaj initialize() na wszystkich komponentach
        for (ActorComponent comp : components.values()) {
            comp.initialize();
        }
    }

    @Override
    public void endPlay() {
        // Wywołaj dispose() na wszystkich komponentach
        for (ActorComponent comp : components.values()) {
            comp.dispose();
        }
    }

    @Override
    public void tick(float delta) {
        // Tickuj wszystkie komponenty
        for (ActorComponent comp : components.values()) {
            comp.tick(delta);
        }

        // Sprawdź lifeSpan
        if (lifeSpan > 0) {
            lifeSpanTimer += delta;
            if (lifeSpanTimer >= lifeSpan && world != null) {
                world.destroyActor(this);
            }
        }
    }

    // ===== Gettery / Settery =====

    @Override
    public int getActorId() { return actorId; }

    /**
     * Zwraca pozycję Actora w świecie (w metrach Box2D).
     * Deleguje do TransformComponent jeśli istnieje (single source of truth).
     */
    @Override
    public Vector2 getPosition() {
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            return transform.getPosition();
        }
        return fallbackPosition;
    }

    /**
     * Ustawia pozycję Actora w świecie.
     * Deleguje do TransformComponent jeśli istnieje.
     */
    @Override
    public void setPosition(float x, float y) {
        TransformComponent transform = getComponent(TransformComponent.class);
        if (transform != null) {
            transform.getPosition().set(x, y);
        } else {
            fallbackPosition.set(x, y);
        }
    }

    @Override
    public NetRole getNetRole() { return netRole; }

    @Override
    public void setNetRole(NetRole role) { this.netRole = role; }

    @Override
    public int getOwnerId() { return ownerId; }

    @Override
    public void setOwnerId(int ownerId) { this.ownerId = ownerId; }

    @Override
    public Entity getAshleyEntity() { return ashleyEntity; }

    @Override
    public GameWorld getWorld() { return world; }

    @Override
    public void setWorld(GameWorld world) { this.world = world; }

    @Override
    public void setLifeSpan(float seconds) {
        this.lifeSpan = seconds;
        this.lifeSpanTimer = 0f;
    }
}

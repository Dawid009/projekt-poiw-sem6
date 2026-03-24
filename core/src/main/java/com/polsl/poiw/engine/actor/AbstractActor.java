package com.polsl.poiw.engine.actor;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.engine.world.GameWorld;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractActor implements Actor {

    // Unikalny identyfikator (generowany przez ActorIdGenerator)
    private final int actorId;

    // Wewnętrzny Ashley Entity — przechowuje te same komponenty co Actor
    private final Entity ashleyEntity;

    // Mapa komponentów: typ klasy → instancja komponentu
    // LinkedHashMap zachowuje kolejność dodawania (ważne dla tick order)
    private final Map<Class<? extends ActorComponent>, ActorComponent> components;

    // Pozycja w świecie gry (w metrach Box2D, nie w pikselach!)
    private final Vector2 position;

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
        this.position = new Vector2();
        this.netRole = NetRole.NONE;
        this.ownerId = -1;
        this.lifeSpan = -1f;

        // Przechowujemy referencję do Actora w Entity.
        // Dzięki temu z Ashley Entity możemy wrócić do naszego Actora:
        //   Actor actor = (Actor) entity.getComponent(ActorRefComponent.class).actor;
        // Lub prościej: entity ma userData (custom trick).
    }

    @Override
    public <T extends ActorComponent> T addComponent(T component) {
        // 1. Ustawiamy ownera
        component.setOwner(this);

        // 2. Dodajemy do naszej mapy
        components.put(component.getClass(), component);

        // 3. Dodajemy do Ashley Entity (bo ActorComponent extends Component)
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

    // gettery/settery...
}

package com.polsl.poiw.engine.world;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.CollisionComponent;

import java.util.*;

public class GameWorld {

    /** Ashley ECS Engine */
    private final Engine ashleyEngine;

    /** Box2D fizyka */
    private final World box2dWorld;

    /** Mapa aktywnych Actorów: actorId → Actor */
    private final Map<Integer, Actor> actors = new LinkedHashMap<>();

    /** Kolejka Actorów do zniszczenia (niszczone PO iteracji, nie W TRAKCIE) */
    private final List<Actor> pendingDestroy = new ArrayList<>();

    /** Fixed timestep dla fizyki */
    private static final float PHYSICS_STEP = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private float physicsAccumulator = 0f;

    public GameWorld() {
        this.ashleyEngine = new Engine();
        // Box2D World z zerową grawitacją
        this.box2dWorld = new World(new Vector2(0, 0), true);
    }

    /**
     * Tworzy (spawnuje) Actora w świecie.
     *
     * @param actorClass klasa Actora do stworzenia (np. EnemyActor.class)
     * @param position pozycja startowa (w metrach Box2D)
     * @return nowy Actor
     *
     * Przykład:
     *   EnemyActor enemy = gameWorld.spawnActor(EnemyActor.class, new Vector2(5, 5));
     */
    public <T extends AbstractActor> T spawnActor(Class<T> actorClass, Vector2 position) {
        try {
            T actor = actorClass.getDeclaredConstructor().newInstance();
            actor.setPosition(position.x, position.y);
            actor.setWorld(this);

            // Jeśli Actor ma CollisionComponent → tworzymy Box2D body
            CollisionComponent collision = actor.getComponent(CollisionComponent.class);
            if (collision != null) {
                collision.createBody(box2dWorld);
            }

            // Dodaje Ashley Entity do Engine (systemy zaczynają widzieć tego Actora)
            ashleyEngine.addEntity(actor.getAshleyEntity());

            // Rejestrowanie w mapie
            actors.put(actor.getActorId(), actor);

            // Lifecycle: beginPlay
            actor.beginPlay();

            return actor;
        } catch (Exception e) {
            throw new RuntimeException("Nie można stworzyć Actora: " + actorClass.getName(), e);
        }
    }

    /**
     * Oznacza Actora do zniszczenia (faktyczne usunięcie po zakończeniu tick).
     */
    public void destroyActor(Actor actor) {
        if (!pendingDestroy.contains(actor)) {
            pendingDestroy.add(actor);
        }
    }

    /**
     * Aktualizuje cały świat gry — wywoływane CO KLATKĘ z GameScreen.render().
     */
    public void update(float delta) {
        // 1. Fizyka (Box2D) — fixed timestep
        physicsAccumulator += delta;
        while (physicsAccumulator >= PHYSICS_STEP) {
            box2dWorld.step(PHYSICS_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            physicsAccumulator -= PHYSICS_STEP;
        }

        // 2. Tick Actorów
        for (Actor actor : actors.values()) {
            actor.tick(delta);
        }

        // 3. Ashley Systems update
        ashleyEngine.update(delta);

        // 4. Niszczenie oznaczonych Actorów
        for (Actor actor : pendingDestroy) {
            actor.endPlay();
            ashleyEngine.removeEntity(actor.getAshleyEntity());
            actors.remove(actor.getActorId());
        }
        pendingDestroy.clear();
    }

    // ===== Queries =====

    /** Znajduje Actora po ID */
    public Actor getActorById(int actorId) {
        return actors.get(actorId);
    }

    /** Pobiera wszystkich Actorów danej klasy */
    @SuppressWarnings("unchecked")
    public <T extends Actor> List<T> getActorsOfClass(Class<T> clazz) {
        List<T> result = new ArrayList<>();
        for (Actor actor : actors.values()) {
            if (clazz.isInstance(actor)) {
                result.add((T) actor);
            }
        }
        return result;
    }

    /** Pobiera Actorów w promieniu od punktu */
    public List<Actor> getActorsInRadius(Vector2 center, float radius) {
        List<Actor> result = new ArrayList<>();
        float radiusSq = radius * radius;
        for (Actor actor : actors.values()) {
            if (actor.getPosition().dst2(center) <= radiusSq) {
                result.add(actor);
            }
        }
        return result;
    }

    /** Wszystkie aktywne Actory */
    public Collection<Actor> getAllActors() {
        return Collections.unmodifiableCollection(actors.values());
    }

    // ===== Dostęp do systemów =====

    public void addSystem(com.badlogic.ashley.core.EntitySystem system) {
        ashleyEngine.addSystem(system);
    }

    /**
     * Pobiera system po typie klasy.
     * Używane np. do dostępu do RenderSystem, CameraSystem z GameScreen.
     */
    @SuppressWarnings("unchecked")
    public <T extends com.badlogic.ashley.core.EntitySystem> T getSystem(Class<T> systemClass) {
        return ashleyEngine.getSystem(systemClass);
    }

    public Engine getAshleyEngine() { return ashleyEngine; }
    public World getBox2dWorld() { return box2dWorld; }

    /** Sprzątanie — wywoływane przy zmianie ekranu */
    public void dispose() {
        for (Actor actor : actors.values()) {
            actor.endPlay();
        }
        actors.clear();
        box2dWorld.dispose();
    }
}

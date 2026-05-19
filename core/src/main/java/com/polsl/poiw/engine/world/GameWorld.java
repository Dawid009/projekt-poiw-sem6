package com.polsl.poiw.engine.world;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.actor.ActorIdGenerator;
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

    /** Kolejka Actorów do spawnu, używana gdy świat jest w trakcie update'u. */
    private final List<PendingSpawn<?>> pendingSpawn = new ArrayList<>();

    /** Fixed timestep dla fizyki */
    private static final float PHYSICS_STEP = 1f / 60f;
    private static final int VELOCITY_ITERATIONS = 6;
    private static final int POSITION_ITERATIONS = 2;
    private float physicsAccumulator = 0f;

    /** Współczynnik interpolacji między krokami fizyki (0..1) */
    private float physicsAlpha = 1f;
    private boolean updating;

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
            return spawnActor(actor, position);
        } catch (Exception e) {
            throw new RuntimeException("Nie można stworzyć Actora: " + actorClass.getName(), e);
        }
    }

    /**
     * Spawnuje już skonfigurowanego Actora w świecie.
     * Używane gdy Actor wymaga konfiguracji przed spawnem (np. PlayerCharacter.configure(atlas)).
     *
     * @param actor skonfigurowany Actor
     * @param position pozycja startowa
     * @return ten sam Actor
     */
    public <T extends AbstractActor> T spawnActor(T actor, Vector2 position) {
        return spawnActorInternal(actor, position);
    }

    /**
     * spawns actor with overriden id (replication from server)
     * used on client when server sends ActorSpawn with specific actorId.
      *
      * @param actor configured Actor
      * @param actorId id overridden by server
      * @param position starting position
      * @return the same Actor
      */
    public <T extends AbstractActor> T spawnActorWithId(T actor, int actorId, Vector2 position) {
        actor.overrideActorId(actorId);
        return spawnActorInternal(actor, position);
    }

    private <T extends AbstractActor> T spawnActorInternal(T actor, Vector2 position) {
        if (updating) {
            pendingSpawn.add(new PendingSpawn<>(actor, new Vector2(position)));
            return actor;
        }

        return spawnActorNow(actor, position);
    }

    private <T extends AbstractActor> T spawnActorNow(T actor, Vector2 position) {
        actor.setPosition(position.x, position.y);
        actor.setWorld(this);

        // Jeśli Actor ma CollisionComponent (dowolny podtyp) → tworzymy Box2D body
        // CollisionComponent sam oblicza body position z TransformComponent ownera
        CollisionComponent collision = actor.getComponentByType(CollisionComponent.class);
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
    }

    /**
     * Oznacza Actora do zniszczenia (faktyczne usunięcie po zakończeniu tick).
     */
    public void destroyActor(Actor actor) {
        if (!pendingDestroy.contains(actor)) {
            pendingDestroy.add(actor);
        }
    }

    // destroys actor by id (used in replication - server informs about destruction)
    public void destroyActorById(int actorId) {
        Actor actor = actors.get(actorId);
        if (actor != null) {
            destroyActor(actor);
        }
    }
    
    // resets the ActorIdGenerator (e.g. when changing level)
    public void resetActorIds() {
        ActorIdGenerator.reset();
    }

    /**
     * Aktualizuje cały świat gry — wywoływane CO KLATKĘ z WorldContext.update().
     */
    public void update(float delta) {
        updating = true;

        // 1. Fizyka (Box2D) — fixed timestep, max 8 steps per frame (prevents spiral)
        physicsAccumulator += delta;
        int maxSteps = 8;
        while (physicsAccumulator >= PHYSICS_STEP && maxSteps-- > 0) {
            capturePreviousPhysicsState();
            box2dWorld.step(PHYSICS_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            captureCurrentPhysicsState();
            physicsAccumulator -= PHYSICS_STEP;
        }
        // discard excess accumulated time to prevent spiral
        if (physicsAccumulator > PHYSICS_STEP * 4) {
            physicsAccumulator = 0f;
        }

        // Współczynnik interpolacji: ile czasu minęło od ostatniego pełnego kroku
        physicsAlpha = physicsAccumulator / PHYSICS_STEP;

        // 2. Tick Actorów
        for (Actor actor : new ArrayList<>(actors.values())) {
            if (actors.containsKey(actor.getActorId())) {
                actor.tick(delta);
            }
        }

        // 3. Ashley Systems update
        ashleyEngine.update(delta);

        updating = false;

        // 4. Niszczenie oznaczonych Actorów
        flushPendingDestroy();
        flushPendingSpawn();
        flushPendingDestroy();
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
     * Używane np. do dostępu do RenderSystem, CameraSystem z WorldContext.
     */
    @SuppressWarnings("unchecked")
    public <T extends com.badlogic.ashley.core.EntitySystem> T getSystem(Class<T> systemClass) {
        return ashleyEngine.getSystem(systemClass);
    }

    public Engine getAshleyEngine() { return ashleyEngine; }
    public World getBox2dWorld() { return box2dWorld; }

    /** Współczynnik interpolacji fizyki (0..1). Używane do smooth renderingu. */
    public float getPhysicsAlpha() { return physicsAlpha; }

    private void capturePreviousPhysicsState() {
        for (Actor actor : actors.values()) {
            CollisionComponent collision = actor.getComponentByType(CollisionComponent.class);
            if (collision != null) {
                collision.capturePreviousBodyPosition();
            }
        }
    }

    private void captureCurrentPhysicsState() {
        for (Actor actor : actors.values()) {
            CollisionComponent collision = actor.getComponentByType(CollisionComponent.class);
            if (collision != null) {
                collision.captureCurrentBodyPosition();
            }
        }
    }

    /** Sprzątanie — wywoływane przy zmianie ekranu */
    public void dispose() {
        for (Actor actor : actors.values()) {
            actor.endPlay();
        }
        actors.clear();
        box2dWorld.dispose();
    }

    private void flushPendingDestroy() {
        for (Actor actor : pendingDestroy) {
            actor.endPlay();
            ashleyEngine.removeEntity(actor.getAshleyEntity());
            actors.remove(actor.getActorId());
        }
        pendingDestroy.clear();
    }

    private void flushPendingSpawn() {
        if (pendingSpawn.isEmpty()) {
            return;
        }

        List<PendingSpawn<?>> spawns = new ArrayList<>(pendingSpawn);
        pendingSpawn.clear();
        for (PendingSpawn<?> pending : spawns) {
            spawnActorNow(pending.actor(), pending.position());
        }
    }

    private record PendingSpawn<T extends AbstractActor>(T actor, Vector2 position) {
    }
}

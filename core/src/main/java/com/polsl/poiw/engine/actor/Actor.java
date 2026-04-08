package com.polsl.poiw.engine.actor;


/** Podstawowy interfejs określający kształt aktora */
public interface Actor {

    /** Unikalny identyfikator Actora (generowany przez ActorIdGenerator) */
    int getActorId();

    /** Pobranie komponentu po typie. Zwraca null jeśli Actor nie ma tego komponentu. */
    <T extends ActorComponent> T getComponent(Class<T> type);

    /** Dodanie komponentu do Actora. Komponent dostaje referencję do ownera. */
    <T extends ActorComponent> T addComponent(T component);

    /** Usunięcie komponentu z Actora. */
    void removeComponent(Class<? extends ActorComponent> type);

    /** Czy Actor ma dany komponent? */
    boolean hasComponent(Class<? extends ActorComponent> type);

    /**
     * Znajduje komponent po typie bazowym (szuka wśród subklas).
     * Np. getComponentByType(CollisionComponent.class) znajdzie BoxCollisionComponent.
     * Wolniejsze niż getComponent() — używaj gdy nie znasz dokładnej klasy.
     */
    <T extends ActorComponent> T getComponentByType(Class<T> baseType);

    /**
     * Wywoływane RAZ po dodaniu Actora do GameWorld.
     * Tutaj inicjalizuje się komponenty, rejestruje listenery itd.
     */
    void beginPlay();

    /**
     * Wywoływane RAZ przed usunięciem Actora z GameWorld.
     * Tutaj sprząta się zasoby: usuwa Box2D body, wyrejestruj listenery itd.
     */
    void endPlay();

    /**
     * Wywoływane CO KLATKĘ przez GameWorld.
     * Domyślna implementacja deleguje tick do wszystkich komponentów.
     * @param delta czas w sekundach od ostatniej klatki (np. 0.016 dla 60 FPS)
     */
    void tick(float delta);

    /** Pozycja Actora w świecie (X, Y w metrach Box2D) */
    com.badlogic.gdx.math.Vector2 getPosition();
    void setPosition(float x, float y);

    /** Rola sieciowa */
    NetRole getNetRole();
    void setNetRole(NetRole role);

    /** ID gracza-właściciela (dla multiplayer — który gracz "posiada" tego Actora) */
    int getOwnerId();
    void setOwnerId(int ownerId);

    /**
     * Dostęp do wewnętrznego Ashley Entity.
     * Używane przez systemy Ashley (RenderSystem, CollisionSystem, etc.)
     */
    com.badlogic.ashley.core.Entity getAshleyEntity();

    /** Referencja do GameWorld, w którym żyje Actor */
    com.polsl.poiw.engine.world.GameWorld getWorld();
    void setWorld(com.polsl.poiw.engine.world.GameWorld world);

    /**
     * Ustawia czas życia — po tylu sekundach Actor zostanie automatycznie zniszczony.
     * Przydatne np. dla pocisków (giną po 5 sekundach).
     */
    void setLifeSpan(float seconds);
}

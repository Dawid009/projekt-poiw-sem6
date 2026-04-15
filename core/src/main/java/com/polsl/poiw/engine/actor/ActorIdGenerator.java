package com.polsl.poiw.engine.actor;

import java.util.concurrent.atomic.AtomicInteger;

public final class ActorIdGenerator {
    private static final AtomicInteger counter = new AtomicInteger(0);

    // id range: server = 1,2,3... (positive), client = -1,-2,-3... (negative temporary)
    //TODO: maybe a better solution would be to have separate counters for server and client?
    // this way we can avoid potential conflicts when a client spawns an actor and then
    // the server spawns another one before receiving the clients message
    private static boolean serverMode = true;

    /** Generuje kolejny unikalny ID */
    public static int next() {
        return serverMode ? counter.incrementAndGet() : counter.decrementAndGet();
    }

    /** Reset (np. przy zmianie mapy) */
    public static void reset() {
        counter.set(0);
    }

    // sets the ID generation mode (server/singleplayer = positive, client = negative)
    public static void setServerMode(boolean isServer) {
        serverMode = isServer;
        counter.set(0);
    }

    public static boolean isServerMode() { return serverMode; }

    private ActorIdGenerator() {} // nie można tworzyć instancji
}

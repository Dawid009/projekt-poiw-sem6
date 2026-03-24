package com.polsl.poiw.engine.actor;

import java.util.concurrent.atomic.AtomicInteger;

public final class ActorIdGenerator {
    private static final AtomicInteger counter = new AtomicInteger(0);

    /** Generuje kolejny unikalny ID */
    public static int next() {
        return counter.incrementAndGet();
    }

    /** Reset (np. przy zmianie mapy) */
    public static void reset() {
        counter.set(0);
    }

    private ActorIdGenerator() {} // nie można tworzyć instancji
}

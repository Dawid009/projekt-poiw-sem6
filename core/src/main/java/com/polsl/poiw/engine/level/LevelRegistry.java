package com.polsl.poiw.engine.level;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rejestr wszystkich dostępnych poziomów w grze.
 * <p>
 * Poziomy rejestrowane raz przy starcie aplikacji.
 * Pobierane przez levelId przy travel
 */
public class LevelRegistry {

    private final Map<String, LevelDefinition> levels = new LinkedHashMap<>();

    /**
     * Rejestruje definicję poziomu. Nadpisuje jeśli levelId już istnieje.
     */
    public void register(LevelDefinition definition) {
        levels.put(definition.getLevelId(), definition);
    }

    /**
     * Pobiera definicję poziomu po ID.
     */
    public LevelDefinition get(String levelId) {
        LevelDefinition def = levels.get(levelId);
        if (def == null) {
            throw new IllegalArgumentException("Nieznany level: '" + levelId + "'. "
                + "Dostępne: " + levels.keySet());
        }
        return def;
    }

    /** Czy level o podanym ID istnieje? */
    public boolean exists(String levelId) {
        return levels.containsKey(levelId);
    }

    /** Wszystkie zarejestrowane poziomy (niemutowalne) */
    public Map<String, LevelDefinition> getAll() {
        return Collections.unmodifiableMap(levels);
    }
}

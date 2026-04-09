package com.polsl.poiw.engine.level;

import com.polsl.poiw.engine.asset.MapAsset;
import com.polsl.poiw.engine.gameframework.GameMode;
import com.polsl.poiw.engine.gameframework.PlayerController;

/**
 * Definicja poziomu — opisuje wszystkie parametry potrzebne do uruchomienia świata.
 * LevelDefinition to dane — nie zarządza stanem, nie ma lifecycle.
 * Instancja uruchomionego poziomu to {@link WorldContext}.
 */
public class LevelDefinition {

    private final String levelId;
    private final String displayName;
    private final WorldType worldType;
    private final MapAsset mapAsset;
    private final Class<? extends GameMode> gameModeClass;
    private final Class<? extends PlayerController> controllerClass;
    private final InputMode inputMode;

    private LevelDefinition(Builder builder) {
        this.levelId = builder.levelId;
        this.displayName = builder.displayName;
        this.worldType = builder.worldType;
        this.mapAsset = builder.mapAsset;
        this.gameModeClass = builder.gameModeClass;
        this.controllerClass = builder.controllerClass;
        this.inputMode = builder.inputMode;
    }

    // ===== Gettery =====

    public String getLevelId() { return levelId; }
    public String getDisplayName() { return displayName; }
    public WorldType getWorldType() { return worldType; }
    public MapAsset getMapAsset() { return mapAsset; }
    public Class<? extends GameMode> getGameModeClass() { return gameModeClass; }
    public Class<? extends PlayerController> getControllerClass() { return controllerClass; }
    public InputMode getInputMode() { return inputMode; }

    public boolean isGameWorld() { return worldType == WorldType.GAME; }
    public boolean isUiOnly() { return worldType == WorldType.UI_ONLY; }

    @Override
    public String toString() {
        return "LevelDefinition{" + levelId + ", " + worldType + "}";
    }

    // ===== Builder =====

    public static Builder builder(String levelId) {
        return new Builder(levelId);
    }

    public static class Builder {
        private final String levelId;
        private String displayName;
        private WorldType worldType = WorldType.GAME;
        private MapAsset mapAsset;
        private Class<? extends GameMode> gameModeClass = GameMode.class;
        private Class<? extends PlayerController> controllerClass = PlayerController.class;
        private InputMode inputMode = InputMode.KEYBOARD_AND_MOUSE;

        private Builder(String levelId) {
            this.levelId = levelId;
            this.displayName = levelId;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder worldType(WorldType worldType) {
            this.worldType = worldType;
            return this;
        }

        public Builder map(MapAsset mapAsset) {
            this.mapAsset = mapAsset;
            return this;
        }

        public Builder gameMode(Class<? extends GameMode> gameModeClass) {
            this.gameModeClass = gameModeClass;
            return this;
        }

        public Builder controller(Class<? extends PlayerController> controllerClass) {
            this.controllerClass = controllerClass;
            return this;
        }

        public Builder inputMode(InputMode inputMode) {
            this.inputMode = inputMode;
            return this;
        }

        public LevelDefinition build() {
            if (levelId == null || levelId.isBlank()) {
                throw new IllegalArgumentException("levelId nie może być pusty");
            }
            return new LevelDefinition(this);
        }
    }
}

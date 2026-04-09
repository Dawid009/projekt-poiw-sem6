package com.polsl.poiw.gameplay.level;

import com.polsl.poiw.engine.asset.MapAsset;
import com.polsl.poiw.engine.level.InputMode;
import com.polsl.poiw.engine.level.LevelDefinition;
import com.polsl.poiw.engine.level.LevelRegistry;
import com.polsl.poiw.engine.level.WorldType;
import com.polsl.poiw.gameplay.gamemode.MainGameMode;
import com.polsl.poiw.gameplay.gamemode.MainPlayerController;
import com.polsl.poiw.gameplay.gamemode.MenuPlayerController;

/**
 * Rejestracja wszystkich poziomów dostępnych w grze.
 */
public final class LevelDefinitions {

    /** ID poziomu menu głównego */
    public static final String MAIN_MENU = "main_menu";

    /** ID poziomu gry */
    public static final String GAME = "game";

    private LevelDefinitions() {
    }

    /**
     * Rejestruje wszystkie poziomy w podanym rejestrze.
     */
    public static void registerAll(LevelRegistry registry) {

        // Menu główne — UI only, bez mapy, sterowanie myszą
        registry.register(
            LevelDefinition.builder(MAIN_MENU)
                .displayName("Menu główne")
                .worldType(WorldType.UI_ONLY)
                .controller(MenuPlayerController.class)
                .inputMode(InputMode.MOUSE_ONLY)
                .build()
        );

        // Poziom gry — pełny świat, mapa Tiled, klawiatura + mysz
        registry.register(
            LevelDefinition.builder(GAME)
                .displayName("Gra")
                .worldType(WorldType.GAME)
                .map(MapAsset.MAIN)
                .gameMode(MainGameMode.class)
                .controller(MainPlayerController.class)
                .inputMode(InputMode.KEYBOARD_AND_MOUSE)
                .build()
        );
    }
}

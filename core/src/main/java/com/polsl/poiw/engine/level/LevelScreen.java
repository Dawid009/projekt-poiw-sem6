package com.polsl.poiw.engine.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.polsl.poiw.Main;

/**
 * Uniwersalny ekran gry — hostuje aktywny {@link WorldContext}.
 */
public class LevelScreen extends ScreenAdapter {

    private static final String TAG = "LevelScreen";

    private final Main game;
    private WorldContext activeContext;

    /** Callback wywoływany po otwarciu poziomu (np. spawn gracza) */
    private LevelReadyCallback levelReadyCallback;

    public LevelScreen(Main game) {
        this.game = game;
    }

    // ===== Zarządzanie poziomami =====

    /**
     * Otwiera nowy poziom — niszczy poprzedni i tworzy nowy WorldContext.
     *
     * @param levelDef definicja poziomu do otwarcia
     */
    public void openLevel(LevelDefinition levelDef) {
        openLevel(levelDef, null);
    }

    /**
     * Otwiera nowy poziom z callbackiem.
     *
     * @param levelDef definicja poziomu do otwarcia
     * @param callback wywoływany po inicjalizacji (np. do spawnu gracza)
     */
    public void openLevel(LevelDefinition levelDef, LevelReadyCallback callback) {
        Gdx.app.debug(TAG, "Otwieram level: " + levelDef.getLevelId());

        // Zamknij poprzedni świat
        if (activeContext != null) {
            activeContext.dispose();
            activeContext = null;
        }

        // Stwórz i zainicjalizuj nowy kontekst
        activeContext = new WorldContext(game, levelDef);
        activeContext.initialize();

        this.levelReadyCallback = callback;

        // Wywołaj callback
        if (callback != null) {
            callback.onLevelReady(activeContext);
        }
    }

    // ===== Screen lifecycle =====

    @Override
    public void render(float delta) {
        // update GameInstance (connect timeout, network messages w fazie CONNECTING)
        game.getGameInstance().update(delta);

        if (activeContext != null && activeContext.isInitialized()) {
            activeContext.update(delta);
            activeContext.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (activeContext != null) {
            activeContext.resize(width, height);
        }
    }

    @Override
    public void hide() {
        // Nie dispose'ujemy — pozwalamy na zachowanie stanu przy hide
    }

    @Override
    public void dispose() {
        if (activeContext != null) {
            activeContext.dispose();
            activeContext = null;
        }
    }

    // ===== Dostęp =====

    public WorldContext getActiveContext() { return activeContext; }

    // ===== Callback =====

    /**
     * Callback po inicjalizacji poziomu.
     * Używany np. do spawnu gracza, konfiguracji specyficznej dla danego levelu.
     */
    @FunctionalInterface
    public interface LevelReadyCallback {
        void onLevelReady(WorldContext context);
    }
}

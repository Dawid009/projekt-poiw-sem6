package com.polsl.poiw;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.ui.screen.LoadingScreen;

import java.util.HashMap;
import java.util.Map;

/**
 * Główna klasa gry — punkt startowy aplikacji.
 * <p>
 * Extends {@link Game} z LibGDX, który zarządza aktywnym {@link Screen}.
 * Main odpowiada za zasoby współdzielone przez WSZYSTKIE ekrany:
 * SpriteBatch, AssetService, OrthographicCamera + Viewport, GameInstance, InputMultiplexer.
 */
public class Main extends Game {

    /** 1 tile = 16px = 1 metr w Box2D */
    public static final float UNIT_SCALE = 1f / 16f;

    /** Rozmiar viewport w metrach (nie pikselach) */
    public static final float WORLD_WIDTH = 16f;
    public static final float WORLD_HEIGHT = 9f;

    private SpriteBatch batch;
    private AssetService assetService;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private GameInstance gameInstance;
    private InputMultiplexer inputMultiplexer;

    /** Cache ekranów — pozwala na przełączanie się między ekranami po klasie */
    private final Map<Class<? extends Screen>, Screen> screenCache = new HashMap<>();

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        // Input multiplexer — rozdziela input między UI Stage a świat gry
        inputMultiplexer = new InputMultiplexer();
        Gdx.input.setInputProcessor(inputMultiplexer);

        // Wspólne zasoby
        batch = new SpriteBatch();
        assetService = new AssetService(new InternalFileHandleResolver());
        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        gameInstance = new GameInstance();

        // Zacznij od LoadingScreen (ładuje zasoby)
        addScreen(new LoadingScreen(this));
        setScreen(LoadingScreen.class);
    }

    /**
     * Dodaje ekran do cache. Pozwala później przełączyć się na niego przez {@link #setScreen(Class)}.
     */
    public void addScreen(Screen screen) {
        screenCache.put(screen.getClass(), screen);
    }

    /**
     * Przełącza aktywny ekran po klasie. Ekran musi być wcześniej dodany przez {@link #addScreen(Screen)}.
     */
    public void setScreen(Class<? extends Screen> screenClass) {
        Screen screen = screenCache.get(screenClass);
        if (screen == null) {
            throw new GdxRuntimeException("Screen " + screenClass.getSimpleName() + " not found in cache.");
        }
        super.setScreen(screen);
    }

    /** Usuwa ekran z cache. */
    public void removeScreen(Screen screen) {
        screenCache.remove(screen.getClass());
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        super.resize(width, height);
    }

    @Override
    public void dispose() {
        for (Screen screen : screenCache.values()) {
            screen.dispose();
        }
        screenCache.clear();
        batch.dispose();
        assetService.debugDiagnostics();
        assetService.dispose();
    }

    /**
     * Ustawia procesory inputu. Czyści poprzednie i dodaje nowe.
     */
    public void setInputProcessors(InputProcessor... processors) {
        inputMultiplexer.clear();
        if (processors == null) return;
        for (InputProcessor processor : processors) {
            if (processor != null) {
                inputMultiplexer.addProcessor(processor);
            }
        }
    }

    // ===== Gettery =====

    public SpriteBatch getBatch() { return batch; }
    public AssetService getAssetService() { return assetService; }
    public OrthographicCamera getCamera() { return camera; }
    public FitViewport getViewport() { return viewport; }
    public GameInstance getGameInstance() { return gameInstance; }
}
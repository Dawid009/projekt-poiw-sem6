package com.polsl.poiw;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.engine.asset.AtlasAsset;
import com.polsl.poiw.engine.level.LevelScreen;
import com.polsl.poiw.engine.level.WorldContext;
import com.polsl.poiw.engine.save.SinglePlayerSaveService;
import com.polsl.poiw.engine.settings.GraphicsSettingsService;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.level.LevelDefinitions;
import com.polsl.poiw.ui.screen.LoadingScreen;

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;

/**
 * Główna klasa gry — punkt startowy aplikacji.
 * <p>
 * Zarządza zasobami współdzielonymi między ekranami:
 * SpriteBatch, AssetService, Camera, Viewport, GameInstance, InputMultiplexer.
 * <p>
 * Przepływ:
 * <ol>
 *   <li>LoadingScreen ładuje zasoby</li>
 *   <li>Po załadowaniu: travel do "main_menu"</li>
 *   <li>Z menu: travel do "game" (spawn gracza, trigger, itp.)</li>
 * </ol>
 */
public class Main extends Game {

    /** 1 tile = 16px = 1 metr w Box2D */
    public static final float UNIT_SCALE = 1f / 16f;

    /** Rozmiar viewport w metrach (nie pikselach) */
    public static final float WORLD_WIDTH = 16f;
    public static final float WORLD_HEIGHT = 9f;
    public static final int REFERENCE_WIDTH = 1280;
    public static final float WORLD_UNITS_PER_PIXEL = WORLD_WIDTH / REFERENCE_WIDTH;

    private SpriteBatch batch;
    private AssetService assetService;
    private OrthographicCamera camera;
    private ScreenViewport viewport;
    private GameInstance gameInstance;
    private InputMultiplexer inputMultiplexer;

    /** Uniwersalny ekran hostujący aktywny WorldContext */
    private LevelScreen levelScreen;

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
        viewport = new ScreenViewport(camera);
        // Wieksza rozdzielczosc pokazuje wiecej swiata, bez rozciagania obiektow.
        viewport.setUnitsPerPixel(WORLD_UNITS_PER_PIXEL);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        GraphicsSettingsService.initialize();
        GraphicsSettingsService.applySettings(GraphicsSettingsService.getAppliedSettings());
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

        gameInstance = new GameInstance();

        // LevelScreen — jeden ekran dla wszystkich poziomów
        levelScreen = new LevelScreen(this);

        // Rejestracja poziomów
        LevelDefinitions.registerAll(gameInstance.getLevelRegistry());

        // Połącz GameInstance z LevelScreen
        gameInstance.setLevelScreen(levelScreen);

        // Travel callback — konfiguracja specyficzna dla poziomu po otwarciu
        gameInstance.setTravelCallback(this::onLevelReady);

        // Zacznij od LoadingScreen (ładuje zasoby)
        setScreen(new LoadingScreen(this));
    }

    /**
     * Callback wywoływany po otwarciu poziomu przez travel().
     * Konfiguruje nowo otwarty świat (np. spawn gracza).
     */
    private void onLevelReady(WorldContext context) {
        String levelId = context.getLevelDefinition().getLevelId();

        switch (levelId) {
            case LevelDefinitions.GAME -> setupGameLevel(context);
            // Menu nie wymaga dodatkowej konfiguracji
        }
    }

    /**
     * Konfiguracja poziomu gry — spawn gracza i odtworzenie stanu save'a.
     * W multiplayer: serwer kontroluje spawn gracza (nie tworzymy lokalnie).
     * W singleplayerze ta metoda umie też podnieść stan z wybranego save'a.
     */
    private void setupGameLevel(WorldContext context) {
        // in multiplayer server spawns the pawn - client waits for ActorSpawn
        if (gameInstance.isMultiplayer()) {
            Gdx.app.debug("Main", "Multiplayer: pawn będzie zespawnowany przez serwer");
            return;
        }

        SinglePlayerSaveService saveService = gameInstance.getSinglePlayerSaveService();

        // Spawn gracza na pierwszej pozycji startowej z mapy
        Vector2 startPos = saveService.resolvePlayerSpawn(context.getTiledParser().getPlayerStartPosition(0));
        TextureAtlas atlas = assetService.get(AtlasAsset.OBJECTS);
        TextureAtlas actionsAtlas = assetService.get(AtlasAsset.PLAYER_ACTIONS);

        PlayerCharacter player = new PlayerCharacter();
        player.configure(atlas, actionsAtlas);
        context.getGameWorld().spawnActor(player, startPos);
        Gdx.app.debug("Main", "Gracz zespawnowany na: " + startPos);

        // Possess — controller przejmuje kontrolę nad graczem
        context.getPlayerController().possess(player);
        saveService.applyPendingSave(context, player, assetService);

    }

    /**
     * Przełącza na LevelScreen. Wywoływane po załadowaniu zasobów.
     */
    public void switchToLevelScreen() {
        setScreen(levelScreen);
    }

    /**
     * Zwraca LevelScreen — potrzebne do travel z LoadingScreen.
     */
    public LevelScreen getLevelScreen() {
        return levelScreen;
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
        if (levelScreen != null) {
            levelScreen.dispose();
        }
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
    public Viewport getViewport() { return viewport; }
    public GameInstance getGameInstance() { return gameInstance; }
}

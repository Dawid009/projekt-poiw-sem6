package com.polsl.poiw.engine.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.engine.asset.AtlasAsset;
import com.polsl.poiw.engine.asset.SkinAsset;
import com.polsl.poiw.engine.collision.CollisionSystem;
import com.polsl.poiw.engine.gameframework.GameMode;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.render.CameraSystem;
import com.polsl.poiw.engine.render.DebugRenderSystem;
import com.polsl.poiw.engine.render.RenderSystem;
import com.polsl.poiw.engine.system.ControllerSystem;
import com.polsl.poiw.engine.system.MovementSystem;
import com.polsl.poiw.engine.tiled.TiledMapParser;
import com.polsl.poiw.engine.ui.HUD;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.EVisibility;
import com.polsl.poiw.engine.ui.TextBlock;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.input.GameControllerState;
import com.polsl.poiw.input.KeyboardController;

/**
 * WorldContext — aktywna instancja poziomu.
 * <p>
 * Przechowuje cały stan uruchomionego świata:
 * <ul>
 *   <li>{@link LevelDefinition} — konfiguracja poziomu</li>
 *   <li>{@link GameWorld} — fizyka, ECS, aktorzy (null dla UI_ONLY)</li>
 *   <li>{@link GameMode} — reguły gry</li>
 *   <li>{@link PlayerController} — sterowanie graczem</li>
 *   <li>{@link HUD} — warstwa UI</li>
 * </ul>
 * <p>
 * Lifecycle:
 * <ol>
 *   <li>{@link #initialize()} — tworzy świat, systemy, GameMode, controller</li>
 *   <li>{@link #update(float)} — tick co klatkę</li>
 *   <li>{@link #render()} — renderuje świat i UI</li>
 *   <li>{@link #dispose()} — sprzątanie</li>
 * </ol>
 */
public class WorldContext implements Disposable {

    private static final String TAG = "WorldContext";

    private final Main game;
    private final LevelDefinition levelDef;

    // ===== Świat gry (null dla UI_ONLY) =====
    private GameWorld gameWorld;
    private TiledMapParser tiledParser;
    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;
    private DebugRenderSystem debugRenderSystem;

    // ===== Sterowanie =====
    private KeyboardController keyboardController;

    // ===== Framework =====
    private GameMode gameMode;
    private PlayerController playerController;
    private HUD hud;
    private Skin skin;

    // ===== Debug UI =====
    private TextBlock fpsDebugText;
    private float fpsUpdateTimer = 0f;

    // ===== Stan =====
    private boolean initialized = false;

    public WorldContext(Main game, LevelDefinition levelDef) {
        this.game = game;
        this.levelDef = levelDef;
    }

    // ===== Lifecycle =====

    /**
     * Inicjalizuje świat — tworzy GameWorld (jeśli GAME), systemy, HUD, GameMode, controller.
     * Wywoływane raz, po stworzeniu kontekstu.
     */
    public void initialize() {
        Gdx.app.debug(TAG, "Inicjalizacja: " + levelDef);

        this.skin = game.getAssetService().get(SkinAsset.DEFAULT);

        // HUD — Stage UI z osobnym viewportem (zawsze, nawet UI_ONLY)
        Stage hudStage = new Stage(new FitViewport(320f, 180f), game.getBatch());
        this.hud = new HUD(hudStage);

        if (levelDef.isGameWorld()) {
            initializeGameWorld();
        }

        // GameMode
        this.gameMode = createInstance(levelDef.getGameModeClass(), "GameMode");
        if (gameWorld != null) {
            gameMode.initGame(gameWorld);
        }

        // PlayerController
        this.playerController = createInstance(levelDef.getControllerClass(), "PlayerController");
        playerController.initialize(game.getGameInstance(), gameWorld, gameMode, hud, skin);

        initializeDebugHud();

        // Input
        configureInput();

        initialized = true;
        Gdx.app.debug(TAG, "Zainicjalizowano: " + levelDef.getLevelId());
    }

    /**
     * Inicjalizuje pełny pipeline gry: GameWorld, systemy ECS, Tiled mapa.
     */
    private void initializeGameWorld() {
        // GameWorld z fizyką
        this.gameWorld = new GameWorld();

        // Systemy Ashley
        addGameSystems();

        // KeyboardController (potrzebuje Engine do Family query)
        this.keyboardController = new KeyboardController(
            GameControllerState.class,
            gameWorld.getAshleyEngine()
        );

        // Tiled Map (jeśli zdefiniowana)
        if (levelDef.getMapAsset() != null) {
            loadTiledMap();
        }
    }

    /**
     * Dodaje systemy ECS do GameWorld.
     */
    private void addGameSystems() {
        gameWorld.addSystem(new CollisionSystem(gameWorld.getBox2dWorld()));
        gameWorld.addSystem(new ControllerSystem());
        gameWorld.addSystem(new MovementSystem());

        cameraSystem = new CameraSystem(game.getCamera());
        gameWorld.addSystem(cameraSystem);

        renderSystem = new RenderSystem(game.getBatch(), game.getViewport(), game.getCamera());
        gameWorld.addSystem(renderSystem);

        debugRenderSystem = new DebugRenderSystem(gameWorld.getBox2dWorld(), game.getCamera());
        gameWorld.addSystem(debugRenderSystem);
    }

    /**
     * Ładuje i parsuje mapę Tiled.
     */
    private void loadTiledMap() {
        AssetService assetService = game.getAssetService();
        TextureAtlas atlas = assetService.get(AtlasAsset.OBJECTS);

        var objectFactory = new com.polsl.poiw.gameplay.tiled.DefaultTiledObjectFactory(gameWorld, atlas);
        this.tiledParser = new TiledMapParser(gameWorld, assetService);
        tiledParser.setObjectFactory(objectFactory);

        TiledMap map = tiledParser.loadMap(levelDef.getMapAsset());
        objectFactory.setMap(map);
        tiledParser.parse(map);

        renderSystem.setMap(map);
        cameraSystem.setMap(map);
    }

    /**
     * Konfiguruje input na podstawie InputMode z LevelDefinition.
     */
    private void configureInput() {
        Stage hudStage = hud.getStage();

        switch (levelDef.getInputMode()) {
            case MOUSE_ONLY -> {
                // Tylko Stage (UI) — mysz
                game.setInputProcessors(hudStage);
            }
            case KEYBOARD_ONLY -> {
                if (keyboardController != null) {
                    game.setInputProcessors(keyboardController);
                }
            }
            case KEYBOARD_AND_MOUSE -> {
                if (keyboardController != null) {
                    // Stage ma priorytet (UI), potem klawiatura (ruch)
                    game.setInputProcessors(hudStage, keyboardController);
                } else {
                    game.setInputProcessors(hudStage);
                }
            }
        }
    }

    private void initializeDebugHud() {
        if (!levelDef.isGameWorld()) {
            return;
        }

        fpsDebugText = new TextBlock("FPS: --", skin);
        fpsDebugText.setAnchor(EAnchor.TOP_RIGHT);
        fpsDebugText.setAlignment(EAnchor.TOP_RIGHT);
        fpsDebugText.setOffset(-6f, -6f);
        fpsDebugText.setVariable(true);
        fpsDebugText.setVisibility(EVisibility.HIDDEN);
        hud.addToViewport(fpsDebugText);
    }

    // ===== Update / Render =====

    /**
     * Aktualizacja co klatkę. Wywoływane z aktywnego ekranu.
     */
    public void update(float delta) {
        delta = Math.min(delta, 1f / 30f);

        // F3 — debug rendering (tylko w GAME world)
        if (debugRenderSystem != null && Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugRenderSystem.toggle();
            if (fpsDebugText != null) {
                fpsDebugText.setVisibility(debugRenderSystem.isDebugEnabled()
                    ? EVisibility.VISIBLE
                    : EVisibility.HIDDEN);
            }
        }

        // GameWorld tick (fizyka, aktorzy, systemy Ashley)
        if (gameWorld != null) {
            gameWorld.update(delta);
        }

        // GameMode i PlayerController tick
        gameMode.tick(delta);
        playerController.tick(delta);

        // HUD tick i act
        updateDebugHud(delta);
        hud.update(delta);
    }

    private void updateDebugHud(float delta) {
        if (fpsDebugText == null || debugRenderSystem == null || !debugRenderSystem.isDebugEnabled()) {
            fpsUpdateTimer = 0f;
            return;
        }

        fpsUpdateTimer += delta;
        if (fpsUpdateTimer >= 1f) {
            fpsUpdateTimer -= 1f;
            fpsDebugText.setText("FPS: " + Gdx.graphics.getFramesPerSecond());
            fpsDebugText.updateLayout();
        }
    }

    /**
     * Renderuje świat i UI. Wywoływane po update().
     */
    public void render() {
        hud.render();
    }

    /**
     * Reaguje na zmianę rozmiaru okna.
     */
    public void resize(int width, int height) {
        if (levelDef.isGameWorld()) {
            game.getViewport().update(width, height, true);
        }
        hud.resize(width, height);
    }

    // ===== Dostęp =====

    public LevelDefinition getLevelDefinition() { return levelDef; }
    public GameWorld getGameWorld() { return gameWorld; }
    public GameMode getGameMode() { return gameMode; }
    public PlayerController getPlayerController() { return playerController; }
    public HUD getHUD() { return hud; }
    public Skin getSkin() { return skin; }
    public TiledMapParser getTiledParser() { return tiledParser; }
    public KeyboardController getKeyboardController() { return keyboardController; }
    public Main getGame() { return game; }
    public boolean isInitialized() { return initialized; }

    // ===== Dispose =====

    @Override
    public void dispose() {
        Gdx.app.debug(TAG, "Dispose: " + levelDef.getLevelId());
        initialized = false;

        if (playerController != null) {
            playerController.destroy();
            playerController = null;
        }
        if (gameMode != null) {
            gameMode.endGame();
            gameMode = null;
        }
        if (debugRenderSystem != null) {
            debugRenderSystem.dispose();
            debugRenderSystem = null;
        }
        if (renderSystem != null) {
            renderSystem.dispose();
            renderSystem = null;
        }
        if (gameWorld != null) {
            gameWorld.dispose();
            gameWorld = null;
        }
        if (hud != null) {
            hud.dispose();
            hud = null;
        }
    }

    // ===== Internals =====

    @SuppressWarnings("unchecked")
    private <T> T createInstance(Class<? extends T> clazz, String label) {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Nie można stworzyć " + label + ": " + clazz.getName(), e);
        }
    }
}

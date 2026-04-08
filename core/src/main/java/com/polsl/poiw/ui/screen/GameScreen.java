package com.polsl.poiw.ui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.asset.AtlasAsset;
import com.polsl.poiw.engine.asset.MapAsset;
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
import com.polsl.poiw.gameplay.tiled.DefaultTiledObjectFactory;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.gameplay.character.PlayerCharacter;
import com.polsl.poiw.gameplay.gamemode.MainGameMode;
import com.polsl.poiw.gameplay.gamemode.MainPlayerController;
import com.polsl.poiw.gameplay.actor.TriggerActor;
import com.polsl.poiw.input.GameControllerState;
import com.polsl.poiw.input.KeyboardController;

/**
 * Główny ekran gry — łączy GameWorld, TiledMapParser, systemy Ashley i rendering.
 * Wykorzystuje GameMode / PlayerController / HUD do organizacji logiki gry.
 */
public class GameScreen extends ScreenAdapter {
    private final Main game;

    private GameWorld gameWorld;
    private TiledMapParser tiledParser;
    private Skin skin;

    private GameMode gameMode;
    private PlayerController playerController;
    private HUD hud;

    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;
    private DebugRenderSystem debugRenderSystem;
    private KeyboardController keyboardController;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.app.debug("GameScreen", "Inicjalizacja GameScreen...");

        // 1. GameWorld
        this.gameWorld = new GameWorld();

        // 2. Systemy Ashley (kolejność priorytetów: controller → movement → camera → render)
        addSystems();

        // 3. KeyboardController — musi być po dodaniu systemów (potrzebuje Engine do Family query)
        this.keyboardController = new KeyboardController(
            GameControllerState.class,
            gameWorld.getAshleyEngine()
        );

        // 4. TiledMapParser — parsuje mapę
        TextureAtlas atlas = game.getAssetService().get(AtlasAsset.OBJECTS);
        DefaultTiledObjectFactory objectFactory = new DefaultTiledObjectFactory(gameWorld, atlas);
        this.tiledParser = new TiledMapParser(gameWorld, game.getAssetService());
        tiledParser.setObjectFactory(objectFactory);

        // 5. Załaduj i sparsuj mapę
        TiledMap map = tiledParser.loadMap(MapAsset.MAIN);
        objectFactory.setMap(map);
        tiledParser.parse(map);

        // 6. Przekaż mapę do systemów
        renderSystem.setMap(map);
        cameraSystem.setMap(map);

        // 7. Skin UI
        this.skin = game.getAssetService().get(SkinAsset.DEFAULT);

        // 8. HUD — Stage UI z osobnym viewportem
        Stage hudStage = new Stage(new FitViewport(320f, 180f), game.getBatch());
        this.hud = new HUD(hudStage);

        // 9. GameMode
        this.gameMode = new MainGameMode();
        gameMode.initGame(gameWorld);

        // 10. PlayerController z HUD
        this.playerController = new MainPlayerController(skin);
        playerController.initialize(gameWorld, gameMode, hud);

        // 11. Spawn gracza i possess
        PlayerCharacter player = spawnPlayer();
        playerController.possess(player);

        // 12. Testowy trigger zadający obrażenia — spawnuje się 2 metry obok gracza
        spawnDamageTrigger(player.getPosition());

        // 13. Ustaw input: HUD Stage (UI) → KeyboardController (ruch)
        game.setInputProcessors(hudStage, keyboardController);

        Gdx.app.debug("GameScreen", "GameScreen zainicjalizowany.");
    }

    /**
     * Dodaje systemy Ashley do GameWorld.
     * Kolejność priorytetów (niższy = wcześniej):
     * 2  - CollisionSystem (ContactListener na Box2D World)
     * 5  - ControllerSystem (odczyt komend z inputu)
     * 10 - MovementSystem (ruch na podstawie kierunku / Box2D velocity)
     * 99 - CameraSystem (śledzenie gracza)
     * 100 - RenderSystem (rysowanie)
     */
    private void addSystems() {
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
     * Tworzy gracza na pierwszej pozycji startowej z mapy.
     */
    private PlayerCharacter spawnPlayer() {
        Vector2 startPos = tiledParser.getPlayerStartPosition(0);
        TextureAtlas atlas = game.getAssetService().get(AtlasAsset.OBJECTS);

        PlayerCharacter player = new PlayerCharacter();
        player.configure(atlas);

        gameWorld.spawnActor(player, startPos);

        Gdx.app.debug("GameScreen", "Gracz zespawnowany na pozycji: " + startPos);
        return player;
    }

    /**
     * Spawnuje testowy trigger obrażeń obok gracza — zadaje 10 HP/s.
     * Służy do demonstracji systemu bindingów HP → UI.
     */
    private void spawnDamageTrigger(Vector2 playerPos) {
        TriggerActor damageTrigger = new TriggerActor();
        float halfW = 1.0f;
        float halfH = 1.0f;
        damageTrigger.configure("damage_zone", halfW, halfH);
        damageTrigger.setDamagePerSecond(10f);

        // 2 metry na prawo od gracza
        Vector2 triggerPos = new Vector2(playerPos.x + 2f, playerPos.y);
        gameWorld.spawnActor(damageTrigger, triggerPos);
        Gdx.app.debug("GameScreen", "Damage trigger zespawnowany na: " + triggerPos);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        // F3 — przełącz debug rendering (hitboxy)
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugRenderSystem.toggle();
            Gdx.app.debug("GameScreen", "Debug rendering: " + (debugRenderSystem.isDebugEnabled() ? "ON" : "OFF"));
        }

        gameWorld.update(delta);

        // Tick GameMode i PlayerController
        gameMode.tick(delta);
        playerController.tick(delta);

        // HUD — aktualizacja i rendering
        hud.update(delta);
        hud.render();
    }

    @Override
    public void resize(int width, int height) {
        game.getViewport().update(width, height, true);
        hud.resize(width, height);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        Gdx.app.debug("GameScreen", "Dispose GameScreen...");
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
        }
        if (renderSystem != null) {
            renderSystem.dispose();
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
}

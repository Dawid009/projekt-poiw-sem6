package com.polsl.poiw.ui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.asset.MapAsset;
import com.polsl.poiw.engine.asset.SkinAsset;
import com.polsl.poiw.engine.render.CameraSystem;
import com.polsl.poiw.engine.render.RenderSystem;
import com.polsl.poiw.engine.tiled.TiledMapParser;
import com.polsl.poiw.engine.world.GameWorld;

/**
 * Główny ekran gry — łączy GameWorld, TiledMapParser, systemy Ashley i rendering.
 */
public class GameScreen extends ScreenAdapter {
    private final Main game;

    /** GameWorld: trzyma Engine + Box2D World + Actorzy */
    private GameWorld gameWorld;

    /** Parser map Tiled — ładuje .tmx i tworzy kolizje/obiekty */
    private TiledMapParser tiledParser;

    /** UI Stage — osobny viewport od kamery świata */
    private Stage stage;

    /** Skin UI (Scene2D) */
    private Skin skin;

    /** Referencje do systemów — potrzebne w show() do ustawienia mapy */
    private RenderSystem renderSystem;
    private CameraSystem cameraSystem;

    public GameScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.app.debug("GameScreen", "Inicjalizacja GameScreen...");

        // 1. GameWorld — tworzy wewnętrznie Ashley Engine + Box2D World
        this.gameWorld = new GameWorld();

        // 2. Dodaj systemy do Engine PRZEZ GameWorld
        addSystems();

        // 3. TiledMapParser — parsuje mapę, dostaje AssetService do ładowania
        this.tiledParser = new TiledMapParser(gameWorld, game.getAssetService());

        // 4. Załaduj i sparsuj mapę
        TiledMap map = tiledParser.loadMap(MapAsset.MAIN);
        tiledParser.parse(map);

        // 5. Przekaż mapę do systemów renderujących
        renderSystem.setMap(map);
        cameraSystem.setMap(map);

        // 6. UI Stage — osobny viewport (do przyszłego HUD)
        this.stage = new Stage(new FitViewport(320f, 180f), game.getBatch());
        this.skin = game.getAssetService().get(SkinAsset.DEFAULT);

        // Ustaw input
        game.setInputProcessors(stage);

        Gdx.app.debug("GameScreen", "GameScreen zainicjalizowany. Mapa załadowana.");
    }

    /**
     * Dodaje systemy Ashley do GameWorld.
     * Kolejność priorytetów:
     * - Systemy logiki (fizyka, AI, input) — niższy priorytet (wykonywane wcześniej)
     * - CameraSystem — priorytet 99 (po logice, przed renderem)
     * - RenderSystem — priorytet 100 (ostatni — rysuje)
     */
    private void addSystems() {
        // System kamery — centruje kamerę na mapie (w przyszłości: śledzi gracza)
        cameraSystem = new CameraSystem(game.getCamera());
        gameWorld.addSystem(cameraSystem);

        // System renderujący — rysuje warstwy kafelkowe mapy Tiled
        renderSystem = new RenderSystem(game.getBatch(), game.getViewport(), game.getCamera());
        gameWorld.addSystem(renderSystem);

        // W przyszłości dodaj tutaj:
        // gameWorld.addSystem(new PhysicMoveSystem());
        // gameWorld.addSystem(new PhysicSystem(gameWorld.getBox2dWorld(), 1f/60f));
        // gameWorld.addSystem(new AnimationSystem(game.getAssetService()));
        // gameWorld.addSystem(new ControllerSystem(game));
        // gameWorld.addSystem(new PhysicDebugRenderSystem(gameWorld.getBox2dWorld(), game.getCamera()));
    }

    @Override
    public void render(float delta) {
        // Zabezpieczenie: nie aktualizuj szybciej niż 30 FPS (unika spiral śmierci fizyki)
        delta = Math.min(delta, 1f / 30f);

        // GameWorld.update() wewnętrznie woła:
        //   box2dWorld.step() → fizyka
        //   actor.tick()     → logika każdego Actora
        //   ashleyEngine.update() → wszystkie systemy (CameraSystem, RenderSystem)
        gameWorld.update(delta);

        // Rysuj HUD (osobny viewport od kamery świata)
        stage.getViewport().apply();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        game.getViewport().update(width, height, true);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void dispose() {
        Gdx.app.debug("GameScreen", "Dispose GameScreen...");
        if (renderSystem != null) {
            renderSystem.dispose();
        }
        if (gameWorld != null) {
            gameWorld.dispose();
            gameWorld = null;
        }
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
    }
}

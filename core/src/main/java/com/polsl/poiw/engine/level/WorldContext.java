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
import com.polsl.poiw.engine.net.driver.NetDriver;
import com.polsl.poiw.engine.net.prediction.InterpolationSystem;
import com.polsl.poiw.engine.net.replication.ClientReplicationHandler;
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

    // ===== Networking (multiplayer client) =====
    private NetDriver netDriver;
    private ClientReplicationHandler replicationHandler;
    private InterpolationSystem interpolationSystem;
    private com.polsl.poiw.engine.net.prediction.NetworkClock networkClock;
    private com.polsl.poiw.engine.net.prediction.ClientPrediction clientPrediction;

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

        // in multiplayer: client generates negative IDs BEFORE loading the map
        // so that actors from TiledMap dont collide with positive IDs from the server
        if (game.getGameInstance().isMultiplayer() && levelDef.isGameWorld()) {
            com.polsl.poiw.engine.actor.ActorIdGenerator.setServerMode(false);
        }

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

        // Multiplayer client: connect with server
        if (game.getGameInstance().isMultiplayer() && levelDef.isGameWorld()) {
            initializeMultiplayer();
        }

        initialized = true;
        Gdx.app.debug(TAG, "Zainicjalizowano: " + levelDef.getLevelId());
    }

    /**
     * re-uses the existing NetDriver from GameInstance (connection already established).
     * sets up replication handler, actor factory, and gameplay message handlers.
     * connection was already made in GameInstance.connectToServer() — we only attach gameplay handlers here.
     */
    private void initializeMultiplayer() {
        var gi = game.getGameInstance();

        // re-use NetDriver from GameInstance (created during connectToServer())
        netDriver = gi.getNetDriver();
        if (netDriver == null) {
            Gdx.app.error(TAG, "NetDriver is null — connection was not established");
            return;
        }

        replicationHandler = new ClientReplicationHandler(gameWorld, gi.getLocalPlayerId());

        // actor factory — client configures actors with atlas (sprite, collision etc.)
        replicationHandler.setActorFactory((actorClass, initialProps) -> {
            try {
                Class<?> clazz = Class.forName(actorClass);
                var actor = (com.polsl.poiw.engine.actor.AbstractActor) clazz.getDeclaredConstructor().newInstance();
                if (actor instanceof com.polsl.poiw.gameplay.character.PlayerCharacter pc) {
                    TextureAtlas atlas = game.getAssetService().get(AtlasAsset.OBJECTS);
                    pc.configure(atlas);
                }
                return actor;
            } catch (Exception e) {
                Gdx.app.error(TAG, "Cannot create actor: " + actorClass, e);
                return null;
            }
        });

        // attach InterpolationSystem so that replicationHandler can feed snapshots
        if (interpolationSystem != null) {
            replicationHandler.setInterpolationSystem(interpolationSystem);
        }

        // override message handler — gameplay messages (ActorSpawn, BatchUpdate, etc.)
        netDriver.setMessageHandler((connectionId, message) -> handleNetworkMessage(message));
        netDriver.setDisconnectHandler(connectionId -> {
            Gdx.app.log(TAG, "Rozłączono z serwerem w trakcie gry");
            gi.returnToMenu("Utracono polaczenie z serwerem");
        });

        // give PlayerController access to ClientPrediction for saveMove()
        if (clientPrediction != null) {
            playerController.setClientPrediction(clientPrediction);
        }
    }


    // handles network messages on the client during gameplay.
    private void handleNetworkMessage(Object message) {
        var gi = game.getGameInstance();

        if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.ServerAccept accept) {
            // ServerAccept already handled in GameInstance.connectToServer() —
            // but keep graceful handling if WorldContext receives it (e.g. after travel)
            gi.setLocalPlayerId(accept.assignedPlayerId);
            gi.setServerTime(accept.serverTime);
            playerController.setPlayerId(accept.assignedPlayerId);
            var oldFactory = replicationHandler.getActorFactory();
            var oldInterp = replicationHandler.getInterpolationSystem();
            replicationHandler = new ClientReplicationHandler(gameWorld, accept.assignedPlayerId);
            if (oldFactory != null) replicationHandler.setActorFactory(oldFactory);
            if (oldInterp != null) replicationHandler.setInterpolationSystem(oldInterp);
            Gdx.app.log(TAG, "Zaakceptowany przez serwer, playerId=" + accept.assignedPlayerId);

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.ServerReject reject) {
            Gdx.app.error(TAG, "Serwer odrzucił połączenie: " + reject.reason);
            gi.returnToMenu("Serwer odrzucil: " + reject.reason);

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.ActorSpawn spawn) {
            replicationHandler.handleActorSpawn(spawn);
            var actor = gameWorld.getActorById(spawn.actorId);
            if (actor != null) {
                if (spawn.ownerId == gi.getLocalPlayerId()) {
                    playerController.possess(actor);
                } else {
                    actor.removeComponent(com.polsl.poiw.engine.component.CameraFollowComponent.class);
                }
            }

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.ActorDestroy destroy) {
            if (interpolationSystem != null) interpolationSystem.removeInterpolator(destroy.actorId);
            replicationHandler.handleActorDestroy(destroy);

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.BatchReplicationUpdate batch) {
            replicationHandler.handleBatchUpdate(batch);

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.BatchMovementSnapshot moveBatch) {
            handleMovementBatch(moveBatch);

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.ServerPositionCorrection correction) {
            var actor = gameWorld.getActorById(correction.actorId);
            if (actor == null) return;

            if (actor.getNetRole() == com.polsl.poiw.engine.actor.NetRole.AUTONOMOUS_PROXY) {
                // reconciliation for local player
                if (clientPrediction != null) {
                    var collision = actor.getComponentByType(
                        com.polsl.poiw.engine.collision.CollisionComponent.class);
                    if (collision != null && collision.getBody() != null) {
                        clientPrediction.reconcile(correction.x, correction.y,
                            correction.velX, correction.velY,
                            correction.lastProcessedInput, collision.getBody());
                    }
                }
            } else if (actor.getNetRole() == com.polsl.poiw.engine.actor.NetRole.SIMULATED_PROXY) {
                // feed interpolation for remote players
                if (interpolationSystem != null) {
                    interpolationSystem.addSnapshot(correction.actorId, correction.serverTime,
                        correction.x, correction.y, correction.velX, correction.velY);
                }
            }

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.Pong pong) {
            float ping = (System.currentTimeMillis() - pong.clientTimestamp) / 1000f;
            gi.setServerTime(pong.serverTimestamp / 1000f);

        } else if (message instanceof com.polsl.poiw.shared.protocol.NetworkProtocol.ServerTravel travel) {
            Gdx.app.log(TAG, "ServerTravel received: " + travel.levelId);
            gi.clientTravel(travel.levelId, gi.getServerHost());
        }
    }

    /**
     * obsługuje batch snapshotów ruchu z serwera (UDP).
     * SIMULATED_PROXY → InterpolationSystem (remote players).
     * AUTONOMOUS_PROXY nie jest tutaj obsługiwany — reconciliation przez ServerPositionCorrection.
     */
    private void handleMovementBatch(com.polsl.poiw.shared.protocol.NetworkProtocol.BatchMovementSnapshot batch) {
        if (batch.snapshots == null) return;

        // update network clock with server time
        if (networkClock != null) {
            networkClock.onServerTimeReceived(batch.serverTime);
        }

        for (var snapshot : batch.snapshots) {
            var actor = gameWorld.getActorById(snapshot.actorId);
            if (actor == null) continue;

            if (actor.getNetRole() == com.polsl.poiw.engine.actor.NetRole.SIMULATED_PROXY) {
                // feed interpolation system
                if (interpolationSystem != null) {
                    interpolationSystem.addSnapshot(snapshot.actorId, batch.serverTime,
                        snapshot.x, snapshot.y, snapshot.velX, snapshot.velY);
                }
            }
            // AUTONOMOUS_PROXY — skip; reconciliation is handled by ServerPositionCorrection
        }
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

        // in multiplayer the client adds InterpolationSystem + NetworkClock + ClientPrediction
        if (game.getGameInstance().isMultiplayer()) {
            networkClock = new com.polsl.poiw.engine.net.prediction.NetworkClock();
            clientPrediction = new com.polsl.poiw.engine.net.prediction.ClientPrediction();
            interpolationSystem = new InterpolationSystem();
            interpolationSystem.setNetworkClock(networkClock);
            gameWorld.addSystem(interpolationSystem);
        }

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

        // update network clock
        if (networkClock != null) {
            networkClock.update(delta);
        }

        // process network messages (main thread!)
        if (netDriver != null) {
            netDriver.processMessages();
        }

        // F3 — debug rendering (tylko w GAME world)
        if (debugRenderSystem != null && Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugRenderSystem.toggle();
            if (fpsDebugText != null) {
                fpsDebugText.setVisibility(debugRenderSystem.isDebugEnabled()
                    ? EVisibility.VISIBLE
                    : EVisibility.HIDDEN);
            }
        }

        // F4 - network debug info
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4) && gameWorld != null) {
            var gi = game.getGameInstance();
            Gdx.app.log(TAG, "=== NETWORK DEBUG ===");
            Gdx.app.log(TAG, "PlayerId=" + gi.getLocalPlayerId()
                + " connected=" + gi.isConnected()
                + " multiplayer=" + gi.isMultiplayer());
            int totalActors = 0;
            for (var a : gameWorld.getAllActors()) {
                totalActors++;
                var p = a.getPosition();
                boolean hasSprite = a.getComponentByType(
                    com.polsl.poiw.engine.component.SpriteComponent.class) != null;
                boolean hasTransform = a.getComponentByType(
                    com.polsl.poiw.engine.component.TransformComponent.class) != null;
                int compCount = 0;
                StringBuilder compNames = new StringBuilder();
                for (var comp : a.getAshleyEntity().getComponents()) {
                    compCount++;
                    if (compNames.length() > 0) compNames.append(", ");
                    compNames.append(comp.getClass().getSimpleName());
                }
                Gdx.app.log(TAG, "  Actor #" + a.getActorId()
                    + " class=" + a.getClass().getSimpleName()
                    + " role=" + a.getNetRole()
                    + " owner=" + a.getOwnerId()
                    + " pos=(" + String.format("%.2f", p.x) + "," + String.format("%.2f", p.y) + ")"
                    + " sprite=" + hasSprite
                    + " transform=" + hasTransform
                    + " comps(" + compCount + ")=[" + compNames + "]");
            }
            Gdx.app.log(TAG, "Total actors in world: " + totalActors);
            if (networkClock != null) {
                Gdx.app.log(TAG, "NetworkClock renderTime=" + networkClock.getRenderTime());
            }
            if (netDriver != null) {
                var ping = new com.polsl.poiw.shared.protocol.NetworkProtocol.Ping();
                ping.clientTimestamp = System.currentTimeMillis();
                netDriver.sendToServer(ping, false);
                Gdx.app.debug(TAG, "Sent debug ping to server");
            }
            Gdx.app.log(TAG, "=== END NETWORK DEBUG ===");
        }

        // GameWorld tick (fizyka, aktorzy, systemy Ashley)
        if (gameWorld != null) {
            gameWorld.update(delta);
        }

        // GameMode i PlayerController tick
        if (gameMode != null) gameMode.tick(delta);
        if (playerController != null) playerController.tick(delta);

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
        // NetDriver is owned by GameInstance — do not dispose here
        netDriver = null;
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

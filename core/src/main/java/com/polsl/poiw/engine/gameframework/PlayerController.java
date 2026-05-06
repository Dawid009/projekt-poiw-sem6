package com.polsl.poiw.engine.gameframework;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.collision.CollisionComponent;
import com.polsl.poiw.engine.component.ControllerComponent;
import com.polsl.poiw.engine.net.prediction.ClientPrediction;
import com.polsl.poiw.engine.ui.HUD;
import com.polsl.poiw.engine.ui.UserWidget;
import com.polsl.poiw.engine.world.GameWorld;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.ArrayList;
import java.util.List;

/**
 * PlayerController — zarządza lokalnym graczem.
 */
public class PlayerController {

    private static final String TAG = "PlayerController";
    private static final int ATTACK_INPUT_FLAG = 1 << 30;
    private static final int INPUT_SEQUENCE_MASK = ATTACK_INPUT_FLAG - 1;

    private GameInstance gameInstance;
    private GameWorld world;
    private GameMode gameMode;
    private HUD hud;
    private Skin skin;
    private TextureAtlas itemsAtlas;

    /** Kontrolowany aktor (possessed pawn) */
    private Actor possessedPawn;

    /** Widgety dodane do viewportu przez ten controller */
    private final List<UserWidget> managedWidgets = new ArrayList<>();

    /** ID lokalnego gracza */
    private int playerId = 0;

    // kryonet connection id (server-side, -1 = local/singleplayer)
    private int connectionId = -1;

    // sequential input number for prediction/reconciliation
    private int nextInputSequence = 0;

    // client-side prediction buffer (set by WorldContext in multiplayer)
    private ClientPrediction clientPrediction;

    public PlayerController() {
    }

    // ===== Lifecycle =====

    /** Wywoływane po stworzeniu controllera */
    public void initialize(GameInstance gameInstance, GameWorld world, GameMode gameMode,
                           HUD hud, Skin skin, TextureAtlas itemsAtlas) {
        this.gameInstance = gameInstance;
        this.world = world;
        this.gameMode = gameMode;
        this.hud = hud;
        this.skin = skin;
        this.itemsAtlas = itemsAtlas;
        setupHUD();
    }

    /** Konfiguracja początkowego HUD. Override w subklasach. */
    protected void setupHUD() {
    }

    /** Aktualizacja co klatkę. Override w subklasach. */
    public void tick(float delta) {
        // in multiplayer: send current input to server every frame
        if (gameInstance != null && gameInstance.isClient() && possessedPawn != null) {
            var move = possessedPawn.getComponent(
                com.polsl.poiw.engine.component.MovementComponent.class);
            if (move != null) {
                float dirX = move.getDirection().x;
                float dirY = move.getDirection().y;
                boolean attackPressed = consumeLocalAttackPressed();
                int sequenceNumber = sendInputToServer(dirX, dirY, attackPressed);

                // save predicted position for reconciliation
                if (clientPrediction != null && sequenceNumber >= 0) {
                    CollisionComponent coll = possessedPawn.getComponentByType(CollisionComponent.class);
                    if (coll != null && coll.getBody() != null) {
                        Vector2 bodyPos = coll.getBody().getPosition();
                        clientPrediction.saveMove(sequenceNumber, dirX, dirY, bodyPos.x, bodyPos.y);
                    }
                }
            }
        }
    }

    /** Renderowanie wykonywane przed HUD-em. Override w subklasach, jeśli potrzebny jest screen-space pass. */
    public void renderBeforeHud() {
    }

    /** Sprzątanie przy zamykaniu */
    public void destroy() {
        unpossess();
        removeAllWidgets();
    }

    // ===== Possess / Unpossess =====

    /**
     * Przejmuje kontrolę nad aktorem (possess).
     * Poprzednio kontrolowany aktor jest zwalniany.
     */
    public void possess(Actor pawn) {
        if (possessedPawn != null) {
            unpossess();
        }
        this.possessedPawn = pawn;
        pawn.setOwnerId(playerId);
        Gdx.app.debug(TAG, "Possess: Actor #" + pawn.getActorId());
        onPossess(pawn);
    }

    /** Zwalnia kontrolę nad aktorem */
    public void unpossess() {
        if (possessedPawn != null) {
            Gdx.app.debug(TAG, "Unpossess: Actor #" + possessedPawn.getActorId());
            possessedPawn = null;
            onUnpossess();
        }
    }

    /** Reakcja na possess — override w subklasach */
    protected void onPossess(Actor pawn) {
    }

    /** Reakcja na unpossess — override w subklasach */
    protected void onUnpossess() {
    }

    // ===== Widget Management =====

    /**
     * Dodaje widget do viewportu HUD.
     * Widget jest śledzony i automatycznie usuwany przy destroy().
     */
    public void addWidgetToViewport(UserWidget widget) {
        if (hud == null) {
            Gdx.app.error(TAG, "Brak HUD — nie można dodać widgetu do viewportu");
            return;
        }
        managedWidgets.add(widget);
        hud.addToViewport(widget);
    }

    /** Usuwa widget z viewportu HUD */
    public void removeWidgetFromViewport(UserWidget widget) {
        if (hud == null) return;
        managedWidgets.remove(widget);
        hud.removeFromViewport(widget);
    }

    /** Usuwa wszystkie widgety dodane przez ten controller */
    public void removeAllWidgets() {
        if (hud == null) return;
        for (UserWidget widget : new ArrayList<>(managedWidgets)) {
            hud.removeFromViewport(widget);
        }
        managedWidgets.clear();
    }

    // ===== Gettery =====

    public Actor getPossessedPawn() { return possessedPawn; }
    public GameInstance getGameInstance() { return gameInstance; }
    public GameWorld getWorld() { return world; }
    public GameMode getGameMode() { return gameMode; }
    public HUD getHUD() { return hud; }
    public Skin getSkin() { return skin; }
    public TextureAtlas getItemsAtlas() { return itemsAtlas; }
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }

    public int getConnectionId() { return connectionId; }
    public void setConnectionId(int connectionId) { this.connectionId = connectionId; }

    public void setClientPrediction(ClientPrediction prediction) { this.clientPrediction = prediction; }

    // ===== Networking =====

    // on server: called when client sends input every update
    // applies movement direction to the posesed pawn
    public void receiveClientInput(float dirX, float dirY, int sequence) {
        if (possessedPawn == null) return;
        var move = possessedPawn.getComponent(
            com.polsl.poiw.engine.component.MovementComponent.class);
        if (move != null) {
            move.getDirection().set(dirX, dirY);
        }
    }

    // sends current input to server on the client
    public int sendInputToServer(float dirX, float dirY, boolean attackPressed) {
        if (gameInstance == null || !gameInstance.isClient()) return -1;
        var netDriver = gameInstance.getNetDriver();
        if (netDriver == null) return -1;

        int sequenceNumber = nextInputSequence;
        nextInputSequence = (nextInputSequence + 1) & INPUT_SEQUENCE_MASK;

        var msg = new NetworkProtocol.ClientInputUpdate();
        msg.playerId = playerId;
        msg.dirX = dirX;
        msg.dirY = dirY;
        msg.sequenceNumber = attackPressed ? sequenceNumber | ATTACK_INPUT_FLAG : sequenceNumber;
        msg.timestamp = gameInstance.getServerTime();
        netDriver.sendToServer(msg, attackPressed);
        return sequenceNumber;
    }

    private boolean consumeLocalAttackPressed() {
        if (possessedPawn == null) {
            return false;
        }

        ControllerComponent controller = possessedPawn.getComponent(ControllerComponent.class);
        return controller != null && controller.consumeAttackInputTrigger();
    }
}

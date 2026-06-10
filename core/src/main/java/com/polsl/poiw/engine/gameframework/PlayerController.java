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

/** Lokalny kontroler gracza. */
public class PlayerController {

    private static final String TAG = "PlayerController";
    private static final int INPUT_SEQUENCE_MASK = (1 << 30) - 1;
    private static final float INPUT_RESEND_INTERVAL = 1f / 30f;

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
    private float inputResendTimer = 0f;
    private float lastSentDirX = Float.NaN;
    private float lastSentDirY = Float.NaN;
    private boolean lastSentSprinting;

    // client-side prediction buffer (set by WorldContext in multiplayer)
    private ClientPrediction clientPrediction;

    public PlayerController() {
    }

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

    protected void setupHUD() {
    }

    public void tick(float delta) {
        if (gameInstance != null && gameInstance.isClient() && possessedPawn != null) {
            var move = possessedPawn.getComponent(
                com.polsl.poiw.engine.component.MovementComponent.class);
            if (move != null) {
                float dirX = move.getDirection().x;
                float dirY = move.getDirection().y;
                boolean sprinting = move.isSprinting();
                boolean attackPressed = consumeLocalAttackPressed();
                if (attackPressed) {
                    sendAttackRequestToServer();
                }

                inputResendTimer += delta;
                if (!shouldSendInput(dirX, dirY, sprinting)) {
                    return;
                }

                int sequenceNumber = sendInputToServer(dirX, dirY, sprinting);
                rememberSentInput(dirX, dirY, sprinting);

                if (clientPrediction != null && sequenceNumber >= 0) {
                    CollisionComponent coll = possessedPawn.getComponentByType(CollisionComponent.class);
                    if (coll != null && coll.getBody() != null) {
                        Vector2 bodyPos = coll.getBody().getPosition();
                        Vector2 velocity = coll.getBody().getLinearVelocity();
                        clientPrediction.saveMove(
                            sequenceNumber,
                            dirX,
                            dirY,
                            sprinting,
                            bodyPos.x,
                            bodyPos.y,
                            velocity.x,
                            velocity.y
                        );
                    }
                }
            }
        }
    }

    public void renderBeforeHud() {
    }

    public void destroy() {
        unpossess();
        removeAllWidgets();
    }

    public void possess(Actor pawn) {
        if (possessedPawn != null) {
            unpossess();
        }
        this.possessedPawn = pawn;
        resetInputSyncState();
        pawn.setOwnerId(playerId);
        Gdx.app.debug(TAG, "Possess: Actor #" + pawn.getActorId());
        onPossess(pawn);
    }

    public void unpossess() {
        if (possessedPawn != null) {
            Gdx.app.debug(TAG, "Unpossess: Actor #" + possessedPawn.getActorId());
            possessedPawn = null;
            resetInputSyncState();
            onUnpossess();
        }
    }

    protected void onPossess(Actor pawn) {
    }

    protected void onUnpossess() {
    }

    public void addWidgetToViewport(UserWidget widget) {
        if (hud == null) {
            Gdx.app.error(TAG, "Brak HUD — nie można dodać widgetu do viewportu");
            return;
        }
        managedWidgets.add(widget);
        hud.addToViewport(widget);
    }

    public void removeWidgetFromViewport(UserWidget widget) {
        if (hud == null) return;
        managedWidgets.remove(widget);
        hud.removeFromViewport(widget);
    }

    public void removeAllWidgets() {
        if (hud == null) return;
        for (UserWidget widget : new ArrayList<>(managedWidgets)) {
            hud.removeFromViewport(widget);
        }
        managedWidgets.clear();
    }

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

    public void receiveClientInput(float dirX, float dirY, boolean sprinting, int sequence) {
        if (possessedPawn == null) return;
        var move = possessedPawn.getComponent(
            com.polsl.poiw.engine.component.MovementComponent.class);
        if (move != null) {
            move.getDirection().set(dirX, dirY);
            move.setSprinting(sprinting);
        }
    }

    public int sendInputToServer(float dirX, float dirY, boolean sprinting) {
        if (gameInstance == null || !gameInstance.isClient()) return -1;
        var netDriver = gameInstance.getNetDriver();
        if (netDriver == null) return -1;

        int sequenceNumber = nextInputSequence;
        nextInputSequence = (nextInputSequence + 1) & INPUT_SEQUENCE_MASK;

        var msg = new NetworkProtocol.ClientInputUpdate();
        msg.playerId = playerId;
        msg.dirX = dirX;
        msg.dirY = dirY;
        msg.sprinting = sprinting;
        msg.sequenceNumber = sequenceNumber;
        msg.timestamp = gameInstance.getServerTime();
        netDriver.sendToServer(msg, false);
        return sequenceNumber;
    }

    private void sendAttackRequestToServer() {
        if (gameInstance == null || !gameInstance.isClient()) {
            return;
        }

        var netDriver = gameInstance.getNetDriver();
        if (netDriver == null) {
            return;
        }

        var msg = new NetworkProtocol.ClientAttackRequest();
        msg.playerId = playerId;
        netDriver.sendToServer(msg, true);
    }

    private boolean shouldSendInput(float dirX, float dirY, boolean sprinting) {
        if (Float.isNaN(lastSentDirX) || Float.isNaN(lastSentDirY)) {
            return true;
        }

        boolean changed = Math.abs(dirX - lastSentDirX) > 0.0001f
            || Math.abs(dirY - lastSentDirY) > 0.0001f
            || sprinting != lastSentSprinting;
        if (changed) {
            return true;
        }

        boolean moving = Math.abs(dirX) > 0.0001f || Math.abs(dirY) > 0.0001f || sprinting;
        return moving && inputResendTimer >= INPUT_RESEND_INTERVAL;
    }

    private void rememberSentInput(float dirX, float dirY, boolean sprinting) {
        lastSentDirX = dirX;
        lastSentDirY = dirY;
        lastSentSprinting = sprinting;
        inputResendTimer = 0f;
    }

    private void resetInputSyncState() {
        inputResendTimer = 0f;
        lastSentDirX = Float.NaN;
        lastSentDirY = Float.NaN;
        lastSentSprinting = false;
    }

    private boolean consumeLocalAttackPressed() {
        if (possessedPawn == null) {
            return false;
        }

        ControllerComponent controller = possessedPawn.getComponent(ControllerComponent.class);
        return controller != null && controller.consumeAttackInputTrigger();
    }
}

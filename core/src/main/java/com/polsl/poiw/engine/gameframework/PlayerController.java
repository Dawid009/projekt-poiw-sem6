package com.polsl.poiw.engine.gameframework;

import com.badlogic.gdx.Gdx;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.ui.HUD;
import com.polsl.poiw.engine.ui.UserWidget;
import com.polsl.poiw.engine.world.GameWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * PlayerController — zarządza lokalnym graczem.
 * <p>
 * Odpowiada za:
 * <ul>
 *   <li>Posiadanie (possess) aktora gracza — kontrolowany Pawn</li>
 *   <li>Dostęp do HUD i dodawanie/usuwanie widgetów z viewportu</li>
 *   <li>Przechowywanie referencji do GameWorld i GameMode</li>
 * </ul>
 * <p>
 */
public class PlayerController {

    private static final String TAG = "PlayerController";

    private GameWorld world;
    private GameMode gameMode;
    private HUD hud;

    /** Kontrolowany aktor (possessed pawn) */
    private Actor possessedPawn;

    /** Widgety dodane do viewportu przez ten controller */
    private final List<UserWidget> managedWidgets = new ArrayList<>();

    /** ID lokalnego gracza */
    private int playerId = 0;

    public PlayerController() {
    }

    // ===== Lifecycle =====

    /** Wywoływane po stworzeniu controllera przez GameMode */
    public void initialize(GameWorld world, GameMode gameMode, HUD hud) {
        this.world = world;
        this.gameMode = gameMode;
        this.hud = hud;
        setupHUD();
    }

    /** Konfiguracja początkowego HUD. Override w subklasach. */
    protected void setupHUD() {
    }

    /** Aktualizacja co klatkę. Override w subklasach. */
    public void tick(float delta) {
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
            Actor old = possessedPawn;
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
    public GameWorld getWorld() { return world; }
    public GameMode getGameMode() { return gameMode; }
    public HUD getHUD() { return hud; }
    public int getPlayerId() { return playerId; }
    public void setPlayerId(int playerId) { this.playerId = playerId; }
}

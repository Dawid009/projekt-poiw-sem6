package com.polsl.poiw.engine.auth;

import com.badlogic.gdx.Gdx;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.level.WorldContext;

/**
 * Mały mostek między gameplayem a statystykami konta.
 * Sprawdza, czy dany efekt został wywołany przez lokalnego gracza i wtedy dopisuje statystyki.
 */
public final class GameplayStatsBridge {

    private GameplayStatsBridge() {
    }

    /** Rejestruje sciecie drzewa, ale tylko jesli zrobil to lokalny gracz. */
    public static void recordTreeCut(Actor actor) {
        recordIfKilledByLocalPlayer(actor, StatType.TREE);
    }

    /** Rejestruje zabicie przeciwnika przez lokalnego gracza. */
    public static void recordEnemyKill(Actor actor) {
        recordIfKilledByLocalPlayer(actor, StatType.ENEMY);
    }

    /** Rejestruje zabicie zwierzecia przez lokalnego gracza. */
    public static void recordAnimalKill(Actor actor) {
        recordIfKilledByLocalPlayer(actor, StatType.ANIMAL);
    }

    /**
     * Sprawdza, czy ostatni cios dobijajacy wyszedl od lokalnego gracza.
     * Jesli tak, przekazuje zdarzenie do AuthService i zlicza odpowiednia statystyke.
     */
    private static void recordIfKilledByLocalPlayer(Actor actor, StatType statType) {
        AuthService authService = resolveAuthService();
        if (authService == null || !authService.isAuthenticated() || actor == null) {
            return;
        }

        HealthComponent health = actor.getComponent(HealthComponent.class);
        int localOwnerId = resolveLocalGameplayOwnerId();
        if (health == null || localOwnerId < 0 || health.getLastDamageOwnerId() != localOwnerId) {
            return;
        }

        if (statType == StatType.TREE) {
            authService.recordTreeCut();
        } else if (statType == StatType.ENEMY) {
            authService.recordEnemyKill();
        } else if (statType == StatType.ANIMAL) {
            authService.recordAnimalKill();
        }
    }

    /** Zwraca aktywny AuthService z aktualnej instancji gry. */
    private static AuthService resolveAuthService() {
        GameInstance gameInstance = resolveGameInstance();
        return gameInstance != null ? gameInstance.getAuthService() : null;
    }

    /** Pobiera ID gracza, ktory aktualnie kontroluje postac w aktywnym swiecie. */
    private static int resolveLocalGameplayOwnerId() {
        GameInstance gameInstance = resolveGameInstance();
        if (gameInstance == null) {
            return -1;
        }

        WorldContext context = gameInstance.getActiveWorldContext();
        if (context == null) {
            return -1;
        }

        PlayerController playerController = context.getPlayerController();
        if (playerController == null) {
            return -1;
        }

        Actor pawn = playerController.getPossessedPawn();
        if (pawn != null && pawn.getOwnerId() >= 0) {
            return pawn.getOwnerId();
        }

        int playerId = playerController.getPlayerId();
        return Math.max(playerId, -1);
    }

    /** Zwraca glowna instancje gry, jesli aplikacja jest juz uruchomiona. */
    private static GameInstance resolveGameInstance() {
        if (Gdx.app == null || !(Gdx.app.getApplicationListener() instanceof Main main)) {
            return null;
        }

        return main.getGameInstance();
    }

    /** Typy statystyk obslugiwane przez ten bridge. */
    private enum StatType {
        TREE,
        ENEMY,
        ANIMAL
    }
}

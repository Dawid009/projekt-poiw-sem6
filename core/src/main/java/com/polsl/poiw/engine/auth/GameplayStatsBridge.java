package com.polsl.poiw.engine.auth;

import com.badlogic.gdx.Gdx;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.component.HealthComponent;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.level.WorldContext;

public final class GameplayStatsBridge {

    private GameplayStatsBridge() {
    }

    public static void recordTreeCut(Actor actor) {
        recordIfKilledByLocalPlayer(actor, StatType.TREE);
    }

    public static void recordEnemyKill(Actor actor) {
        recordIfKilledByLocalPlayer(actor, StatType.ENEMY);
    }

    public static void recordAnimalKill(Actor actor) {
        recordIfKilledByLocalPlayer(actor, StatType.ANIMAL);
    }

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

    private static AuthService resolveAuthService() {
        GameInstance gameInstance = resolveGameInstance();
        return gameInstance != null ? gameInstance.getAuthService() : null;
    }

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

    private static GameInstance resolveGameInstance() {
        if (Gdx.app == null || !(Gdx.app.getApplicationListener() instanceof Main main)) {
            return null;
        }

        return main.getGameInstance();
    }

    private enum StatType {
        TREE,
        ENEMY,
        ANIMAL
    }
}
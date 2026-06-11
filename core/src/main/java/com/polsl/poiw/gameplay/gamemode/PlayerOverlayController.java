package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.auth.AuthService;
import com.polsl.poiw.engine.ui.DeathMenuWidget;
import com.polsl.poiw.engine.ui.EVisibility;
import com.polsl.poiw.engine.ui.InventoryPanelWidget;
import com.polsl.poiw.engine.ui.PauseMenuWidget;
import com.polsl.poiw.engine.ui.SettingsPanelWidget;
import com.polsl.poiw.engine.ui.StatsPanelWidget;
import com.polsl.poiw.engine.ui.TextBlock;

final class PlayerOverlayController {
    private static final float SAVE_STATUS_DURATION_SECONDS = 2.2f;

    private final MainPlayerController owner;
    private final InventoryPanelWidget inventoryPanel;
    private final PauseMenuWidget pauseMenu;
    private final SettingsPanelWidget settingsPanel;
    private final StatsPanelWidget statsPanel;
    private final DeathMenuWidget deathMenu;
    private final TextBlock saveStatusText;
    private final Runnable clearInteractionPanels;

    private float saveStatusTimer;

    PlayerOverlayController(MainPlayerController owner,
                            InventoryPanelWidget inventoryPanel,
                            PauseMenuWidget pauseMenu,
                            SettingsPanelWidget settingsPanel,
                            StatsPanelWidget statsPanel,
                            DeathMenuWidget deathMenu,
                            TextBlock saveStatusText,
                            Runnable clearInteractionPanels) {
        this.owner = owner;
        this.inventoryPanel = inventoryPanel;
        this.pauseMenu = pauseMenu;
        this.settingsPanel = settingsPanel;
        this.statsPanel = statsPanel;
        this.deathMenu = deathMenu;
        this.saveStatusText = saveStatusText;
        this.clearInteractionPanels = clearInteractionPanels;
    }

    void updateSaveStatus(float delta) {
        if (saveStatusText == null || !saveStatusText.isVisible() || delta <= 0f) {
            return;
        }

        saveStatusTimer = Math.max(0f, saveStatusTimer - delta);
        if (saveStatusTimer <= 0f) {
            saveStatusText.setVisibility(EVisibility.HIDDEN);
        }
    }

    void handlePauseToggle() {
        if (deathMenu != null && deathMenu.isVisible()) {
            return;
        }

        if (Gdx.input == null || !Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            return;
        }

        if (statsPanel != null && statsPanel.isVisible()) {
            closeStatsPanel();
            return;
        }

        if (settingsPanel != null && settingsPanel.isVisible()) {
            closeSettingsToPauseMenu();
            return;
        }

        if (pauseMenu != null && pauseMenu.isVisible()) {
            hidePauseMenu();
            return;
        }

        showPauseMenu();
    }

    boolean isOverlayVisible() {
        return (pauseMenu != null && pauseMenu.isVisible())
            || (settingsPanel != null && settingsPanel.isVisible())
            || (statsPanel != null && statsPanel.isVisible())
            || (deathMenu != null && deathMenu.isVisible());
    }

    void hidePauseMenu() {
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
    }

    void openSettingsFromPauseMenu() {
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.refreshFromAppliedSettings();
            settingsPanel.setVisibility(EVisibility.VISIBLE);
        }
    }

    void closeSettingsToPauseMenu() {
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.VISIBLE);
        }
    }

    void openStatsPanel() {
        if (statsPanel == null) {
            return;
        }

        hideInventory();
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }

        statsPanel.setVisibility(EVisibility.VISIBLE);
        refreshStatsPanel();
    }

    void closeStatsPanel() {
        if (statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null && isStatsAvailable()) {
            pauseMenu.setVisibility(EVisibility.VISIBLE);
        }
    }

    void refreshStatsPanel() {
        if (statsPanel == null) {
            return;
        }

        GameInstance gameInstance = owner.getGameInstance();
        AuthService authService = gameInstance != null ? gameInstance.getAuthService() : null;
        if (authService == null || !authService.isAuthenticated() || authService.isOfflineSession()) {
            statsPanel.showError("Statystyki sa dostepne tylko po zalogowaniu.");
            return;
        }

        statsPanel.showLoading();
        authService.fetchCurrentStats(new AuthService.StatsResultListener() {
            @Override
            public void onSuccess(AuthService.PlayerStatsSnapshot stats) {
                if (statsPanel == null || !statsPanel.isAddedToViewport()) {
                    return;
                }
                statsPanel.setStats(stats);
            }

            @Override
            public void onFailure(String message) {
                if (statsPanel == null || !statsPanel.isAddedToViewport()) {
                    return;
                }
                statsPanel.showError(message);
            }
        });
    }

    void updateStatsUiState() {
        boolean statsAvailable = isStatsAvailable();
        if (pauseMenu != null) {
            pauseMenu.setStatsVisible(statsAvailable);
        }
        if (!statsAvailable && statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
        }
    }

    void quitToMainMenu() {
        GameInstance gameInstance = owner.getGameInstance();
        if (gameInstance != null) {
            gameInstance.returnToMenu("Wyjscie do menu glownego");
        }
    }

    void saveAndQuitToMainMenu() {
        saveGameProgress();
        quitToMainMenu();
    }

    void saveGameProgress() {
        GameInstance gameInstance = owner.getGameInstance();
        if (gameInstance == null || !gameInstance.isSinglePlayer()) {
            return;
        }

        boolean saved = gameInstance.getSinglePlayerSaveService().saveCurrentGame(gameInstance.getActiveWorldContext());
        if (saved) {
            Gdx.app.log("MainPlayerController", "Zapisano postep gry do slotu #"
                + (gameInstance.getSinglePlayerSaveService().getActiveSlotIndex() + 1));
            showSaveStatus("Zapisano gre", new Color(0.75f, 1f, 0.75f, 1f));
        } else {
            Gdx.app.error("MainPlayerController", "Nie udalo sie zapisac postepu gry");
            showSaveStatus("Blad zapisu", new Color(1f, 0.7f, 0.7f, 1f));
        }
    }

    void showDeathMenu() {
        hideInventory();
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (deathMenu != null) {
            deathMenu.setVisibility(EVisibility.VISIBLE);
        }
    }

    void hideDeathMenu() {
        if (deathMenu != null) {
            deathMenu.setVisibility(EVisibility.HIDDEN);
        }
    }

    boolean isDeathMenuVisible() {
        return deathMenu != null && deathMenu.isVisible();
    }

    void reset() {
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (deathMenu != null) {
            deathMenu.setVisibility(EVisibility.HIDDEN);
        }
        if (saveStatusText != null) {
            saveStatusText.setVisibility(EVisibility.HIDDEN);
        }
        saveStatusTimer = 0f;
    }

    private void showPauseMenu() {
        hideInventory();
        if (statsPanel != null) {
            statsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (pauseMenu != null) {
            pauseMenu.setVisibility(EVisibility.VISIBLE);
        }
    }

    private void hideInventory() {
        if (inventoryPanel != null) {
            inventoryPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (clearInteractionPanels != null) {
            clearInteractionPanels.run();
        }
    }

    private boolean isStatsAvailable() {
        GameInstance gameInstance = owner.getGameInstance();
        if (gameInstance == null) {
            return false;
        }

        AuthService authService = gameInstance.getAuthService();
        return authService != null && authService.isAuthenticated() && !authService.isOfflineSession();
    }

    private void showSaveStatus(String text, Color color) {
        if (saveStatusText == null) {
            return;
        }

        saveStatusText.setText(text);
        saveStatusText.setColor(color);
        saveStatusText.setVisibility(EVisibility.VISIBLE);
        saveStatusTimer = SAVE_STATUS_DURATION_SECONDS;
    }
}

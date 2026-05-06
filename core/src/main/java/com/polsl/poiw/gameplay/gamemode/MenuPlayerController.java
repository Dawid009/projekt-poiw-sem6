package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.auth.AuthService;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.ui.AuthPanelWidget;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.EVisibility;
import com.polsl.poiw.engine.ui.FullscreenBackgroundRenderer;
import com.polsl.poiw.engine.ui.SettingsPanelWidget;
import com.polsl.poiw.engine.ui.UiSkinStyles;
import com.polsl.poiw.engine.ui.UserWidget;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

/**
 * Controller menu głównego — tworzy widgety Play / Multiplayer / Options / Quit.
 * flow multiplayer: ustaw dane sesji → connectToServer → czekaj na ServerAccept → travel("game").
 */
public class MenuPlayerController extends PlayerController {

    private static final String TAG = "MenuPlayerController";
    private static final float CONTENT_FONT_SCALE = 0.5f;
    private static final float ACCOUNT_FONT_SCALE = 0.5f;
    private static final float ACCOUNT_BUTTON_WIDTH = 44f;
    private static final float ACCOUNT_BUTTON_HEIGHT = 12f;
    private static final float MENU_BUTTON_WIDTH = 55f;
    private static final float MENU_BUTTON_HEIGHT = 12f;
    private static final float PANEL_BUTTON_WIDTH = 55f;
    private static final float PANEL_BUTTON_HEIGHT = 12f;
    private static final float PANEL_FIELD_WIDTH = 55f;
    private static final float PANEL_FIELD_HEIGHT = 12f;

    private UserWidget menuContainer;
    private UserWidget multiplayerPanel;
    private UserWidget accountOverlay;
    private FullscreenBackgroundRenderer backgroundRenderer;
    private AuthPanelWidget authPanel;
    private TextField ipField;
    private TextField portField;
    private Label statusText;
    private Label accountNameLabel;
    private Label accountTimeLabel;
    private TextButton connectButton;
    private SettingsPanelWidget settingsPanel;

    @Override
    protected void setupHUD() {
        Skin skin = getSkin();

        backgroundRenderer = new FullscreenBackgroundRenderer("menu_bg.png");

        // Menu główne
        menuContainer = new UserWidget();
        menuContainer.setAnchor(EAnchor.CENTER);
        menuContainer.setAlignment(EAnchor.CENTER);
        Window menuWindow = new Window("Gra 2D", skin, "atlas");
        menuWindow.setMovable(false);
        UiSkinStyles.centerWindowTitle(menuWindow);

        Table menuContent = new Table();
        menuContent.defaults().width(MENU_BUTTON_WIDTH).height(MENU_BUTTON_HEIGHT);

        TextButton playButton = createMenuButton(skin, "Graj", MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onPlayClicked();
            }
        });
        menuContent.add(playButton).padBottom(1f).padTop(4f).row();

        TextButton multiplayerButton = createMenuButton(skin, "Multiplayer", MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
        multiplayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                openMultiplayerPanel();
            }
        });
        menuContent.add(multiplayerButton).padBottom(1f).row();

        TextButton optionsButton = createMenuButton(skin, "Opcje", MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onOptionsClicked();
            }
        });
        menuContent.add(optionsButton).row();

        TextButton quitButton = createMenuButton(skin, "Wyjdz", MENU_BUTTON_WIDTH, MENU_BUTTON_HEIGHT);
        quitButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onQuitClicked();
            }
        });
        menuContent.add(quitButton).padTop(10f);

        menuWindow.add(menuContent).pad(10f, 8f, 7f, 8f);
        menuWindow.pack();
        menuWindow.setSize(menuWindow.getPrefWidth(), menuWindow.getPrefHeight());
        menuContainer.setSize(menuWindow.getWidth(), menuWindow.getHeight());
        menuContainer.getRoot().addActor(menuWindow);

        addWidgetToViewport(menuContainer);

        accountOverlay = new UserWidget();
        accountOverlay.setAnchor(EAnchor.BOTTOM_LEFT);
        accountOverlay.setAlignment(EAnchor.BOTTOM_LEFT);
        accountOverlay.setOffset(4f, 4f);

        Table accountTable = new Table();
        accountTable.defaults().left();
        accountNameLabel = new Label("---", UiSkinStyles.copyScaledLabelStyle(skin, "font", ACCOUNT_FONT_SCALE));
        accountTimeLabel = new Label("00:00:00", UiSkinStyles.copyScaledLabelStyle(skin, "font", ACCOUNT_FONT_SCALE));
        TextButton logoutButton = new TextButton("Wyloguj",
            UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 18f, 14f, ACCOUNT_FONT_SCALE));
        logoutButton.getLabel().setWrap(false);
        logoutButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onLogoutClicked();
            }
        });

        accountTable.add(accountNameLabel).padBottom(1f).row();
        accountTable.add(accountTimeLabel).padBottom(1.5f).row();
        accountTable.add(logoutButton).width(ACCOUNT_BUTTON_WIDTH).height(ACCOUNT_BUTTON_HEIGHT).left();
        accountTable.pack();

        accountOverlay.setSize(accountTable.getPrefWidth(), accountTable.getPrefHeight());
        accountOverlay.getRoot().addActor(accountTable);
        addWidgetToViewport(accountOverlay);
        accountOverlay.setVisibility(EVisibility.COLLAPSED);

        // Panel Multiplayer
        multiplayerPanel = new UserWidget();
        multiplayerPanel.setAnchor(EAnchor.CENTER);
        multiplayerPanel.setAlignment(EAnchor.CENTER);
        multiplayerPanel.setVisibility(EVisibility.COLLAPSED);
        Window multiplayerWindow = new Window("Siec", skin, "atlas");
        multiplayerWindow.setMovable(false);
        UiSkinStyles.centerWindowTitle(multiplayerWindow);
        multiplayerWindow.getTitleLabel().setStyle(UiSkinStyles.resolveLabelStyle(skin, "font"));

        Label ipLabel = new Label("Adres", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
        Label portLabel = new Label("Port", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
        statusText = new Label("", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));
        statusText.setColor(Color.YELLOW);
        statusText.setVisible(false);

        ipField = new TextField("", UiSkinStyles.copyCompactTextFieldStyle(skin, "atlas", "font", 24f, 14f, CONTENT_FONT_SCALE));
        ipField.setMessageText("localhost");
        ipField.setText("localhost");
        ipField.setAlignment(com.badlogic.gdx.utils.Align.left);

        portField = new TextField(String.valueOf(NetworkProtocol.DEFAULT_TCP_PORT),
            UiSkinStyles.copyCompactTextFieldStyle(skin, "atlas", "font", 24f, 14f, CONTENT_FONT_SCALE));
        portField.setMessageText("54555");
        portField.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
        portField.setAlignment(com.badlogic.gdx.utils.Align.left);

        connectButton = createMenuButton(skin, "Polacz", PANEL_BUTTON_WIDTH, PANEL_BUTTON_HEIGHT);
        connectButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                onConnectClicked();
            }
        });

        TextButton backButton = createMenuButton(skin, "Powrot", PANEL_BUTTON_WIDTH, PANEL_BUTTON_HEIGHT);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                closeMultiplayerPanel();
            }
        });

        Table form = new Table();
        form.defaults().padBottom(4f);
        form.add(ipLabel).left().padRight(5f);
        form.add(ipField).width(PANEL_FIELD_WIDTH).height(PANEL_FIELD_HEIGHT).left().row();
        form.add(portLabel).left().padRight(5f);
        form.add(portField).width(PANEL_FIELD_WIDTH).height(PANEL_FIELD_HEIGHT).left().row();
        form.add(connectButton).colspan(2).width(PANEL_BUTTON_WIDTH).height(PANEL_BUTTON_HEIGHT).center().padTop(2f).row();
        form.add(statusText).colspan(2).center().padTop(1f).padBottom(1f).row();
        form.add(backButton).colspan(2).width(PANEL_BUTTON_WIDTH).height(PANEL_BUTTON_HEIGHT).center().padTop(2f);

        multiplayerWindow.add(form).pad(16f, 7f, 7f, 7f);
        multiplayerWindow.pack();
        multiplayerWindow.setSize(multiplayerWindow.getPrefWidth(), multiplayerWindow.getPrefHeight());
        multiplayerPanel.setSize(multiplayerWindow.getWidth(), multiplayerWindow.getHeight());
        multiplayerPanel.getRoot().addActor(multiplayerWindow);

        addWidgetToViewport(multiplayerPanel);

        settingsPanel = new SettingsPanelWidget(skin);
        settingsPanel.setAnchor(EAnchor.CENTER);
        settingsPanel.setAlignment(EAnchor.CENTER);
        settingsPanel.setCloseAction(this::closeSettingsPanel);
        addWidgetToViewport(settingsPanel);

        authPanel = new AuthPanelWidget(skin);
        authPanel.setAnchor(EAnchor.CENTER);
        authPanel.setAlignment(EAnchor.CENTER);
        authPanel.setActionListener(new AuthPanelWidget.AuthPanelActionListener() {
            @Override
            public void onLoginRequested(String login, String password) {
                MenuPlayerController.this.onLoginRequested(login, password);
            }

            @Override
            public void onRegisterRequested(String login, String email, String password) {
                MenuPlayerController.this.onRegisterRequested(login, email, password);
            }
        });
        addWidgetToViewport(authPanel);

        refreshAccountInfo();
        if (getGameInstance().getAuthService().isAuthenticated()) {
            showMainMenu();
        } else {
            showAuthPanel(null, null);
        }
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
        refreshAccountInfo();
    }

    @Override
    public void renderBeforeHud() {
        if (backgroundRenderer != null) {
            backgroundRenderer.render(getHUD().getStage().getBatch());
        }
    }

    @Override
    public void destroy() {
        if (backgroundRenderer != null) {
            backgroundRenderer.dispose();
            backgroundRenderer = null;
        }
        super.destroy();
    }


    private void onPlayClicked() {
        Gdx.app.debug(TAG, "Play → single-player");
        GameInstance gi = getGameInstance();
        if (gi != null) {
            gi.setMode(GameInstance.Mode.SINGLE_PLAYER);
            gi.travel("game");
        }
    }

    private void openMultiplayerPanel() {
        menuContainer.setVisibility(EVisibility.COLLAPSED);
        multiplayerPanel.setVisibility(EVisibility.VISIBLE);
    }

    private void onOptionsClicked() {
        openSettingsPanel();
    }

    private void onQuitClicked() {
        Gdx.app.debug(TAG, "Quit → zamykanie aplikacji");
        Gdx.app.exit();
    }

    //  connect flow: ustaw dane sesji → connectToServer → czekaj na ServerAccept
    private void onConnectClicked() {
        String ip = ipField.getText().trim();
        String portText = portField.getText().trim();

        if (ip.isEmpty()) {
            ip = "localhost";
        }

        int port = NetworkProtocol.DEFAULT_TCP_PORT;
        if (!portText.isEmpty()) {
            try {
                port = Integer.parseInt(portText);
                if (port < 1 || port > 65535) {
                    showStatus("Nieprawidlowy port: " + port, Color.RED);
                    return;
                }
            } catch (NumberFormatException e) {
                showStatus("Nieprawidlowy format portu", Color.RED);
                return;
            }
        }

        Gdx.app.debug(TAG, "Łączenie z " + ip + ":" + port);
        GameInstance gi = getGameInstance();
        if (gi != null) {
            gi.setMode(GameInstance.Mode.MULTIPLAYER);
            gi.setServerHost(ip);
            gi.setServerTcpPort(port);

            // zablokuj przycisk na czas łączenia
            connectButton.setDisabled(true);

            // rozpocznij asynchroniczne łączenie — ServerAccept → travel("game")
            gi.connectToServer(
                status -> showStatus(status, Color.YELLOW),
                error -> {
                    showStatus(error, Color.RED);
                    connectButton.setDisabled(false);
                }
            );
        }
    }

    /**
     * wyświetla status / błąd w panelu multiplayer
     */
    private void showStatus(String text, Color color) {
        if (statusText != null) {
            statusText.setText(text);
            statusText.setColor(color);
            statusText.setVisible(true);
        }
    }

    private void closeMultiplayerPanel() {
        multiplayerPanel.setVisibility(EVisibility.COLLAPSED);
        if (getGameInstance().getAuthService().isAuthenticated()) {
            menuContainer.setVisibility(EVisibility.VISIBLE);
        }
        // reset statusu i przycisku
        if (statusText != null) statusText.setVisible(false);
        if (connectButton != null) connectButton.setDisabled(false);
    }

    private void openSettingsPanel() {
        if (settingsPanel == null) {
            return;
        }

        settingsPanel.refreshFromAppliedSettings();
        settingsPanel.setVisibility(EVisibility.VISIBLE);
        menuContainer.setVisibility(EVisibility.HIDDEN);
        multiplayerPanel.setVisibility(EVisibility.COLLAPSED);
    }

    private void closeSettingsPanel() {
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (getGameInstance().getAuthService().isAuthenticated()) {
            menuContainer.setVisibility(EVisibility.VISIBLE);
        } else {
            showAuthPanel(null, null);
        }
    }

    private void onLoginRequested(String login, String password) {
        authPanel.setBusy(true);
        authPanel.setStatus("Logowanie...", Color.YELLOW);

        getGameInstance().getAuthService().login(login, password, new AuthService.AuthResultListener() {
            @Override
            public void onSuccess(AuthService.SessionSnapshot session) {
                authPanel.setBusy(false);
                authPanel.clearStatus();
                authPanel.resetToLoginMode();
                applyAuthenticatedSession(session);
            }

            @Override
            public void onFailure(String message) {
                authPanel.setBusy(false);
                authPanel.setStatus(message, Color.RED);
            }
        });
    }

    private void onRegisterRequested(String login, String email, String password) {
        authPanel.setBusy(true);
        authPanel.setStatus("Rejestracja...", Color.YELLOW);

        getGameInstance().getAuthService().register(login, email, password, new AuthService.AuthResultListener() {
            @Override
            public void onSuccess(AuthService.SessionSnapshot session) {
                authPanel.setBusy(false);
                authPanel.clearStatus();
                authPanel.resetToLoginMode();
                applyAuthenticatedSession(session);
            }

            @Override
            public void onFailure(String message) {
                authPanel.setBusy(false);
                authPanel.setStatus(message, Color.RED);
            }
        });
    }

    private void applyAuthenticatedSession(AuthService.SessionSnapshot session) {
        getGameInstance().setPlayerName(session.username());
        refreshAccountInfo();
        showMainMenu();
    }

    private void onLogoutClicked() {
        getGameInstance().getAuthService().logout(() -> {
            getGameInstance().setPlayerName("Player");
            if (multiplayerPanel != null) {
                multiplayerPanel.setVisibility(EVisibility.COLLAPSED);
            }
            if (settingsPanel != null) {
                settingsPanel.setVisibility(EVisibility.HIDDEN);
            }
            showAuthPanel("Wylogowano.", Color.YELLOW);
        });
    }

    private void showMainMenu() {
        if (authPanel != null) {
            authPanel.setVisibility(EVisibility.COLLAPSED);
        }
        if (accountOverlay != null) {
            accountOverlay.setVisibility(EVisibility.VISIBLE);
        }
        if (multiplayerPanel != null) {
            multiplayerPanel.setVisibility(EVisibility.COLLAPSED);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        menuContainer.setVisibility(EVisibility.VISIBLE);
        refreshAccountInfo();
    }

    private void showAuthPanel(String statusText, Color statusColor) {
        menuContainer.setVisibility(EVisibility.COLLAPSED);
        if (accountOverlay != null) {
            accountOverlay.setVisibility(EVisibility.COLLAPSED);
        }
        if (multiplayerPanel != null) {
            multiplayerPanel.setVisibility(EVisibility.COLLAPSED);
        }
        if (settingsPanel != null) {
            settingsPanel.setVisibility(EVisibility.HIDDEN);
        }
        if (authPanel != null) {
            authPanel.setRememberedCredentials(getGameInstance().getAuthService().getRememberedCredentials());
            authPanel.resetToLoginMode();
            authPanel.setBusy(false);
            if (statusText != null && !statusText.isBlank()) {
                authPanel.setStatus(statusText, statusColor);
            } else {
                authPanel.clearStatus();
            }
            authPanel.setVisibility(EVisibility.VISIBLE);
        }
    }

    private void refreshAccountInfo() {
        AuthService authService = getGameInstance().getAuthService();
        if (accountNameLabel != null) {
            String username = authService.isAuthenticated() ? authService.getCurrentUsername() : "---";
            accountNameLabel.setText(username);
        }
        if (accountTimeLabel != null) {
            long seconds = authService.getCurrentPlaytimeSeconds();
            accountTimeLabel.setText(formatDuration(seconds));
        }
    }

    private String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format(java.util.Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private TextButton createMenuButton(Skin skin, String text, float width, float height) {
        TextButton button = new TextButton(text,
            UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 18f, 14f, CONTENT_FONT_SCALE));
        button.getLabel().setWrap(false);
        button.getLabelCell().padBottom(0f);
        button.setSize(width, height);
        return button;
    }
}


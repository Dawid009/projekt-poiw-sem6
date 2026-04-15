package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.ui.ButtonWidget;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.EVisibility;
import com.polsl.poiw.engine.ui.TextBlock;
import com.polsl.poiw.engine.ui.TextFieldWidget;
import com.polsl.poiw.engine.ui.UserWidget;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

/**
 * Controller menu głównego — tworzy widgety Play / Multiplayer / Options / Quit
 */

//TODO: maybe pressing Esc should return the player to the menu from the game? (currently Esc does nothing in-game)

public class MenuPlayerController extends PlayerController {

    private static final String TAG = "MenuPlayerController";

    private UserWidget menuContainer;
    private UserWidget multiplayerPanel;
    private TextFieldWidget ipField;
    private TextFieldWidget portField;

    @Override
    protected void setupHUD() {
        Skin skin = getSkin();

        // Menu główne
        menuContainer = new UserWidget();
        menuContainer.setAnchor(EAnchor.CENTER);
        menuContainer.setAlignment(EAnchor.CENTER);
        menuContainer.setSize(200f, 150f);

        // Tytuł gry
        TextBlock title = new TextBlock("Gra 2D", skin);
        title.setAnchor(EAnchor.TOP_CENTER);
        title.setAlignment(EAnchor.TOP_CENTER);
        title.setOffset(0f, -5f);
        title.setFontScale(1.5f);
        title.setColor(Color.CYAN);
        menuContainer.addChild(title);

        // Przycisk Play
        ButtonWidget playButton = new ButtonWidget("Graj", skin);
        playButton.setAnchor(EAnchor.CENTER);
        playButton.setAlignment(EAnchor.CENTER);
        playButton.setOffset(0f, 30f);
        playButton.setButtonSize(80f, 20f);
        playButton.onClick(this::onPlayClicked);
        menuContainer.addChild(playButton);

        // Przycisk Multiplayer
        ButtonWidget multiplayerButton = new ButtonWidget("Multiplayer", skin);
        multiplayerButton.setAnchor(EAnchor.CENTER);
        multiplayerButton.setAlignment(EAnchor.CENTER);
        multiplayerButton.setOffset(0f, 5f);
        multiplayerButton.setButtonSize(80f, 20f);
        multiplayerButton.onClick(this::openMultiplayerPanel);
        menuContainer.addChild(multiplayerButton);

        // Przycisk Options
        ButtonWidget optionsButton = new ButtonWidget("Opcje", skin);
        optionsButton.setAnchor(EAnchor.CENTER);
        optionsButton.setAlignment(EAnchor.CENTER);
        optionsButton.setOffset(0f, -20f);
        optionsButton.setButtonSize(80f, 20f);
        optionsButton.onClick(this::onOptionsClicked);
        menuContainer.addChild(optionsButton);

        // Przycisk Quit
        ButtonWidget quitButton = new ButtonWidget("Wyjdz", skin);
        quitButton.setAnchor(EAnchor.CENTER);
        quitButton.setAlignment(EAnchor.CENTER);
        quitButton.setOffset(0f, -45f);
        quitButton.setButtonSize(80f, 20f);
        quitButton.onClick(this::onQuitClicked);
        menuContainer.addChild(quitButton);

        addWidgetToViewport(menuContainer);

        // Panel Multiplayer
        multiplayerPanel = new UserWidget();
        multiplayerPanel.setAnchor(EAnchor.CENTER);
        multiplayerPanel.setAlignment(EAnchor.CENTER);
        multiplayerPanel.setSize(200f, 160f);
        multiplayerPanel.setVisibility(EVisibility.COLLAPSED);

        // Tytuł panelu
        TextBlock panelTitle = new TextBlock("Tryb sieciowy", skin);
        panelTitle.setAnchor(EAnchor.TOP_CENTER);
        panelTitle.setAlignment(EAnchor.TOP_CENTER);
        panelTitle.setOffset(0f, -5f);
        panelTitle.setFontScale(1.2f);
        panelTitle.setColor(Color.CYAN);
        multiplayerPanel.addChild(panelTitle);

        // Etykieta IP
        TextBlock ipLabel = new TextBlock("Adres IP:", skin);
        ipLabel.setAnchor(EAnchor.TOP_CENTER);
        ipLabel.setAlignment(EAnchor.CENTER_RIGHT);
        ipLabel.setOffset(-5f, -40f);
        multiplayerPanel.addChild(ipLabel);

        // Pole IP
        ipField = new TextFieldWidget("localhost", skin);
        ipField.setText("localhost");
        ipField.setAnchor(EAnchor.TOP_CENTER);
        ipField.setAlignment(EAnchor.CENTER_LEFT);
        ipField.setOffset(5f, -40f);
        ipField.setFieldSize(90f, 16f);
        multiplayerPanel.addChild(ipField);

        // Etykieta Port
        TextBlock portLabel = new TextBlock("Port:", skin);
        portLabel.setAnchor(EAnchor.TOP_CENTER);
        portLabel.setAlignment(EAnchor.CENTER_RIGHT);
        portLabel.setOffset(-5f, -64f);
        multiplayerPanel.addChild(portLabel);

        // Pole Port
        portField = new TextFieldWidget("54555", skin);
        portField.setText(String.valueOf(NetworkProtocol.DEFAULT_TCP_PORT));
        portField.setAnchor(EAnchor.TOP_CENTER);
        portField.setAlignment(EAnchor.CENTER_LEFT);
        portField.setOffset(5f, -64f);
        portField.setFieldSize(90f, 16f);
        portField.getTextField().setTextFieldFilter(new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter.DigitsOnlyFilter());
        multiplayerPanel.addChild(portField);

        // Przycisk Połącz
        ButtonWidget connectButton = new ButtonWidget("Polacz", skin);
        connectButton.setAnchor(EAnchor.CENTER);
        connectButton.setAlignment(EAnchor.CENTER);
        connectButton.setOffset(0f, -20f);
        connectButton.setButtonSize(80f, 20f);
        connectButton.onClick(this::onConnectClicked);
        multiplayerPanel.addChild(connectButton);

        // Przycisk Powrót
        ButtonWidget backButton = new ButtonWidget("Powrot", skin);
        backButton.setAnchor(EAnchor.CENTER);
        backButton.setAlignment(EAnchor.CENTER);
        backButton.setOffset(0f, -45f);
        backButton.setButtonSize(80f, 20f);
        backButton.onClick(this::closeMultiplayerPanel);
        multiplayerPanel.addChild(backButton);

        addWidgetToViewport(multiplayerPanel);
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
        Gdx.app.debug(TAG, "Options (niezaimplementowane)");
    }

    private void onQuitClicked() {
        Gdx.app.debug(TAG, "Quit → zamykanie aplikacji");
        Gdx.app.exit();
    }

    //TODO: handle connection to non-existent server
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
                    Gdx.app.error(TAG, "Nieprawidłowy port: " + port);
                    return;
                }
            } catch (NumberFormatException e) {
                Gdx.app.error(TAG, "Nieprawidłowy format portu: " + portText);
                return;
            }
        }

        Gdx.app.debug(TAG, "Łączenie z " + ip + ":" + port);
        GameInstance gi = getGameInstance();
        if (gi != null) {
            gi.setMode(GameInstance.Mode.MULTIPLAYER);
            gi.setServerHost(ip);
            gi.setServerTcpPort(port);
            gi.travel("game");
        }
    }

    private void closeMultiplayerPanel() {
        multiplayerPanel.setVisibility(EVisibility.COLLAPSED);
        menuContainer.setVisibility(EVisibility.VISIBLE);
    }
}


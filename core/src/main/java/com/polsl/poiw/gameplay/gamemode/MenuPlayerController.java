package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.ui.ButtonWidget;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.TextBlock;
import com.polsl.poiw.engine.ui.UserWidget;

/**
 * Controller menu głównego — tworzy widgety Play / Options / Quit.
 */
public class MenuPlayerController extends PlayerController {

    private static final String TAG = "MenuPlayerController";

    @Override
    protected void setupHUD() {
        Skin skin = getSkin();

        // Kontener menu — wyśrodkowany
        UserWidget menuContainer = new UserWidget();
        menuContainer.setAnchor(EAnchor.CENTER);
        menuContainer.setAlignment(EAnchor.CENTER);
        menuContainer.setSize(200f, 120f);

        // Tytuł gry
        TextBlock title = new TextBlock("Projekt POIW", skin);
        title.setAnchor(EAnchor.TOP_CENTER);
        title.setAlignment(EAnchor.TOP_CENTER);
        title.setOffset(0f, -5f);
        title.setFontScale(1.5f);
        title.setColor(Color.WHITE);
        menuContainer.addChild(title);

        // Przycisk Play
        ButtonWidget playButton = new ButtonWidget("Graj", skin);
        playButton.setAnchor(EAnchor.CENTER);
        playButton.setAlignment(EAnchor.CENTER);
        playButton.setOffset(0f, 10f);
        playButton.setButtonSize(80f, 20f);
        playButton.onClick(this::onPlayClicked);
        menuContainer.addChild(playButton);

        // Przycisk Options
        ButtonWidget optionsButton = new ButtonWidget("Opcje", skin);
        optionsButton.setAnchor(EAnchor.CENTER);
        optionsButton.setAlignment(EAnchor.CENTER);
        optionsButton.setOffset(0f, -12f);
        optionsButton.setButtonSize(80f, 20f);
        optionsButton.onClick(this::onOptionsClicked);
        menuContainer.addChild(optionsButton);

        // Przycisk Quit
        ButtonWidget quitButton = new ButtonWidget("Wyjdz", skin);
        quitButton.setAnchor(EAnchor.CENTER);
        quitButton.setAlignment(EAnchor.CENTER);
        quitButton.setOffset(0f, -34f);
        quitButton.setButtonSize(80f, 20f);
        quitButton.onClick(this::onQuitClicked);
        menuContainer.addChild(quitButton);

        addWidgetToViewport(menuContainer);
    }

    private void onPlayClicked() {
        Gdx.app.debug(TAG, "Play → travel do game");
        GameInstance gi = getGameInstance();
        if (gi != null) {
            gi.travel("game");
        }
    }

    private void onOptionsClicked() {
        Gdx.app.debug(TAG, "Options (niezaimplementowane)");
    }

    private void onQuitClicked() {
        Gdx.app.debug(TAG, "Quit → zamykanie aplikacji");
        Gdx.app.exit();
    }
}

package com.polsl.poiw.ui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.engine.asset.AtlasAsset;
import com.polsl.poiw.engine.asset.SkinAsset;
import com.polsl.poiw.gameplay.level.LevelDefinitions;

/**
 * Ekran ładowania zasobów.
 * <p>
 * Po załadowaniu przełącza na LevelScreen i wykonuje travel do menu głównego.
 */
public class LoadingScreen extends ScreenAdapter {
    private final Main game;
    private final AssetService assetService;

    public LoadingScreen(Main game) {
        this.game = game;
        this.assetService = game.getAssetService();
    }

    @Override
    public void show() {
        Gdx.app.debug("LoadingScreen", "Rozpoczynam ładowanie zasobów...");

        // Kolejkuj atlasy tekstur
        for (AtlasAsset atlasAsset : AtlasAsset.values()) {
            assetService.queue(atlasAsset);
        }

        // Kolejkuj skiny UI
        for (SkinAsset skinAsset : SkinAsset.values()) {
            assetService.queue(skinAsset);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // AssetManager ładuje w tle — update() zwraca true gdy skończy
        if (assetService.update()) {
            Gdx.app.debug("LoadingScreen", "Zasoby załadowane. Przechodzę do menu.");

            // Przełącz na LevelScreen i travel do menu głównego
            game.switchToLevelScreen();
            game.getGameInstance().travel(LevelDefinitions.MAIN_MENU);

            // LoadingScreen już nie jest potrzebny
            this.dispose();
        }
    }
}
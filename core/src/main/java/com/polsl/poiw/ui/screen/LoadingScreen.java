package com.polsl.poiw.ui.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.asset.AssetService;
import com.polsl.poiw.engine.asset.AtlasAsset;
import com.polsl.poiw.engine.asset.SkinAsset;

/**
 * Ekran ładowania zasobów.
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

        // Kolejkuj skin UI
        assetService.queue(SkinAsset.DEFAULT);

        // Dźwięki — audio folder jest pusty póki to co pomijamy
        // for (SoundAsset s : SoundAsset.values()) { assetService.queue(s); }
    }

    @Override
    public void render(float delta) {
        // Placeholder
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // AssetManager ładuje w tle — update() zwraca true gdy skończy
        if (assetService.update()) {
            Gdx.app.debug("LoadingScreen", "Zasoby załadowane. Tworzę ekrany.");
            createScreens();
            game.removeScreen(this);
            this.dispose();
            game.setScreen(GameScreen.class);
        }
    }

    /**
     * Tworzy ekrany gry po załadowaniu zasobów.
     * Ekrany wymagają zasobów (skin, atlas) — dlatego tworzymy je PO załadowaniu.
     */
    private void createScreens() {
        game.addScreen(new GameScreen(game));
        // W przyszłości: game.addScreen(new MenuScreen(game));
    }
}

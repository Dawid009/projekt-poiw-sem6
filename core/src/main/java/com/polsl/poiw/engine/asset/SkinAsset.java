package com.polsl.poiw.engine.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Enum definiujący dostępne skiny UI.
 * Skin to plik JSON + TextureAtlas definiujący wygląd elementów Scene2D.
 */
public enum SkinAsset implements Asset<Skin> {
    DEFAULT("uiskin.json");

    private final AssetDescriptor<Skin> descriptor;

    SkinAsset(String skinJsonFile) {
        this.descriptor = new AssetDescriptor<>("ui/" + skinJsonFile, Skin.class);
    }

    @Override
    public AssetDescriptor<Skin> getDescriptor() {
        return descriptor;
    }
}

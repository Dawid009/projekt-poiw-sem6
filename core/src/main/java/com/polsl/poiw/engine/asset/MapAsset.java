package com.polsl.poiw.engine.asset;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.maps.tiled.BaseTiledMapLoader;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public enum MapAsset implements Asset<TiledMap> {
    MAIN("mainmap.tmx");

    private final AssetDescriptor<TiledMap> descriptor;

    MapAsset(String mapName) {
        BaseTiledMapLoader.Parameters parameters = new TmxMapLoader.Parameters();
        //TODO: Uzupełnić poprawnie ścieżkę
        parameters.projectFilePath = "maps/ ";
        this.descriptor = new AssetDescriptor<>("maps/" + mapName, TiledMap.class, parameters);
    }

    @Override
    public AssetDescriptor<TiledMap> getDescriptor() {
        return descriptor;
    }
}

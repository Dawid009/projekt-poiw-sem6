package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;

public class FullscreenBackgroundRenderer implements Disposable {

    private final Texture texture;
    private final Matrix4 projection = new Matrix4();

    public FullscreenBackgroundRenderer(String internalPath) {
        this.texture = new Texture(Gdx.files.internal(internalPath));
    }

    public void render(Batch batch) {
        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        projection.setToOrtho2D(0f, 0f, width, height);

        batch.setProjectionMatrix(projection);
        batch.begin();
        batch.draw(texture, 0f, 0f, width, height);
        batch.end();
    }

    @Override
    public void dispose() {
        texture.dispose();
    }
}
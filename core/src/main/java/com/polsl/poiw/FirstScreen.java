package com.polsl.poiw;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/** First screen of the application. Displayed after the application is created. */
public class FirstScreen implements Screen {
    ShapeRenderer shapeRenderer;

    float circleX = 200;
    float circleY = 100;

    float xSpeed = 320;
    float ySpeed = 240;

    float circleRadius = 50;

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
    }

    @Override
    public void render(float delta) {
        circleX += xSpeed *  delta;
        circleY += ySpeed *  delta;

        if(circleX < circleRadius || circleX > Gdx.graphics.getWidth()-circleRadius){
            xSpeed *= -1;
        }

        if(circleY < circleRadius || circleY > Gdx.graphics.getHeight()-circleRadius){
            ySpeed *= -1;
        }

        Gdx.gl.glClearColor(.25f, .25f, .25f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 1, 0, 1);
        shapeRenderer.circle(circleX, circleY, circleRadius);
        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        // If the window is minimized on a desktop (LWJGL3) platform, width and height are 0, which causes problems.
        // In that case, we don't resize anything, and wait for the window to be a normal size before updating.
        if(width <= 0 || height <= 0) return;

        // Resize your screen here. The parameters represent the new window size.
    }

    @Override
    public void pause() {
        // Invoked when your application is paused.
    }

    @Override
    public void resume() {
        // Invoked when your application is resumed after pause.
    }

    @Override
    public void hide() {
        // This method is called when another screen replaces this one.
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}

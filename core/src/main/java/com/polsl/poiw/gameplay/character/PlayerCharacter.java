package com.polsl.poiw.gameplay.character;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.polsl.poiw.Main;
import com.polsl.poiw.engine.actor.AbstractActor;
import com.polsl.poiw.engine.component.*;

/**
 * Postać gracza — podstawowy Actor z komponentami ruchu, grafiki i kamery.
 */
public class PlayerCharacter extends AbstractActor {

    /** Prędkość gracza w metrach/s */
    private static final float PLAYER_SPEED = 3.5f;

    /** Klucz regionu w atlasie */
    private static final String PLAYER_REGION = "player/idle_down";

    /** Rozmiar sprite'a w pikselach (32x32) */
    private static final float SPRITE_PX = 32f;

    public PlayerCharacter() {
        // Komponenty dodawane są w configure(), bo tam mamy dostęp do parametrów
    }

    /**
     * Konfiguruje gracza z podanym atlasem.
     * Wywoływane po stworzeniu, ale przed beginPlay().
     */
    public void configure(TextureAtlas atlas) {
        // Znajdź region w atlasie
        TextureRegion region = atlas.findRegion(PLAYER_REGION);
        if (region == null) {
            throw new RuntimeException("Nie znaleziono regionu: " + PLAYER_REGION + " w atlasie");
        }

        // Rozmiar w świecie (32px * UNIT_SCALE = 2 metry)
        float sizeW = SPRITE_PX * Main.UNIT_SCALE;
        float sizeH = SPRITE_PX * Main.UNIT_SCALE;


        //TransformComponent - odpowiada za umieszczenie w świecie gry
        addComponent(new TransformComponent(
            new Vector2(getPosition()),
            1,
            new Vector2(sizeW, sizeH)
        ));
        //Sprite component odpowiada za sprite - który będzie rysowany
        addComponent(new SpriteComponent(region, Color.WHITE.cpy()));
        //Movement component - opisuje aktualny ruch i jego parametry
        addComponent(new MovementComponent(PLAYER_SPEED));
        //Camera follow - kamera podążajaca za aktorem
        addComponent(new CameraFollowComponent());
        //Działa jako inputy z klawiatury
        addComponent(new ControllerComponent());
    }

    @Override
    public void beginPlay() {
        super.beginPlay();
        // Synchronizuj SceneComponent.position z Actor.position
        //TODO: Przemyśleć, to aby było to synchronizowane automatycznie
        TransformComponent scene = getComponent(TransformComponent.class);
        if (scene != null) {
            scene.getPosition().set(getPosition());
        }
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);
    }
}

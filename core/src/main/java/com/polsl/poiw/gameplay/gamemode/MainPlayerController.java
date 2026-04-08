package com.polsl.poiw.gameplay.gamemode;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.polsl.poiw.engine.actor.Actor;
import com.polsl.poiw.engine.binding.BindingHandle;
import com.polsl.poiw.engine.gameframework.PlayerController;
import com.polsl.poiw.engine.ui.EAnchor;
import com.polsl.poiw.engine.ui.TextBlock;
import com.polsl.poiw.gameplay.character.PlayerCharacter;

/**
 * Controller gracza — tworzy HUD z wyświetlaniem HP
 * i binduje go do PropertyBinding z PlayerCharacter.
 */
public class MainPlayerController extends PlayerController {

    private final Skin skin;

    private TextBlock hpText;
    private BindingHandle healthBinding;
    private BindingHandle maxHealthBinding;

    /** Aktualne wartości do formatowania tekstu */
    private float currentHp = 0f;
    private float currentMaxHp = 0f;

    public MainPlayerController(Skin skin) {
        this.skin = skin;
    }

    @Override
    protected void setupHUD() {
        // TextBlock wyświetlający HP — lewy górny róg
        hpText = new TextBlock("HP: ---", skin);
        hpText.setAnchor(EAnchor.TOP_LEFT);
        hpText.setAlignment(EAnchor.TOP_LEFT);
        hpText.setOffset(4f, -4f);
        hpText.setColor(Color.WHITE);
        hpText.setFontScale(1f);
        hpText.setVariable(true);

        addWidgetToViewport(hpText);
    }

    @Override
    protected void onPossess(Actor pawn) {
        // Binduj HP z PlayerCharacter do TextBlock
        if (pawn instanceof PlayerCharacter player) {
            healthBinding = player.getHealth().bind(val -> {
                currentHp = val;
                updateHpText();
            });
            maxHealthBinding = player.getMaxHealth().bind(val -> {
                currentMaxHp = val;
                updateHpText();
            });
        }
    }

    @Override
    protected void onUnpossess() {
        if (healthBinding != null) {
            healthBinding.unbind();
            healthBinding = null;
        }
        if (maxHealthBinding != null) {
            maxHealthBinding.unbind();
            maxHealthBinding = null;
        }
    }

    @Override
    public void destroy() {
        onUnpossess();
        super.destroy();
    }

    private void updateHpText() {
        if (hpText != null) {
            int hp = Math.round(currentHp);
            int max = Math.round(currentMaxHp);
            hpText.setText("HP: " + hp + " / " + max);

            // Kolor zależny od poziomu HP
            float ratio = currentMaxHp > 0 ? currentHp / currentMaxHp : 0f;
            if (ratio > 0.5f) {
                hpText.setColor(Color.WHITE);
            } else if (ratio > 0.25f) {
                hpText.setColor(Color.YELLOW);
            } else {
                hpText.setColor(Color.RED);
            }
        }
    }
}

package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

/**
 * PlayerListWidget — Minecraft-style TAB player list.
 * Shown at top-center while TAB is held.
 */
public class PlayerListWidget extends UserWidget {

    private static final float WIDTH = 220f;
    private static final float HEADER_HEIGHT = 24f;
    private static final float ROW_HEIGHT = 18f;
    private static final float FONT_SCALE = 0.85f;

    private final Skin skin;
    private Table container;
    private String[] playerNames = new String[0];

    public PlayerListWidget(Skin skin) {
        super();
        this.skin = skin;
        root.setTouchable(Touchable.disabled);
        buildUI();
    }

    @Override
    public void setVisibility(EVisibility visibility) {
        super.setVisibility(visibility);
        // Display-only widget — never intercept touches
        root.setTouchable(Touchable.disabled);
    }

    private void buildUI() {
        container = new Table();
        Drawable bg = skin.newDrawable("white", new Color(0, 0, 0, 0.55f));
        container.setBackground(bg);
        container.top();
        container.pad(6f, 10f, 6f, 10f);

        addActor(container);
        rebuild();
    }

    public void updatePlayers(String[] names) {
        this.playerNames = names != null ? names : new String[0];
        rebuild();
    }

    private void rebuild() {
        container.clearChildren();

        // Header
        Label header = new Label("Players (" + playerNames.length + ")", skin);
        header.setColor(Color.GOLD);
        header.setFontScale(FONT_SCALE);
        header.setAlignment(Align.center);
        container.add(header).width(WIDTH - 20f).height(HEADER_HEIGHT).center().padBottom(4f).row();

        // Player rows
        for (String name : playerNames) {
            Label row = new Label(name, skin);
            row.setColor(Color.WHITE);
            row.setFontScale(FONT_SCALE);
            row.setAlignment(Align.center);
            container.add(row).width(WIDTH - 20f).height(ROW_HEIGHT).center().row();
        }

        float totalHeight = 12f + HEADER_HEIGHT + 4f + playerNames.length * ROW_HEIGHT;
        container.setSize(WIDTH, totalHeight);
        setSize(WIDTH, totalHeight);
    }
}

package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.polsl.poiw.engine.settings.GraphicsSettings;
import com.polsl.poiw.engine.settings.GraphicsSettingsService;
import com.polsl.poiw.engine.settings.ResolutionOption;

import java.util.List;

public class SettingsPanelWidget extends UserWidget {

    private static final float CONTENT_FONT_SCALE = 0.5f;
    private static final float SELECT_WIDTH = 94f;
    private static final float SELECT_HEIGHT = 12f;
    private static final float BUTTON_WIDTH = 38f;
    private static final float BUTTON_HEIGHT = 12f;
    private static final float CONTENT_PADDING = 4f;
    private static final float ROW_SPACING = 1.5f;
    private static final float SECTION_SPACING = 3f;

    private final List<ResolutionOption> availableResolutions;
    private final List<Integer> fpsLimits;

    private final Window window;
    private final SelectBox<String> resolutionSelect;
    private final CheckBox vSyncCheckBox;
    private final SelectBox<String> fpsLimitSelect;

    private Runnable closeAction;

    public SettingsPanelWidget(Skin skin) {
        super();
        this.availableResolutions = GraphicsSettingsService.getAvailableResolutions();
        this.fpsLimits = GraphicsSettingsService.getAvailableFpsLimits();
        this.window = new Window("Opcje", skin, "atlas");
        this.window.setMovable(false);
        UiSkinStyles.centerWindowTitle(window);

        var compactSelectStyle = UiSkinStyles.copyCompactSelectBoxStyle(skin, "atlas", "font", 24f, 14f, 4f, 10f, 16f, CONTENT_FONT_SCALE);
        var compactCheckStyle = UiSkinStyles.copyCompactCheckBoxStyle(skin, "atlas", "font", 10f, 10f, CONTENT_FONT_SCALE);
        this.resolutionSelect = createSelectBox(compactSelectStyle);
        this.vSyncCheckBox = new CheckBox(" VSync", compactCheckStyle);
        this.fpsLimitSelect = createSelectBox(compactSelectStyle);
        this.vSyncCheckBox.getImageCell().padRight(2f);

        populateSelectBoxes();

        TextButton applyButton = new TextButton("Apply", UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE));
        TextButton saveButton = new TextButton("Save", UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE));
        TextButton resetButton = new TextButton("Reset", UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE));
        TextButton backButton = new TextButton("Powrot", UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 16f, 14f, CONTENT_FONT_SCALE));

        Label resolutionLabel = new Label("Rozdzielczosc", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));

        Label fpsLimitLabel = new Label("Limit FPS", UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE));

        Table content = new Table();
        content.defaults().left().padBottom(ROW_SPACING);
        content.add(resolutionLabel).left().padBottom(0.5f).row();
        content.add(resolutionSelect).width(SELECT_WIDTH).height(SELECT_HEIGHT).left().padBottom(SECTION_SPACING).row();
        content.add(vSyncCheckBox).left().padBottom(SECTION_SPACING).row();
        content.add(fpsLimitLabel).left().padBottom(0.5f).row();
        content.add(fpsLimitSelect).width(58f).height(SELECT_HEIGHT).left().padBottom(SECTION_SPACING).row();

        Table buttons = new Table();
        buttons.defaults().width(BUTTON_WIDTH).height(BUTTON_HEIGHT);
        buttons.add(applyButton).padRight(1.5f);
        buttons.add(saveButton).padRight(1.5f);
        buttons.add(resetButton).padRight(1.5f);
        buttons.add(backButton);

        content.add(buttons).left().padTop(1.5f).row();

        window.add(content).pad(7f, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING);
        window.pack();

        addActor(window);
        syncSize();
        setVisibility(EVisibility.HIDDEN);

        applyButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                applyPending(false);
            }
        });

        saveButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                applyPending(true);
            }
        });

        resetButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                GraphicsSettings defaults = GraphicsSettingsService.getDefaultSettings();
                GraphicsSettingsService.applySettings(defaults);
                setSettings(defaults);
            }
        });

        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (closeAction != null) {
                    closeAction.run();
                }
            }
        });
    }

    private SelectBox<String> createSelectBox(SelectBox.SelectBoxStyle style) {
        return new SelectBox<>(style) {
            @Override
            protected void onShow(Actor scrollPane, boolean below) {
                scrollPane.clearActions();
                scrollPane.getColor().a = 1f;
            }

            @Override
            protected void onHide(Actor scrollPane) {
                scrollPane.clearActions();
                scrollPane.remove();
            }
        };
    }

    public void refreshFromAppliedSettings() {
        setSettings(GraphicsSettingsService.getAppliedSettings());
    }

    public void setCloseAction(Runnable closeAction) {
        this.closeAction = closeAction;
    }

    private void populateSelectBoxes() {
        Array<String> resolutionItems = new Array<>();
        for (ResolutionOption option : availableResolutions) {
            resolutionItems.add(option.label());
        }
        resolutionSelect.setItems(resolutionItems);

        Array<String> fpsItems = new Array<>();
        for (Integer fpsLimit : fpsLimits) {
            fpsItems.add(String.valueOf(fpsLimit));
        }
        fpsLimitSelect.setItems(fpsItems);

        resolutionSelect.setMaxListCount(5);
        fpsLimitSelect.setMaxListCount(5);
    }

    private void setSettings(GraphicsSettings settings) {
        resolutionSelect.setSelected(settings.resolution().label());
        vSyncCheckBox.setChecked(settings.vSyncEnabled());
        fpsLimitSelect.setSelected(String.valueOf(settings.fpsLimit()));
    }

    // Apply rusza runtime od razu, Save dodatkowo zapisuje wybor do prefs.
    private void applyPending(boolean save) {
        GraphicsSettings pending = readPendingSettings();
        GraphicsSettings applied = GraphicsSettingsService.applySettings(pending);
        if (save) {
            GraphicsSettingsService.saveSettings(applied);
        }
        setSettings(applied);
    }

    private GraphicsSettings readPendingSettings() {
        ResolutionOption resolution = findResolution(resolutionSelect.getSelected());
        int fpsLimit = Integer.parseInt(fpsLimitSelect.getSelected());
        return new GraphicsSettings(resolution, vSyncCheckBox.isChecked(), fpsLimit);
    }

    private ResolutionOption findResolution(String label) {
        for (ResolutionOption option : availableResolutions) {
            if (option.label().equals(label)) {
                return option;
            }
        }
        return GraphicsSettingsService.getAppliedSettings().resolution();
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }
}

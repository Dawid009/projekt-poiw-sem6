package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * HUD — kontener widgetów wyświetlanych na ekranie gracza.
 * <p>
 * Zarządza listą aktywnych widgetów (dodawanie/usuwanie z viewportu),
 * ich aktualizacją (tick) i przekazywaniem do Scene2D Stage.
 * <p>
 * HUD jest tworzony przez GameMode i przypisywany do PlayerController.
 */
public class HUD implements Disposable {

    /** Stage Scene2D odpowiadający za renderowanie UI */
    private final Stage stage;

    /** Lista widgetów dodanych do viewportu */
    private final List<UserWidget> widgets = new ArrayList<>();

    /** Kolejki modyfikacji (dodawanie/usuwanie poza iteracją) */
    private final List<UserWidget> pendingAdd = new ArrayList<>();
    private final List<UserWidget> pendingRemove = new ArrayList<>();

    public HUD(Stage stage) {
        this.stage = stage;
    }

    // ===== Zarządzanie widgetami =====

    /** Dodaje widget do viewportu — będzie renderowany na ekranie */
    public void addToViewport(UserWidget widget) {
        if (!widgets.contains(widget) && !pendingAdd.contains(widget)) {
            pendingAdd.add(widget);
        }
    }

    /** Usuwa widget z viewportu */
    public void removeFromViewport(UserWidget widget) {
        if (!pendingRemove.contains(widget)) {
            pendingRemove.add(widget);
        }
    }

    /** Sprawdza czy widget jest aktualnie w viewporcie */
    public boolean isInViewport(UserWidget widget) {
        return widgets.contains(widget);
    }

    /** Zwraca niemutowalną listę aktywnych widgetów */
    public List<UserWidget> getWidgets() {
        return Collections.unmodifiableList(widgets);
    }

    // ===== Aktualizacja =====

    /** Wywoływane co klatkę — tickuje widgety, przetwarza kolejki, aktualizuje Stage */
    public void update(float delta) {
        // Przetwórz kolejki modyfikacji
        processPending();

        // Tick aktywnych widgetów
        for (UserWidget widget : widgets) {
            if (widget.isVisible()) {
                widget.tick(delta);
            }
        }

        // Scene2D act
        stage.act(delta);
    }

    /** Renderuje UI — wywoływane po renderowaniu świata gry */
    public void render() {
        stage.getViewport().apply();
        stage.draw();
    }

    /** Przelicza layout po zmianie rozmiaru okna */
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        for (UserWidget widget : widgets) {
            widget.updateLayout();
        }
    }

    // ===== Internals =====

    private void processPending() {
        // Dodawanie
        for (UserWidget widget : pendingAdd) {
            if (!widgets.contains(widget)) {
                widgets.add(widget);
                widget.addToStage(stage);
            }
        }
        pendingAdd.clear();

        // Usuwanie
        for (UserWidget widget : pendingRemove) {
            if (widgets.remove(widget)) {
                widget.removeFromStage();
            }
        }
        pendingRemove.clear();
    }

    // ===== Dostęp =====

    public Stage getStage() { return stage; }

    @Override
    public void dispose() {
        for (UserWidget widget : new ArrayList<>(widgets)) {
            widget.removeFromStage();
        }
        widgets.clear();
        pendingAdd.clear();
        pendingRemove.clear();
        stage.dispose();
    }
}

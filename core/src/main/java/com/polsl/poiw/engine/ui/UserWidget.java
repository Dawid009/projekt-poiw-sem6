package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Bazowy widget UI — odpowiednik UWidget.
 * <p>
 * Opakowuje Scene2D {@link Group} i dodaje system ankierowania,
 * widoczności, hierarchii parent/child oraz dynamicznego dodawania do viewportu.
 * <p>
 * Każdy widget posiada:
 * <ul>
 *   <li>anchor — punkt zakotwiczenia na ekranie/parentcie</li>
 *   <li>alignment — wyrównanie samego widgetu względem anchora</li>
 *   <li>offset — przesunięcie od obliczonej pozycji anchora</li>
 *   <li>visibility — sterowanie widocznością</li>
 * </ul>
 */
public class UserWidget {

    /** Wewnętrzny aktor Scene2D */
    protected final Group root;

    /** Anchor — punkt zakotwiczenia w przestrzeni parenta */
    private EAnchor anchor = EAnchor.TOP_LEFT;

    /** Alignment — wyrównanie widgetu względem obliczonej pozycji */
    private EAnchor alignment = EAnchor.TOP_LEFT;

    /** Offset od pozycji anchora (w pikselach UI) */
    private float offsetX = 0f;
    private float offsetY = 0f;

    /** Widoczność */
    private EVisibility visibility = EVisibility.VISIBLE;

    /** Hierarchia */
    private UserWidget parent;
    private final List<UserWidget> children = new ArrayList<>();

    /** Czy widget jest dodany do viewportu (Stage) */
    private boolean addedToViewport = false;

    public UserWidget() {
        this.root = new Group();
        root.setName(getClass().getSimpleName());
    }

    // ===== Lifecycle =====

    /** Wywoływane po dodaniu do viewportu lub do parenta. Override w subklasach. */
    public void construct() {}

    /** Wywoływane przed usunięciem z viewportu. Override w subklasach. */
    public void destruct() {}

    /** Aktualizacja co klatkę. Override w subklasach. */
    public void tick(float delta) {
        for (UserWidget child : children) {
            if (child.visibility != EVisibility.COLLAPSED) {
                child.tick(delta);
            }
        }
    }

    // ===== Hierarchia =====

    /** Dodaje child widget */
    public void addChild(UserWidget child) {
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        child.parent = this;
        children.add(child);
        root.addActor(child.root);
        child.construct();
        child.updateLayout();
    }

    /** Usuwa child widget */
    public void removeChild(UserWidget child) {
        if (children.remove(child)) {
            child.destruct();
            child.parent = null;
            root.removeActor(child.root);
        }
    }

    /** Usuwa wszystkie dzieci */
    public void clearChildren() {
        for (UserWidget child : new ArrayList<>(children)) {
            removeChild(child);
        }
    }

    /** Dodaje surowy aktor Scene2D do root group */
    protected void addActor(Actor actor) {
        root.addActor(actor);
    }

    // ===== Layout =====

    /** Przelicza pozycję widgetu na podstawie anchora, alignment i offsetu */
    public void updateLayout() {
        float parentW, parentH;

        if (parent != null) {
            parentW = parent.getWidth();
            parentH = parent.getHeight();
        } else if (root.getStage() != null) {
            parentW = root.getStage().getViewport().getWorldWidth();
            parentH = root.getStage().getViewport().getWorldHeight();
        } else {
            return;
        }

        // Pozycja anchora w przestrzeni parenta
        float anchorX = parentW * anchor.getX();
        float anchorY = parentH * anchor.getY();

        // Przesunięcie z alignment (wyrównanie widgetu względem anchora)
        float alignX = getWidth() * alignment.getX();
        float alignY = getHeight() * alignment.getY();

        root.setPosition(anchorX - alignX + offsetX, anchorY - alignY + offsetY);

        // Przelicz dzieci
        for (UserWidget child : children) {
            child.updateLayout();
        }
    }

    // ===== Viewport =====

    /** Dodaje widget do Stage (viewport HUD). Wywoływane przez HUD. */
    public void addToStage(Stage stage) {
        stage.addActor(root);
        addedToViewport = true;
        construct();
        updateLayout();
    }

    /** Usuwa widget ze Stage */
    public void removeFromStage() {
        destruct();
        for (UserWidget child : new ArrayList<>(children)) {
            child.removeFromStage();
        }
        root.remove();
        addedToViewport = false;
    }

    // ===== Visibility =====

    public void setVisibility(EVisibility visibility) {
        this.visibility = visibility;
        switch (visibility) {
            case VISIBLE -> {
                root.setVisible(true);
                root.setTouchable(Touchable.enabled);
            }
            case HIDDEN -> {
                root.setVisible(false);
                root.setTouchable(Touchable.disabled);
            }
            case COLLAPSED -> {
                root.setVisible(false);
                root.setTouchable(Touchable.disabled);
            }
        }
    }

    public EVisibility getVisibility() { return visibility; }
    public boolean isVisible() { return visibility == EVisibility.VISIBLE; }

    // ===== Anchor / Alignment / Offset =====

    public void setAnchor(EAnchor anchor) {
        this.anchor = anchor;
        updateLayout();
    }

    public void setAlignment(EAnchor alignment) {
        this.alignment = alignment;
        updateLayout();
    }

    public void setOffset(float x, float y) {
        this.offsetX = x;
        this.offsetY = y;
        updateLayout();
    }

    public EAnchor getAnchor() { return anchor; }
    public EAnchor getAlignment() { return alignment; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }

    // ===== Rozmiar =====

    public void setSize(float width, float height) {
        root.setSize(width, height);
        updateLayout();
    }

    public float getWidth() { return root.getWidth(); }
    public float getHeight() { return root.getHeight(); }

    // ===== Gettery =====

    public Group getRoot() { return root; }
    public UserWidget getParent() { return parent; }
    public List<UserWidget> getChildren() { return Collections.unmodifiableList(children); }
    public boolean isAddedToViewport() { return addedToViewport; }
}

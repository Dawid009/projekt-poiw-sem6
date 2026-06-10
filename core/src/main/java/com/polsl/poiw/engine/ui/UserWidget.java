package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Prosty bazowy widget oparty na Scene2D. */
public class UserWidget {

    protected final Group root;
    private EAnchor anchor = EAnchor.TOP_LEFT;
    private EAnchor alignment = EAnchor.TOP_LEFT;
    private float offsetX = 0f;
    private float offsetY = 0f;
    private EVisibility visibility = EVisibility.VISIBLE;
    private UserWidget parent;
    private final List<UserWidget> children = new ArrayList<>();
    private boolean addedToViewport = false;

    public UserWidget() {
        this.root = new Group();
        root.setName(getClass().getSimpleName());
    }

    public void construct() {}

    public void destruct() {}

    public void tick(float delta) {
        for (UserWidget child : children) {
            if (child.visibility != EVisibility.COLLAPSED) {
                child.tick(delta);
            }
        }
    }

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

    public void removeChild(UserWidget child) {
        if (children.remove(child)) {
            child.destruct();
            child.parent = null;
            root.removeActor(child.root);
        }
    }

    public void clearChildren() {
        for (UserWidget child : new ArrayList<>(children)) {
            removeChild(child);
        }
    }

    protected void addActor(Actor actor) {
        root.addActor(actor);
    }
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

        float anchorX = parentW * anchor.getX();
        float anchorY = parentH * anchor.getY();

        float alignX = getWidth() * alignment.getX();
        float alignY = getHeight() * alignment.getY();

        root.setPosition(anchorX - alignX + offsetX, anchorY - alignY + offsetY);

        for (UserWidget child : children) {
            child.updateLayout();
        }
    }
    public void addToStage(Stage stage) {
        stage.addActor(root);
        addedToViewport = true;
        construct();
        updateLayout();
    }

    public void removeFromStage() {
        destruct();
        for (UserWidget child : new ArrayList<>(children)) {
            child.removeFromStage();
        }
        root.remove();
        addedToViewport = false;
    }

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

    public void setSize(float width, float height) {
        root.setSize(width, height);
        updateLayout();
    }

    public float getWidth() { return root.getWidth(); }
    public float getHeight() { return root.getHeight(); }

    public Group getRoot() { return root; }
    public UserWidget getParent() { return parent; }
    public List<UserWidget> getChildren() { return Collections.unmodifiableList(children); }
    public boolean isAddedToViewport() { return addedToViewport; }
}

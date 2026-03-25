package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.math.Vector2;

/**
 * Pozycja, rozmiar, skala i rotacja Actora w świecie gry.
 * Każdy widoczny Actor powinien mieć TransformComponent.
 * Pozycja jest w metrach (świat Box2D)
 */
public class TransformComponent extends AbstractActorComponent implements Comparable<TransformComponent> {
    public static final ComponentMapper<TransformComponent> MAPPER = ComponentMapper.getFor(TransformComponent.class);

    private final Vector2 position;
    private final Vector2 size;
    private final Vector2 scaling;
    private int zOrder;
    private float rotationDeg;
    private float sortOffsetY;

    public TransformComponent(Vector2 position, int zOrder, Vector2 size) {
        this(position, zOrder, size, new Vector2(1f, 1f), 0f, 0f);
    }

    public TransformComponent(Vector2 position, int zOrder, Vector2 size,
                              Vector2 scaling, float rotationDeg, float sortOffsetY) {
        this.position = position;
        this.zOrder = zOrder;
        this.size = size;
        this.scaling = scaling;
        this.rotationDeg = rotationDeg;
        this.sortOffsetY = sortOffsetY;
    }

    @Override
    public int compareTo(TransformComponent other) {
        if (this.zOrder != other.zOrder) {
            return Integer.compare(this.zOrder, other.zOrder);
        }
        // Wyższy Y = dalej od kamery = rysowany wcześniej (pod spodem)
        float thisY = this.position.y + this.sortOffsetY;
        float otherY = other.position.y + other.sortOffsetY;
        if (thisY != otherY) {
            return Float.compare(otherY, thisY);
        }
        return Float.compare(this.position.x, other.position.x);
    }

    @Override
    public void tick(float delta) {
        if (getOwner() != null) {
            getOwner().setPosition(position.x, position.y);
        }
    }

    // ===== Gettery / Settery =====

    public Vector2 getPosition() { return position; }
    public Vector2 getSize() { return size; }
    public Vector2 getScaling() { return scaling; }
    public int getZOrder() { return zOrder; }
    public void setZOrder(int zOrder) { this.zOrder = zOrder; }
    public float getRotationDeg() { return rotationDeg; }
    public void setRotationDeg(float rotationDeg) { this.rotationDeg = rotationDeg; }
    public float getSortOffsetY() { return sortOffsetY; }
    public void setSortOffsetY(float sortOffsetY) { this.sortOffsetY = sortOffsetY; }
}

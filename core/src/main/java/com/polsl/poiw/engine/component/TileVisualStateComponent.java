package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.polsl.poiw.engine.net.Replicated;

/**
 * Replikowany stan wizualny obiektów mapy opartych o pojedynczy tile gid.
 */
public class TileVisualStateComponent extends AbstractActorComponent {
    public static final ComponentMapper<TileVisualStateComponent> MAPPER =
        ComponentMapper.getFor(TileVisualStateComponent.class);

    @Replicated
    private int tileGid;

    public TileVisualStateComponent() {
        this(0);
    }

    public TileVisualStateComponent(int tileGid) {
        setReplicated(true);
        this.tileGid = tileGid;
    }

    public int getTileGid() {
        return tileGid;
    }

    public void setTileGid(int tileGid) {
        if (this.tileGid == tileGid) {
            return;
        }

        this.tileGid = tileGid;
        markDirty("tileGid");
    }
}
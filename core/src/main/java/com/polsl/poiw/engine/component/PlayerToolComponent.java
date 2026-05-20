package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

public class PlayerToolComponent extends AbstractActorComponent {
    public static final ComponentMapper<PlayerToolComponent> MAPPER =
        ComponentMapper.getFor(PlayerToolComponent.class);

    @Replicated
    @RepNotify("onActiveToolChanged")
    private int activeToolOrdinal = PlayerToolType.SWORD.ordinal();

    private final transient PropertyBinding<PlayerToolType> activeToolBinding =
        new PropertyBinding<>(PlayerToolType.SWORD);

    public PlayerToolComponent() {
        setReplicated(true);
    }

    public PlayerToolType getActiveTool() {
        return PlayerToolType.fromOrdinal(activeToolOrdinal);
    }

    public void setActiveTool(PlayerToolType toolType) {
        PlayerToolType resolvedTool = toolType != null ? toolType : PlayerToolType.SWORD;
        int ordinal = resolvedTool.ordinal();
        if (activeToolOrdinal == ordinal) {
            return;
        }

        activeToolOrdinal = ordinal;
        markDirty("activeToolOrdinal");
        activeToolBinding.set(resolvedTool);
    }

    public void cycleNext() {
        setActiveTool(getActiveTool().next());
    }

    public void cyclePrevious() {
        setActiveTool(getActiveTool().previous());
    }

    public PropertyBinding<PlayerToolType> getActiveToolBinding() {
        return activeToolBinding;
    }

    @SuppressWarnings("unused")
    private void onActiveToolChanged() {
        activeToolBinding.set(getActiveTool());
    }
}
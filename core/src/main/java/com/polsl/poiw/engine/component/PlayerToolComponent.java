package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.polsl.poiw.engine.binding.PropertyBinding;
import com.polsl.poiw.engine.net.RepNotify;
import com.polsl.poiw.engine.net.Replicated;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

/**
 * Przechowuje aktualnie wybrane narzedzie gracza.
 * Komponent jest replikowany, bo narzedzie musi byc takie samo po stronie klienta i serwera.
 */
public class PlayerToolComponent extends AbstractActorComponent {
    public static final ComponentMapper<PlayerToolComponent> MAPPER =
        ComponentMapper.getFor(PlayerToolComponent.class);

    @Replicated
    @RepNotify("onActiveToolChanged")
    private int activeToolOrdinal = PlayerToolType.SWORD.ordinal();

    private final transient PropertyBinding<PlayerToolType> activeToolBinding =
        new PropertyBinding<>(PlayerToolType.SWORD);

    /** Narzedzie jest replikowane, bo wybór gracza musi sie zgadzac po obu stronach. */
    public PlayerToolComponent() {
        setReplicated(true);
    }

    /** Zwraca aktualnie wybrane narzedzie. */
    public PlayerToolType getActiveTool() {
        return PlayerToolType.fromOrdinal(activeToolOrdinal);
    }

    /** Ustawia nowe narzedzie i od razu oznacza komponent jako zmieniony. */
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

    /** Przechodzi na nastepne narzedzie z listy. */
    public void cycleNext() {
        setActiveTool(getActiveTool().next());
    }

    /** Przechodzi na poprzednie narzedzie z listy. */
    public void cyclePrevious() {
        setActiveTool(getActiveTool().previous());
    }

    /** Zwraca bindowalna wartosc aktywnego narzedzia do UI. */
    public PropertyBinding<PlayerToolType> getActiveToolBinding() {
        return activeToolBinding;
    }

    /** Odpala sie po replikacji i aktualizuje binding po stronie klienta. */
    @SuppressWarnings("unused")
    private void onActiveToolChanged() {
        activeToolBinding.set(getActiveTool());
    }
}

package com.polsl.poiw.gameplay.gamemode;

import com.polsl.poiw.engine.gameframework.GameMode;
import com.polsl.poiw.engine.ui.HUD;
import com.polsl.poiw.gameplay.character.PlayerCharacter;

/**
 * Domyślny GameMode — konfiguruje klasy postaci, controllera i HUD.
 */
public class MainGameMode extends GameMode {

    public MainGameMode() {
        setDefaultPawnClass(PlayerCharacter.class);
        setHudClass(HUD.class);
    }
}

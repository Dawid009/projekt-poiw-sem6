package com.polsl.poiw.engine.settings;

import java.util.Objects;

public record GraphicsSettings(ResolutionOption resolution, boolean vSyncEnabled, int fpsLimit) {

    public GraphicsSettings {
        Objects.requireNonNull(resolution, "resolution cannot be null");
    }
}
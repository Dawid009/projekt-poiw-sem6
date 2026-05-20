package com.polsl.poiw.engine.settings;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Preferences;
import com.polsl.poiw.Main;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GraphicsSettingsService {

    private static final String PREFS_NAME = "poiw-graphics";
    private static final String KEY_WIDTH = "width";
    private static final String KEY_HEIGHT = "height";
    private static final String KEY_VSYNC = "vsync";
    private static final String KEY_FPS_LIMIT = "fpsLimit";

    private static final GraphicsSettings DEFAULT_SETTINGS = new GraphicsSettings(
        new ResolutionOption(Main.REFERENCE_WIDTH, 720),
        true,
        60
    );

    private static final List<Integer> FPS_LIMITS = List.of(24, 30, 60, 90, 120, 144, 180, 200, 240);

    private static GraphicsSettings appliedSettings;

    private GraphicsSettingsService() {
    }

    public static void initialize() {
        if (appliedSettings == null) {
            appliedSettings = sanitize(loadSavedSettings());
        }
    }

    public static GraphicsSettings getAppliedSettings() {
        initialize();
        return appliedSettings;
    }

    public static GraphicsSettings getDefaultSettings() {
        return DEFAULT_SETTINGS;
    }

    public static List<Integer> getAvailableFpsLimits() {
        return FPS_LIMITS;
    }

    public static List<ResolutionOption> getAvailableResolutions() {
        Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
        int maxWidth = displayMode.width;
        int maxHeight = displayMode.height;

        Map<String, ResolutionOption> uniqueResolutions = new LinkedHashMap<>();
        addResolution(uniqueResolutions, 960, 540, maxWidth, maxHeight);
        addResolution(uniqueResolutions, 1024, 576, maxWidth, maxHeight);
        addResolution(uniqueResolutions, 1152, 648, maxWidth, maxHeight);
        addResolution(uniqueResolutions, 1280, 720, maxWidth, maxHeight);
        addResolution(uniqueResolutions, 1366, 768, maxWidth, maxHeight);
        addResolution(uniqueResolutions, 1600, 900, maxWidth, maxHeight);
        addResolution(uniqueResolutions, 1920, 1080, maxWidth, maxHeight);
        addResolution(uniqueResolutions, 2560, 1440, maxWidth, maxHeight);

        for (Graphics.DisplayMode mode : Gdx.graphics.getDisplayModes()) {
            addResolution(uniqueResolutions, mode.width, mode.height, maxWidth, maxHeight);
        }

        addResolution(uniqueResolutions, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), maxWidth, maxHeight);

        List<ResolutionOption> sorted = new ArrayList<>(uniqueResolutions.values());
        sorted.sort(Comparator.comparingInt(ResolutionOption::width).thenComparingInt(ResolutionOption::height));
        return sorted;
    }

    public static GraphicsSettings applySettings(GraphicsSettings settings) {
        GraphicsSettings normalized = sanitize(settings);

        Gdx.graphics.setWindowedMode(normalized.resolution().width(), normalized.resolution().height());
        Gdx.graphics.setVSync(normalized.vSyncEnabled());
        Gdx.graphics.setForegroundFPS(normalized.fpsLimit());

        appliedSettings = normalized;
        return normalized;
    }

    public static GraphicsSettings saveSettings(GraphicsSettings settings) {
        GraphicsSettings normalized = sanitize(settings);
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putInteger(KEY_WIDTH, normalized.resolution().width());
        prefs.putInteger(KEY_HEIGHT, normalized.resolution().height());
        prefs.putBoolean(KEY_VSYNC, normalized.vSyncEnabled());
        prefs.putInteger(KEY_FPS_LIMIT, normalized.fpsLimit());
        prefs.flush();
        appliedSettings = normalized;
        return normalized;
    }

    public static GraphicsSettings loadSavedSettings() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        ResolutionOption resolution = new ResolutionOption(
            prefs.getInteger(KEY_WIDTH, DEFAULT_SETTINGS.resolution().width()),
            prefs.getInteger(KEY_HEIGHT, DEFAULT_SETTINGS.resolution().height())
        );

        return new GraphicsSettings(
            resolution,
            prefs.getBoolean(KEY_VSYNC, DEFAULT_SETTINGS.vSyncEnabled()),
            prefs.getInteger(KEY_FPS_LIMIT, DEFAULT_SETTINGS.fpsLimit())
        );
    }

    private static GraphicsSettings sanitize(GraphicsSettings settings) {
        ResolutionOption resolution = findMatchingResolution(settings.resolution());
        int fpsLimit = FPS_LIMITS.contains(settings.fpsLimit())
            ? settings.fpsLimit()
            : DEFAULT_SETTINGS.fpsLimit();

        return new GraphicsSettings(resolution, settings.vSyncEnabled(), fpsLimit);
    }

    private static ResolutionOption findMatchingResolution(ResolutionOption requested) {
        for (ResolutionOption option : getAvailableResolutions()) {
            if (option.width() == requested.width() && option.height() == requested.height()) {
                return option;
            }
        }

        return DEFAULT_SETTINGS.resolution();
    }

    private static void addResolution(Map<String, ResolutionOption> resolutions,
                                      int width, int height,
                                      int maxWidth, int maxHeight) {
        if (width <= 0 || height <= 0 || width > maxWidth || height > maxHeight) {
            return;
        }

        String key = width + "x" + height;
        resolutions.putIfAbsent(key, new ResolutionOption(width, height));
    }
}
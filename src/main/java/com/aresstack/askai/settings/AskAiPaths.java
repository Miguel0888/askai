package com.aresstack.askai.settings;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralizes user-specific storage locations for the spike application.
 */
public final class AskAiPaths {

    private static final String APP_DIR_NAME = ".askai";

    private AskAiPaths() {
    }

    public static Path appDirectory() {
        String appData = System.getenv("APPDATA");
        if (appData == null || appData.trim().isEmpty()) {
            appData = System.getProperty("user.home");
        }
        return Paths.get(appData, APP_DIR_NAME);
    }

    public static Path settingsFile() {
        return appDirectory().resolve("settings.properties");
    }

    public static Path downloadOverridesFile() {
        return appDirectory().resolve("download-overrides.json");
    }

    public static Path defaultModelRoot() {
        return appDirectory().resolve("models");
    }
}

package com.aresstack.askai.settings;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable application settings used by the Workbench panels.
 */
public final class AskAiSettings {

    private final String ollamaBaseUrl;
    private final Path modelRoot;
    private final String defaultQuantization;
    private final String defaultKeepAlive;

    public AskAiSettings(String ollamaBaseUrl, Path modelRoot,
                               String defaultQuantization, String defaultKeepAlive) {
        this.ollamaBaseUrl = normalizeBaseUrl(ollamaBaseUrl);
        this.modelRoot = Objects.requireNonNull(modelRoot, "modelRoot");
        this.defaultQuantization = defaultQuantization == null ? "" : defaultQuantization.trim();
        this.defaultKeepAlive = defaultKeepAlive == null ? "5m" : defaultKeepAlive.trim();
    }

    public static AskAiSettings defaults() {
        return new AskAiSettings("http://10.126.26.41:11434",
                AskAiPaths.defaultModelRoot(), "", "5m");
    }

    public String getOllamaBaseUrl() {
        return ollamaBaseUrl;
    }

    public Path getModelRoot() {
        return modelRoot;
    }

    public String getDefaultQuantization() {
        return defaultQuantization;
    }

    public String getDefaultKeepAlive() {
        return defaultKeepAlive;
    }

    public AskAiSettings withOllamaBaseUrl(String newBaseUrl) {
        return new AskAiSettings(newBaseUrl, modelRoot, defaultQuantization, defaultKeepAlive);
    }

    public AskAiSettings withModelRoot(Path newModelRoot) {
        return new AskAiSettings(ollamaBaseUrl, newModelRoot, defaultQuantization, defaultKeepAlive);
    }

    public AskAiSettings withDefaultQuantization(String newDefaultQuantization) {
        return new AskAiSettings(ollamaBaseUrl, modelRoot, newDefaultQuantization, defaultKeepAlive);
    }

    public AskAiSettings withDefaultKeepAlive(String newDefaultKeepAlive) {
        return new AskAiSettings(ollamaBaseUrl, modelRoot, defaultQuantization, newDefaultKeepAlive);
    }

    private static String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            normalized = "http://127.0.0.1:11434";
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

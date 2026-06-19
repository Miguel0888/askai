package com.aresstack.askai.settings;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists simple application settings to a properties file.
 */
public final class AskAiSettingsStore {

    private static final Logger LOG = Logger.getLogger(AskAiSettingsStore.class.getName());

    private static final String OLLAMA_BASE_URL = "ollama.baseUrl";
    private static final String MODEL_ROOT = "model.root";
    private static final String DEFAULT_QUANTIZATION = "ollama.defaultQuantization";
    private static final String DEFAULT_KEEP_ALIVE = "ollama.defaultKeepAlive";

    private final Path settingsFile;

    public AskAiSettingsStore() {
        this(AskAiPaths.settingsFile());
    }

    public AskAiSettingsStore(Path settingsFile) {
        this.settingsFile = settingsFile;
    }

    public AskAiSettings load() {
        AskAiSettings defaults = AskAiSettings.defaults();
        if (!Files.isRegularFile(settingsFile)) {
            return defaults;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(settingsFile)) {
            properties.load(reader);
            return new AskAiSettings(
                    properties.getProperty(OLLAMA_BASE_URL, defaults.getOllamaBaseUrl()),
                    Paths.get(properties.getProperty(MODEL_ROOT, defaults.getModelRoot().toString())),
                    properties.getProperty(DEFAULT_QUANTIZATION, defaults.getDefaultQuantization()),
                    properties.getProperty(DEFAULT_KEEP_ALIVE, defaults.getDefaultKeepAlive())
            );
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not load settings from " + settingsFile, ex);
            return defaults;
        }
    }

    public void save(AskAiSettings settings) {
        Properties properties = new Properties();
        properties.setProperty(OLLAMA_BASE_URL, settings.getOllamaBaseUrl());
        properties.setProperty(MODEL_ROOT, settings.getModelRoot().toString());
        properties.setProperty(DEFAULT_QUANTIZATION, settings.getDefaultQuantization());
        properties.setProperty(DEFAULT_KEEP_ALIVE, settings.getDefaultKeepAlive());

        try {
            Path parent = settingsFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(settingsFile)) {
                properties.store(writer, "AskAI settings");
            }
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "Could not save settings to " + settingsFile, ex);
        }
    }
}

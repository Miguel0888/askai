package com.aresstack.askai.importing;

import com.aresstack.askai.catalog.OllamaModelImportProfile;

import java.nio.file.Path;

/**
 * Input for uploading a local model directory to an Ollama server.
 */
public final class OllamaImportPlan {

    private final Path modelDirectory;
    private final String ollamaModelName;
    private final String quantization;
    private final OllamaModelImportProfile importProfile;

    public OllamaImportPlan(Path modelDirectory, String ollamaModelName, String quantization,
                            OllamaModelImportProfile importProfile) {
        this.modelDirectory = modelDirectory;
        this.ollamaModelName = ollamaModelName;
        this.quantization = quantization == null ? "" : quantization.trim();
        this.importProfile = importProfile == null ? OllamaModelImportProfile.plain() : importProfile;
    }

    public Path getModelDirectory() {
        return modelDirectory;
    }

    public String getOllamaModelName() {
        return ollamaModelName;
    }

    public String getQuantization() {
        return quantization;
    }

    public OllamaModelImportProfile getImportProfile() {
        return importProfile;
    }
}

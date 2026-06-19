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
    private final AskAiModelInstallation installation;

    public OllamaImportPlan(Path modelDirectory, String ollamaModelName, String quantization,
                            OllamaModelImportProfile importProfile) {
        this(modelDirectory, ollamaModelName, quantization, importProfile, null);
    }

    public OllamaImportPlan(Path modelDirectory, String ollamaModelName, String quantization,
                            OllamaModelImportProfile importProfile, AskAiModelInstallation installation) {
        this.modelDirectory = modelDirectory;
        this.ollamaModelName = ollamaModelName;
        this.quantization = quantization == null ? "" : quantization.trim();
        this.importProfile = importProfile == null ? OllamaModelImportProfile.plain() : importProfile;
        this.installation = installation;
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

    public AskAiModelInstallation getInstallation() {
        return installation;
    }

    public boolean hasManagedInstallation() {
        return installation != null;
    }
}

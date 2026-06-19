package com.aresstack.askai.importing;

import java.nio.file.Path;

/**
 * Input for uploading a local model directory to an Ollama server.
 */
public final class OllamaImportPlan {

    private final Path modelDirectory;
    private final String ollamaModelName;
    private final String quantization;

    public OllamaImportPlan(Path modelDirectory, String ollamaModelName, String quantization) {
        this.modelDirectory = modelDirectory;
        this.ollamaModelName = ollamaModelName;
        this.quantization = quantization == null ? "" : quantization.trim();
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
}

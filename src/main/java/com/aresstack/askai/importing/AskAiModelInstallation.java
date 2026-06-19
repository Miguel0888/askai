package com.aresstack.askai.importing;

import java.nio.file.Path;

/**
 * Points to all files that belong to one locally managed AskAI model installation.
 */
public final class AskAiModelInstallation {

    private final String modelId;
    private final Path rootDirectory;
    private final Path sourceDirectory;
    private final Path ollamaDirectory;
    private final Path modelfile;
    private final Path createRequestFile;
    private final Path installMetadataFile;

    public AskAiModelInstallation(String modelId, Path rootDirectory) {
        this.modelId = modelId;
        this.rootDirectory = rootDirectory;
        this.sourceDirectory = rootDirectory.resolve("source");
        this.ollamaDirectory = rootDirectory.resolve("ollama");
        this.modelfile = ollamaDirectory.resolve("Modelfile");
        this.createRequestFile = ollamaDirectory.resolve("create-request.json");
        this.installMetadataFile = ollamaDirectory.resolve("install.json");
    }

    public String getModelId() {
        return modelId;
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public Path getSourceDirectory() {
        return sourceDirectory;
    }

    public Path getOllamaDirectory() {
        return ollamaDirectory;
    }

    public Path getModelfile() {
        return modelfile;
    }

    public Path getCreateRequestFile() {
        return createRequestFile;
    }

    public Path getInstallMetadataFile() {
        return installMetadataFile;
    }
}

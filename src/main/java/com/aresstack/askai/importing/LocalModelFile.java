package com.aresstack.askai.importing;

import java.nio.file.Path;

/**
 * Describes a local model file that can be uploaded to Ollama as a blob.
 */
public final class LocalModelFile {

    private final Path file;
    private final String relativePath;
    private final long sizeBytes;

    public LocalModelFile(Path file, String relativePath, long sizeBytes) {
        this.file = file;
        this.relativePath = relativePath;
        this.sizeBytes = sizeBytes;
    }

    public Path getFile() {
        return file;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }
}

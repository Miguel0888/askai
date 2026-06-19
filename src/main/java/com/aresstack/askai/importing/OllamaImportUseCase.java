package com.aresstack.askai.importing;

import com.aresstack.askai.client.OllamaClient;
import com.aresstack.askai.client.OllamaRequestException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uploads local model files as Ollama blobs and creates the Ollama model entry.
 */
public final class OllamaImportUseCase {

    private final OllamaClient ollamaClient;
    private final LocalModelScanner scanner;
    private final Sha256DigestService digestService;

    public OllamaImportUseCase(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.scanner = new LocalModelScanner();
        this.digestService = new Sha256DigestService();
    }

    public String execute(OllamaImportPlan plan, OllamaImportListener listener)
            throws IOException, OllamaRequestException {
        List<LocalModelFile> files = scanner.scanImportableFiles(plan.getModelDirectory());
        if (files.isEmpty()) {
            throw new IOException("No importable files found in " + plan.getModelDirectory());
        }

        Map<String, String> digestByRelativePath = new LinkedHashMap<String, String>();
        int index = 0;
        for (LocalModelFile file : files) {
            index++;
            listener.onProgress(percent(index - 1, files.size()), "Hashing " + file.getRelativePath());
            listener.onMessage("Hashing " + file.getRelativePath() + " (" + humanBytes(file.getSizeBytes()) + ")");
            String digest = digestService.digest(file.getFile());
            digestByRelativePath.put(file.getRelativePath(), digest);

            listener.onProgress(percent(index - 1, files.size()), "Checking blob " + file.getRelativePath());
            if (ollamaClient.blobExists(digest)) {
                listener.onMessage("Blob exists: " + file.getRelativePath() + " -> " + digest);
            } else {
                listener.onMessage("Uploading blob: " + file.getRelativePath() + " -> " + digest);
                ollamaClient.uploadBlob(digest, file.getFile());
                listener.onMessage("Uploaded blob: " + file.getRelativePath());
            }
            listener.onProgress(percent(index, files.size()), "Uploaded " + index + " / " + files.size());
        }

        listener.onMessage("Creating Ollama model: " + plan.getOllamaModelName());
        listener.onProgress(99, "Creating model");
        String result = ollamaClient.createModelFromFiles(plan.getOllamaModelName(), digestByRelativePath,
                plan.getQuantization());
        listener.onProgress(100, "Model created");
        return result;
    }

    private static int percent(int index, int total) {
        if (total <= 0) {
            return 100;
        }
        return Math.max(0, Math.min(100, (int) Math.round(index * 100.0d / total)));
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0d;
        if (kb < 1024.0d) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0d;
        if (mb < 1024.0d) {
            return String.format("%.1f MB", mb);
        }
        return String.format("%.2f GB", mb / 1024.0d);
    }
}

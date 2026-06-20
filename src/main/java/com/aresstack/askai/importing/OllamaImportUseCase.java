package com.aresstack.askai.importing;

import com.aresstack.askai.client.AskAiOllamaImportClient;
import com.aresstack.askai.client.OllamaCreateModelRequest;
import com.aresstack.askai.client.OllamaRequestException;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Uploads local model files as Ollama blobs and creates the Ollama model entry.
 */
public final class OllamaImportUseCase {

    private final AskAiOllamaImportClient ollamaClient;
    private final LocalModelScanner scanner;
    private final Sha256DigestService digestService;
    private final AskAiModelStore modelStore;

    public OllamaImportUseCase(AskAiOllamaImportClient ollamaClient) {
        this.ollamaClient = ollamaClient;
        this.scanner = new LocalModelScanner();
        this.digestService = new Sha256DigestService();
        this.modelStore = new AskAiModelStore();
    }

    public String execute(OllamaImportPlan plan, OllamaImportListener listener)
            throws IOException, OllamaRequestException {
        List<LocalModelFile> files = scanner.scanImportableFiles(plan.getModelDirectory());
        if (files.isEmpty()) {
            throw new IOException("No importable files found in " + plan.getModelDirectory());
        }

        Map<String, String> digestByRelativePath = uploadModelFiles(files, listener);
        OllamaCreateModelRequest createRequest = new OllamaCreateModelRequest(plan.getOllamaModelName(),
                digestByRelativePath, plan.getQuantization(), plan.getImportProfile());
        writeVisibleInstallationArtifacts(plan, createRequest, listener);

        listener.onMessage("Creating Ollama model: " + plan.getOllamaModelName());
        listener.onProgress(99, "Creating model");
        listener.onMessage("Using Ollama import profile: " + plan.getImportProfile().getDisplayName());
        String result = ollamaClient.createModel(createRequest);
        listener.onProgress(100, "Model created");
        return result;
    }

    private Map<String, String> uploadModelFiles(List<LocalModelFile> files, OllamaImportListener listener)
            throws IOException, OllamaRequestException {
        Map<String, String> digestByRelativePath = new LinkedHashMap<String, String>();
        int index = 0;
        for (LocalModelFile file : files) {
            index++;
            listener.onProgress(percent(index - 1, files.size()), "Hashing " + file.getRelativePath());
            listener.onMessage("Hashing " + file.getRelativePath() + " (" + humanBytes(file.getSizeBytes()) + ")");
            String digest = digestService.digest(file.getFile());
            digestByRelativePath.put(file.getRelativePath(), digest);

            listener.onProgress(percent(index - 1, files.size()), "Checking blob " + file.getRelativePath());
            uploadMissingBlob(file, digest, listener);
            listener.onProgress(percent(index, files.size()), "Uploaded " + index + " / " + files.size());
        }
        return digestByRelativePath;
    }

    private void uploadMissingBlob(LocalModelFile file, String digest, OllamaImportListener listener)
            throws IOException, OllamaRequestException {
        if (ollamaClient.blobExists(digest)) {
            listener.onMessage("Blob exists: " + file.getRelativePath() + " -> " + digest);
            return;
        }
        listener.onMessage("Uploading blob: " + file.getRelativePath() + " -> " + digest);
        ollamaClient.uploadBlob(digest, file.getFile());
        listener.onMessage("Uploaded blob: " + file.getRelativePath());
    }

    private void writeVisibleInstallationArtifacts(OllamaImportPlan plan, OllamaCreateModelRequest createRequest,
                                                   OllamaImportListener listener) throws IOException {
        if (!plan.hasManagedInstallation()) {
            return;
        }
        listener.onMessage("Writing AskAI model artifacts: " + plan.getInstallation().getOllamaDirectory());
        modelStore.writeOllamaArtifacts(plan.getInstallation(), createRequest, plan);
        listener.onMessage("Wrote Modelfile: " + plan.getInstallation().getModelfile());
        listener.onMessage("Wrote create request: " + plan.getInstallation().getCreateRequestFile());
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

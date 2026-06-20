package com.aresstack.askai.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Isolated adapter for the Ollama model-import endpoints that {@code ollama4j} does not
 * support in a fitting way.
 *
 * <p>{@code ollama4j} exposes no blob upload at all, and its {@code createModel} models
 * {@code quantize} as a boolean rather than the quantization-level string AskAI needs.
 * This client therefore keeps a small, typed HTTP adapter strictly for the import path:
 * {@code /api/blobs/:digest} (HEAD + POST) and {@code /api/create}. It is deliberately
 * separate from {@link AskAiOllamaClient} and never used for the regular REST surface.
 * All digests pass through {@link OllamaDigest} so the {@code sha256:} prefix is set
 * exactly once.</p>
 */
public final class AskAiOllamaImportClient {

    private final String baseUrl;
    private final HttpClient httpClient;

    public AskAiOllamaImportClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public boolean blobExists(String digest) throws OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(blobEndpoint(digest))
                .timeout(Duration.ofMinutes(2))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() == 200) {
                return true;
            }
            if (response.statusCode() == 404) {
                return false;
            }
            throw new OllamaRequestException("Blob check failed: HTTP " + response.statusCode());
        } catch (IOException ex) {
            throw new OllamaRequestException("Blob check failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OllamaRequestException("Blob check interrupted", ex);
        }
    }

    public void uploadBlob(String digest, Path file) throws IOException, OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(blobEndpoint(digest))
                .timeout(Duration.ofHours(12))
                .POST(HttpRequest.BodyPublishers.ofFile(file))
                .build();
        sendText(request);
    }

    public String createModel(OllamaCreateModelRequest createRequest) throws OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/create"))
                .timeout(Duration.ofHours(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createRequest.toJson()))
                .build();
        return sendText(request);
    }

    private String sendText(HttpRequest request) throws OllamaRequestException {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            throw new OllamaRequestException("HTTP " + response.statusCode() + ": " + response.body());
        } catch (IOException ex) {
            throw new OllamaRequestException("Request failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OllamaRequestException("Request interrupted", ex);
        }
    }

    private URI blobEndpoint(String digest) {
        return endpoint("/api/blobs/" + OllamaDigest.prefixed(digest));
    }

    private URI endpoint(String path) {
        return URI.create(baseUrl + path);
    }

    private static String normalizeBaseUrl(String url) {
        String normalized = url == null || url.trim().isEmpty() ? "http://127.0.0.1:11434" : url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

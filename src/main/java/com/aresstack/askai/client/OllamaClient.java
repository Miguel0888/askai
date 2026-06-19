package com.aresstack.askai.client;

import com.aresstack.askai.util.JsonSupport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin HTTP client for the local/remote Ollama API.
 */
public final class OllamaClient {

    private final HttpClient httpClient;
    private final String baseUrl;

    public OllamaClient(String baseUrl) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String getVersion() throws OllamaRequestException {
        return sendText(HttpRequest.newBuilder(endpoint("/api/version"))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build());
    }

    public String getTagsJson() throws OllamaRequestException {
        return sendText(HttpRequest.newBuilder(endpoint("/api/tags"))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build());
    }

    public List<String> getModelNames() throws OllamaRequestException {
        return JsonSupport.extractModelNames(getTagsJson());
    }

    public String getRunningModelsJson() throws OllamaRequestException {
        return sendText(HttpRequest.newBuilder(endpoint("/api/ps"))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build());
    }

    public boolean blobExists(String digest) throws OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/blobs/" + digest))
                .timeout(Duration.ofSeconds(30))
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
            throw new OllamaRequestException("Blob HEAD failed with HTTP " + response.statusCode());
        } catch (IOException ex) {
            throw new OllamaRequestException("Blob HEAD failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OllamaRequestException("Blob HEAD interrupted", ex);
        }
    }

    public String uploadBlob(String digest, Path file) throws OllamaRequestException {
        HttpRequest.BodyPublisher bodyPublisher;
        try {
            bodyPublisher = HttpRequest.BodyPublishers.ofFile(file);
        } catch (java.io.FileNotFoundException ex) {
            throw new OllamaRequestException("Blob file not found: " + file, ex);
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/blobs/" + digest))
                .timeout(Duration.ofHours(6))
                .POST(bodyPublisher)
                .build();
        return sendText(request);
    }

    public String createModelFromFiles(String modelName, Map<String, String> files,
                                       String quantization) throws OllamaRequestException {
        String body = createModelJson(modelName, files, quantization);
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/create"))
                .timeout(Duration.ofHours(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return sendText(request);
    }

    public String deleteModel(String modelName) throws OllamaRequestException {
        String body = "{\"model\":" + JsonSupport.quote(modelName) + "}";
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/delete"))
                .timeout(Duration.ofMinutes(2))
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return sendText(request);
    }

    public String chat(String modelName, String systemPrompt, String userPrompt, String keepAlive)
            throws OllamaRequestException {
        String body = chatJson(modelName, systemPrompt, userPrompt, keepAlive);
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/chat"))
                .timeout(Duration.ofMinutes(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
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
            throw new OllamaRequestException("HTTP request failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OllamaRequestException("HTTP request interrupted", ex);
        }
    }

    private URI endpoint(String path) {
        return URI.create(baseUrl + path);
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

    private static String createModelJson(String modelName, Map<String, String> files, String quantization) {
        LinkedHashMap<String, String> stableFiles = new LinkedHashMap<String, String>(files);
        StringBuilder builder = new StringBuilder(256 + stableFiles.size() * 96);
        builder.append('{');
        builder.append("\"model\":").append(JsonSupport.quote(modelName));
        builder.append(",\"files\":{");
        boolean first = true;
        for (Map.Entry<String, String> entry : stableFiles.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            builder.append(JsonSupport.quote(entry.getKey())).append(':').append(JsonSupport.quote(entry.getValue()));
            first = false;
        }
        builder.append('}');
        String cleanQuantization = quantization == null ? "" : quantization.trim();
        if (!cleanQuantization.isEmpty()) {
            builder.append(",\"quantize\":").append(JsonSupport.quote(cleanQuantization));
        }
        builder.append(",\"stream\":false");
        builder.append('}');
        return builder.toString();
    }

    private static String chatJson(String modelName, String systemPrompt, String userPrompt, String keepAlive) {
        StringBuilder builder = new StringBuilder(512);
        builder.append('{');
        builder.append("\"model\":").append(JsonSupport.quote(modelName)).append(',');
        builder.append("\"messages\":[");
        String cleanSystem = systemPrompt == null ? "" : systemPrompt.trim();
        boolean hasSystem = !cleanSystem.isEmpty();
        if (hasSystem) {
            builder.append("{\"role\":\"system\",\"content\":")
                    .append(JsonSupport.quote(cleanSystem)).append("},");
        }
        builder.append("{\"role\":\"user\",\"content\":")
                .append(JsonSupport.quote(userPrompt)).append("}");
        builder.append("],\"stream\":false");
        String cleanKeepAlive = keepAlive == null ? "" : keepAlive.trim();
        if (!cleanKeepAlive.isEmpty()) {
            builder.append(",\"keep_alive\":").append(JsonSupport.quote(cleanKeepAlive));
        }
        builder.append('}');
        return builder.toString();
    }
}

package com.aresstack.askai.client;

import com.aresstack.askai.catalog.OllamaModelImportProfile;
import com.aresstack.askai.util.JsonSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Small Ollama HTTP client used by the Swing app.
 */
public final class OllamaClient {

    private final String baseUrl;
    private final HttpClient httpClient;

    public OllamaClient(String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String getTagsJson() throws OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/tags"))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        return sendText(request);
    }

    public List<String> getModelNames() throws OllamaRequestException {
        return JsonSupport.extractModelNames(getTagsJson());
    }

    public String getRunningModelsJson() throws OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/ps"))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        return sendText(request);
    }

    public String getVersion() throws OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/version"))
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        return sendText(request);
    }

    public String deleteModel(String modelName) throws OllamaRequestException {
        String body = "{\"model\":" + JsonSupport.quote(modelName) + "}";
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/delete"))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                .build();
        return sendText(request);
    }

    public String chat(String modelName, String systemPrompt, String userPrompt, String keepAlive)
            throws OllamaRequestException {
        String body = chatJson(modelName, systemPrompt, userPrompt, keepAlive, false);
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/chat"))
                .timeout(Duration.ofHours(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return sendText(request);
    }

    public void streamChat(String modelName, String systemPrompt, String userPrompt, String keepAlive,
                           OllamaChatStreamListener listener) throws OllamaRequestException {
        String body = chatJson(modelName, systemPrompt, userPrompt, keepAlive, true);
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/chat"))
                .timeout(Duration.ofHours(6))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String responseBody = readAll(response.body());
                throw new OllamaRequestException("HTTP " + response.statusCode() + ": " + responseBody);
            }
            readChatStream(response.body(), listener);
        } catch (IOException ex) {
            throw new OllamaRequestException("Chat stream failed: " + ex.getMessage(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new OllamaRequestException("Chat stream interrupted", ex);
        }
    }

    public boolean blobExists(String digest) throws OllamaRequestException {
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/blobs/sha256:" + digest))
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
        HttpRequest request = HttpRequest.newBuilder(endpoint("/api/blobs/sha256:" + digest))
                .timeout(Duration.ofHours(12))
                .POST(HttpRequest.BodyPublishers.ofFile(file))
                .build();
        sendText(request);
    }

    public String createModelFromFiles(String modelName, Map<String, String> digestByRelativePath, String quantization,
                                       OllamaModelImportProfile importProfile)
            throws OllamaRequestException {
        return createModel(new OllamaCreateModelRequest(modelName, digestByRelativePath, quantization, importProfile));
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

    private static String chatJson(String modelName, String systemPrompt, String userPrompt, String keepAlive, boolean stream) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"model\":").append(JsonSupport.quote(modelName));
        builder.append(",\"messages\":[");
        boolean hasSystem = systemPrompt != null && !systemPrompt.trim().isEmpty();
        if (hasSystem) {
            builder.append("{\"role\":\"system\",\"content\":")
                    .append(JsonSupport.quote(systemPrompt))
                    .append("},");
        }
        builder.append("{\"role\":\"user\",\"content\":")
                .append(JsonSupport.quote(userPrompt))
                .append("}");
        builder.append("],\"stream\":").append(stream ? "true" : "false");
        if (keepAlive != null && !keepAlive.trim().isEmpty()) {
            builder.append(",\"keep_alive\":").append(JsonSupport.quote(keepAlive.trim()));
        }
        builder.append('}');
        return builder.toString();
    }

    private void readChatStream(InputStream inputStream, OllamaChatStreamListener listener) throws IOException {
        String finalLine = "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    listener.onStatus("Chat request was cancelled.");
                    return;
                }
                if (line.trim().isEmpty()) {
                    continue;
                }
                finalLine = line;
                String content = JsonSupport.extractChatMessageContent(line);
                if (!content.isEmpty()) {
                    listener.onContent(content);
                }
                if (line.contains("\"done\":true")) {
                    listener.onComplete(line);
                    return;
                }
            }
        }
        listener.onComplete(finalLine);
    }

    private static String readAll(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.lineSeparator());
            }
        }
        return builder.toString();
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

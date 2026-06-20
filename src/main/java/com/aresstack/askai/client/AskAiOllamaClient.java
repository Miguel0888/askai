package com.aresstack.askai.client;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResponseModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.chat.OllamaChatTokenHandler;

import java.util.List;

/**
 * AskAI adapter over the {@code ollama4j} library.
 *
 * <p>This is the only place where AskAI talks to {@code ollama4j} for the regular
 * Ollama REST surface (version, installed/running models, chat, delete, details).
 * It maps ollama4j DTOs to AskAI domain models via {@link OllamaResponseMapper} so
 * the UI never sees ollama4j types or raw JSON. The blob upload and model create
 * endpoints are not supported by ollama4j in a fitting way and live in the isolated
 * {@link AskAiOllamaImportClient}.</p>
 */
public final class AskAiOllamaClient {

    /** Generous timeout so CPU-only chat generations are not cut off. */
    private static final long REQUEST_TIMEOUT_SECONDS = 6L * 60L * 60L;

    private final Ollama ollama;

    public AskAiOllamaClient(String baseUrl) {
        this.ollama = new Ollama(normalizeBaseUrl(baseUrl));
        this.ollama.setRequestTimeoutSeconds(REQUEST_TIMEOUT_SECONDS);
    }

    public String getVersion() throws OllamaRequestException {
        try {
            return ollama.getVersion();
        } catch (OllamaException ex) {
            throw wrap("Could not read Ollama version", ex);
        }
    }

    public OllamaModelListResponse getInstalledModels() throws OllamaRequestException {
        try {
            return OllamaResponseMapper.toInstalledModels(ollama.listModels());
        } catch (OllamaException ex) {
            throw wrap("Could not list installed models", ex);
        }
    }

    public List<String> getModelNames() throws OllamaRequestException {
        java.util.ArrayList<String> names = new java.util.ArrayList<String>();
        for (OllamaModelInfo info : getInstalledModels().getModels()) {
            names.add(info.getDisplayName());
        }
        return names;
    }

    public OllamaRunningModelListResponse getRunningModels() throws OllamaRequestException {
        try {
            return OllamaResponseMapper.toRunningModels(ollama.ps());
        } catch (OllamaException ex) {
            throw wrap("Could not list running models", ex);
        }
    }

    public OllamaModelDetails getModelDetails(String modelName) throws OllamaRequestException {
        try {
            return OllamaResponseMapper.toDetails(ollama.getModelDetails(modelName).getDetails());
        } catch (OllamaException ex) {
            throw wrap("Could not read details for " + modelName, ex);
        }
    }

    public void deleteModel(String modelName) throws OllamaRequestException {
        try {
            ollama.deleteModel(modelName, true);
        } catch (OllamaException ex) {
            throw wrap("Could not delete " + modelName, ex);
        }
    }

    public OllamaChatCompletion chat(String modelName, String systemPrompt, String userPrompt, String keepAlive)
            throws OllamaRequestException {
        OllamaChatRequest request = buildChatRequest(modelName, systemPrompt, userPrompt, keepAlive, false);
        try {
            OllamaChatResult result = ollama.chat(request, null);
            return toCompletion(result.getResponseModel());
        } catch (OllamaException ex) {
            throw wrap("Chat request failed", ex);
        }
    }

    public OllamaChatCompletion streamChat(String modelName, String systemPrompt, String userPrompt, String keepAlive,
                                           OllamaChatStreamListener listener) throws OllamaRequestException {
        OllamaChatRequest request = buildChatRequest(modelName, systemPrompt, userPrompt, keepAlive, true);
        OllamaChatTokenHandler handler = (OllamaChatResponseModel chunk) -> {
            String delta = chunkContent(chunk);
            if (!delta.isEmpty()) {
                listener.onContent(delta);
            }
        };
        try {
            OllamaChatResult result = ollama.chat(request, handler);
            OllamaChatCompletion completion = toCompletion(result.getResponseModel());
            listener.onComplete(completion);
            return completion;
        } catch (OllamaException ex) {
            throw wrap("Chat stream failed", ex);
        }
    }

    private static OllamaChatRequest buildChatRequest(String modelName, String systemPrompt, String userPrompt,
                                                      String keepAlive, boolean stream) {
        OllamaChatRequest request = OllamaChatRequest.builder().withModel(modelName);
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            request = request.withMessage(OllamaChatMessageRole.SYSTEM, systemPrompt);
        }
        request = request.withMessage(OllamaChatMessageRole.USER, userPrompt);
        if (keepAlive != null && !keepAlive.trim().isEmpty()) {
            request = request.withKeepAlive(keepAlive.trim());
        }
        if (stream) {
            request = request.withStreaming();
        }
        return request.build();
    }

    private static OllamaChatCompletion toCompletion(OllamaChatResponseModel responseModel) {
        if (responseModel == null) {
            return OllamaChatCompletion.empty();
        }
        String content = responseModel.getMessage() == null ? "" : responseModel.getMessage().getResponse();
        long evalCount = responseModel.getEvalCount() == null ? 0L : responseModel.getEvalCount().longValue();
        long evalDuration = responseModel.getEvalDuration() == null ? 0L : responseModel.getEvalDuration().longValue();
        return new OllamaChatCompletion(content, evalCount, evalDuration);
    }

    private static String chunkContent(OllamaChatResponseModel chunk) {
        if (chunk == null || chunk.getMessage() == null) {
            return "";
        }
        String content = chunk.getMessage().getResponse();
        return content == null ? "" : content;
    }

    private static OllamaRequestException wrap(String context, OllamaException ex) {
        return new OllamaRequestException(context + ": " + ex.getMessage(), ex);
    }

    private static String normalizeBaseUrl(String url) {
        String normalized = url == null || url.trim().isEmpty() ? "http://127.0.0.1:11434" : url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}

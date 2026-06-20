package com.aresstack.askai.client;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatResponseModel;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.models.chat.OllamaChatTokenHandler;
import io.github.ollama4j.models.embed.OllamaEmbedRequest;
import io.github.ollama4j.models.embed.OllamaEmbedResult;
import io.github.ollama4j.models.generate.OllamaGenerateRequest;
import io.github.ollama4j.models.response.OllamaResult;

import java.util.ArrayList;
import java.util.List;

/**
 * AskAI adapter over the {@code ollama4j} library.
 *
 * <p>This is the only place where AskAI talks to {@code ollama4j} for the regular
 * Ollama REST surface: ping, version, installed/running models, model info, chat
 * (single- and multi-turn, streaming), generate, embeddings, pull, unload and delete.
 * It maps ollama4j DTOs to AskAI domain models via {@link OllamaResponseMapper} so the
 * UI never sees ollama4j types or raw JSON. The blob upload and model create endpoints
 * are not supported by ollama4j in a fitting way and live in the isolated
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

    public boolean ping() throws OllamaRequestException {
        try {
            return ollama.ping();
        } catch (OllamaException ex) {
            throw wrap("Could not reach Ollama", ex);
        }
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
        ArrayList<String> names = new ArrayList<String>();
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
        return getModelInfo(modelName).getDetails();
    }

    public OllamaModelInfoView getModelInfo(String modelName) throws OllamaRequestException {
        try {
            return OllamaResponseMapper.toModelInfoView(ollama.getModelDetails(modelName));
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

    public void unloadModel(String modelName) throws OllamaRequestException {
        try {
            ollama.unloadModel(modelName);
        } catch (OllamaException ex) {
            throw wrap("Could not unload " + modelName, ex);
        }
    }

    public void pullModel(String modelName, final OllamaPullListener listener) throws OllamaRequestException {
        try {
            if (listener == null) {
                ollama.pullModel(modelName);
                return;
            }
            ollama.pullModel(modelName, (status, response) ->
                    listener.onProgress(new OllamaPullProgress(
                            response == null ? status : response.getStatus(),
                            response == null ? 0L : response.getCompleted(),
                            response == null ? 0L : response.getTotal())));
        } catch (OllamaException ex) {
            throw wrap("Could not pull " + modelName, ex);
        }
    }

    public String generate(String modelName, String prompt) throws OllamaRequestException {
        OllamaGenerateRequest request = OllamaGenerateRequest.builder()
                .withModel(modelName)
                .withPrompt(prompt)
                .build();
        try {
            OllamaResult result = ollama.generate(request, null);
            return result == null || result.getResponse() == null ? "" : result.getResponse();
        } catch (OllamaException ex) {
            throw wrap("Generate failed", ex);
        }
    }

    public List<List<Double>> embed(String modelName, List<String> inputs) throws OllamaRequestException {
        OllamaEmbedRequest request = new OllamaEmbedRequest(modelName, inputs);
        try {
            OllamaEmbedResult result = ollama.embed(request);
            return result == null || result.getEmbeddings() == null
                    ? new ArrayList<List<Double>>()
                    : result.getEmbeddings();
        } catch (OllamaException ex) {
            throw wrap("Embedding failed", ex);
        }
    }

    public OllamaChatCompletion chat(String modelName, String systemPrompt, String userPrompt, String keepAlive)
            throws OllamaRequestException {
        return chat(modelName, conversationOf(systemPrompt, userPrompt), keepAlive);
    }

    public OllamaChatCompletion chat(String modelName, List<OllamaChatTurn> conversation, String keepAlive)
            throws OllamaRequestException {
        OllamaChatRequest request = buildChatRequest(modelName, conversation, keepAlive, false);
        try {
            OllamaChatResult result = ollama.chat(request, null);
            return toCompletion(result.getResponseModel());
        } catch (OllamaException ex) {
            throw wrap("Chat request failed", ex);
        }
    }

    public OllamaChatCompletion streamChat(String modelName, String systemPrompt, String userPrompt, String keepAlive,
                                           OllamaChatStreamListener listener) throws OllamaRequestException {
        return streamChat(modelName, conversationOf(systemPrompt, userPrompt), keepAlive, listener);
    }

    public OllamaChatCompletion streamChat(String modelName, List<OllamaChatTurn> conversation, String keepAlive,
                                           OllamaChatStreamListener listener) throws OllamaRequestException {
        OllamaChatRequest request = buildChatRequest(modelName, conversation, keepAlive, true);
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

    private static List<OllamaChatTurn> conversationOf(String systemPrompt, String userPrompt) {
        ArrayList<OllamaChatTurn> turns = new ArrayList<OllamaChatTurn>();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            turns.add(OllamaChatTurn.system(systemPrompt));
        }
        turns.add(OllamaChatTurn.user(userPrompt));
        return turns;
    }

    private static OllamaChatRequest buildChatRequest(String modelName, List<OllamaChatTurn> conversation,
                                                      String keepAlive, boolean stream) {
        OllamaChatRequest request = OllamaChatRequest.builder()
                .withModel(modelName)
                .withMessages(OllamaResponseMapper.toChatMessages(conversation));
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

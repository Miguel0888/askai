package com.aresstack.askai.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Installed-model list returned by Ollama /api/tags, mapped into AskAI domain models.
 */
public final class OllamaModelListResponse {

    private final List<OllamaModelInfo> models;

    public OllamaModelListResponse(List<OllamaModelInfo> models) {
        this.models = Collections.unmodifiableList(new ArrayList<OllamaModelInfo>(models));
    }

    public List<OllamaModelInfo> getModels() {
        return models;
    }
}

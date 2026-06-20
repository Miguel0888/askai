package com.aresstack.askai.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Running-model list returned by Ollama /api/ps, mapped into AskAI domain models.
 */
public final class OllamaRunningModelListResponse {

    private final List<OllamaRunningModelInfo> models;

    public OllamaRunningModelListResponse(List<OllamaRunningModelInfo> models) {
        this.models = Collections.unmodifiableList(new ArrayList<OllamaRunningModelInfo>(models));
    }

    public List<OllamaRunningModelInfo> getModels() {
        return models;
    }
}

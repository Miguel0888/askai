package com.aresstack.askai.client;

import com.aresstack.askai.util.JsonSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response returned by Ollama /api/ps.
 */
public final class OllamaRunningModelListResponse {

    private final List<OllamaRunningModelInfo> models;

    public OllamaRunningModelListResponse(List<OllamaRunningModelInfo> models) {
        this.models = Collections.unmodifiableList(new ArrayList<OllamaRunningModelInfo>(models));
    }

    public static OllamaRunningModelListResponse fromJson(String json) {
        ArrayList<OllamaRunningModelInfo> models = new ArrayList<OllamaRunningModelInfo>();
        for (String modelJson : JsonSupport.extractArrayObjects(json, "models")) {
            models.add(OllamaRunningModelInfo.fromJson(modelJson));
        }
        return new OllamaRunningModelListResponse(models);
    }

    public List<OllamaRunningModelInfo> getModels() {
        return models;
    }
}

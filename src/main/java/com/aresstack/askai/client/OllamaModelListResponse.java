package com.aresstack.askai.client;

import com.aresstack.askai.util.JsonSupport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Response returned by Ollama /api/tags.
 */
public final class OllamaModelListResponse {

    private final List<OllamaModelInfo> models;

    public OllamaModelListResponse(List<OllamaModelInfo> models) {
        this.models = Collections.unmodifiableList(new ArrayList<OllamaModelInfo>(models));
    }

    public static OllamaModelListResponse fromJson(String json) {
        ArrayList<OllamaModelInfo> models = new ArrayList<OllamaModelInfo>();
        for (String modelJson : JsonSupport.extractArrayObjects(json, "models")) {
            models.add(OllamaModelInfo.fromJson(modelJson));
        }
        return new OllamaModelListResponse(models);
    }

    public List<OllamaModelInfo> getModels() {
        return models;
    }
}

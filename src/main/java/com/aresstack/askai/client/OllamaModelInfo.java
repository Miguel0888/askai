package com.aresstack.askai.client;

import com.aresstack.askai.util.JsonSupport;

/**
 * Installed Ollama model metadata.
 */
public final class OllamaModelInfo {

    private final String name;
    private final String model;
    private final String modifiedAt;
    private final long size;
    private final String digest;
    private final OllamaModelDetails details;

    public OllamaModelInfo(String name, String model, String modifiedAt, long size, String digest,
                           OllamaModelDetails details) {
        this.name = safe(name);
        this.model = safe(model);
        this.modifiedAt = safe(modifiedAt);
        this.size = size;
        this.digest = safe(digest);
        this.details = details == null ? OllamaModelDetails.empty() : details;
    }

    public static OllamaModelInfo fromJson(String json) {
        String detailsJson = JsonSupport.extractObjectValue(json, "details");
        return new OllamaModelInfo(
                JsonSupport.extractFirstStringValue(json, "name"),
                JsonSupport.extractFirstStringValue(json, "model"),
                JsonSupport.extractFirstStringValue(json, "modified_at"),
                JsonSupport.extractFirstLongValue(json, "size"),
                JsonSupport.extractFirstStringValue(json, "digest"),
                OllamaModelDetails.fromJson(detailsJson));
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public String getDisplayName() {
        return !name.isEmpty() ? name : model;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public long getSize() {
        return size;
    }

    public String getDigest() {
        return digest;
    }

    public OllamaModelDetails getDetails() {
        return details;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

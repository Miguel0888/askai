package com.aresstack.askai.client;

import com.aresstack.askai.util.JsonSupport;

/**
 * Active Ollama model metadata.
 */
public final class OllamaRunningModelInfo {

    private final String name;
    private final String model;
    private final String expiresAt;
    private final long size;
    private final long sizeVram;
    private final OllamaModelDetails details;

    public OllamaRunningModelInfo(String name, String model, String expiresAt, long size, long sizeVram,
                                  OllamaModelDetails details) {
        this.name = safe(name);
        this.model = safe(model);
        this.expiresAt = safe(expiresAt);
        this.size = size;
        this.sizeVram = sizeVram;
        this.details = details == null ? OllamaModelDetails.empty() : details;
    }

    public static OllamaRunningModelInfo fromJson(String json) {
        String detailsJson = JsonSupport.extractObjectValue(json, "details");
        return new OllamaRunningModelInfo(
                JsonSupport.extractFirstStringValue(json, "name"),
                JsonSupport.extractFirstStringValue(json, "model"),
                JsonSupport.extractFirstStringValue(json, "expires_at"),
                JsonSupport.extractFirstLongValue(json, "size"),
                JsonSupport.extractFirstLongValue(json, "size_vram"),
                OllamaModelDetails.fromJson(detailsJson));
    }

    public String getDisplayName() {
        return !name.isEmpty() ? name : model;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public long getSize() {
        return size;
    }

    public long getSizeVram() {
        return sizeVram;
    }

    public OllamaModelDetails getDetails() {
        return details;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

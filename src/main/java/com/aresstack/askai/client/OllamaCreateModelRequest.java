package com.aresstack.askai.client;

import com.aresstack.askai.catalog.OllamaModelImportProfile;
import com.aresstack.askai.util.JsonSupport;

import java.util.Map;

/**
 * Builds the Ollama /api/create request used for one imported model.
 */
public final class OllamaCreateModelRequest {

    private final String modelName;
    private final Map<String, String> digestByRelativePath;
    private final String quantization;
    private final OllamaModelImportProfile importProfile;

    public OllamaCreateModelRequest(String modelName, Map<String, String> digestByRelativePath,
                                    String quantization, OllamaModelImportProfile importProfile) {
        this.modelName = modelName;
        this.digestByRelativePath = digestByRelativePath;
        this.quantization = quantization == null ? "" : quantization.trim();
        this.importProfile = importProfile == null ? OllamaModelImportProfile.plain() : importProfile;
    }

    public String toJson() {
        return appendJson(new StringBuilder(), false).toString();
    }

    public String toPrettyJson() {
        return appendJson(new StringBuilder(), true).append('\n').toString();
    }

    private StringBuilder appendJson(StringBuilder builder, boolean pretty) {
        String indent = pretty ? "  " : "";
        String nestedIndent = pretty ? "    " : "";
        String newline = pretty ? "\n" : "";
        String separator = pretty ? ": " : ":";
        String comma = pretty ? ",\n" : ",";

        builder.append('{').append(newline);
        appendField(builder, indent, separator, comma, "model", JsonSupport.quote(modelName));
        appendField(builder, indent, separator, comma, "stream", "false");
        appendFiles(builder, indent, nestedIndent, newline, separator, comma);
        appendImportProfile(builder, indent, newline, separator, comma);
        appendQuantization(builder, indent, newline, separator);
        builder.append(newline).append('}');
        return builder;
    }

    private void appendField(StringBuilder builder, String indent, String separator, String comma,
                             String name, String valueJson) {
        builder.append(indent).append(JsonSupport.quote(name)).append(separator).append(valueJson).append(comma);
    }

    private void appendFiles(StringBuilder builder, String indent, String nestedIndent, String newline,
                             String separator, String comma) {
        builder.append(indent).append(JsonSupport.quote("files")).append(separator).append('{').append(newline);
        int index = 0;
        for (Map.Entry<String, String> entry : digestByRelativePath.entrySet()) {
            index++;
            builder.append(nestedIndent)
                    .append(JsonSupport.quote(entry.getKey()))
                    .append(separator)
                    .append(JsonSupport.quote("sha256:" + entry.getValue()));
            if (index < digestByRelativePath.size()) {
                builder.append(comma);
            }
        }
        builder.append(newline).append(indent).append('}');
    }

    private void appendImportProfile(StringBuilder builder, String indent, String newline, String separator, String comma) {
        if (importProfile.hasTemplate()) {
            builder.append(comma);
            builder.append(indent).append(JsonSupport.quote("template")).append(separator)
                    .append(JsonSupport.quote(importProfile.getTemplate()));
        }
        if (importProfile.hasSystemPrompt()) {
            builder.append(comma);
            builder.append(indent).append(JsonSupport.quote("system")).append(separator)
                    .append(JsonSupport.quote(importProfile.getSystemPrompt()));
        }
        if (importProfile.hasParametersJson()) {
            builder.append(comma);
            builder.append(indent).append(JsonSupport.quote("parameters")).append(separator)
                    .append(importProfile.getParametersJson());
        }
    }

    private void appendQuantization(StringBuilder builder, String indent, String newline, String separator) {
        if (quantization == null || quantization.isEmpty() || "none".equalsIgnoreCase(quantization)) {
            return;
        }
        builder.append(',').append(newline).append(indent)
                .append(JsonSupport.quote("quantize"))
                .append(separator)
                .append(JsonSupport.quote(quantization));
    }

    public OllamaModelImportProfile getImportProfile() {
        return importProfile;
    }
}

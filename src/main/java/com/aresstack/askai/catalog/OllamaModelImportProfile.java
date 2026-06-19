package com.aresstack.askai.catalog;

/**
 * Describes Ollama-side model behavior baked into the imported model entry.
 */
public final class OllamaModelImportProfile {

    private final String displayName;
    private final String template;
    private final String systemPrompt;
    private final String parametersJson;

    private OllamaModelImportProfile(String displayName, String template, String systemPrompt, String parametersJson) {
        this.displayName = displayName;
        this.template = template;
        this.systemPrompt = systemPrompt;
        this.parametersJson = parametersJson;
    }

    public static OllamaModelImportProfile plain() {
        return new OllamaModelImportProfile("Default Ollama auto-detection", "", "", "");
    }

    public static OllamaModelImportProfile qwenChatMl() {
        return new OllamaModelImportProfile(
                "Qwen ChatML",
                "{{ if .System }}<|im_start|>system\n"
                        + "{{ .System }}<|im_end|>\n"
                        + "{{ end }}{{ if .Prompt }}<|im_start|>user\n"
                        + "{{ .Prompt }}<|im_end|>\n"
                        + "{{ end }}<|im_start|>assistant\n"
                        + "{{ .Response }}<|im_end|>",
                "You are a concise local assistant. Answer the user's question directly.",
                "{\"temperature\":0.2,\"top_p\":0.8,\"repeat_penalty\":1.05,\"num_predict\":256,"
                        + "\"stop\":[\"<|im_start|>\",\"<|im_end|>\"]}");
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getTemplate() {
        return template;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String getParametersJson() {
        return parametersJson;
    }

    public boolean hasTemplate() {
        return template != null && !template.trim().isEmpty();
    }

    public boolean hasSystemPrompt() {
        return systemPrompt != null && !systemPrompt.trim().isEmpty();
    }

    public boolean hasParametersJson() {
        return parametersJson != null && !parametersJson.trim().isEmpty();
    }
}

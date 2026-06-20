package com.aresstack.askai.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Click-dummy implementation for future ollama4j-backed capabilities.
 */
final class DummyFeatureActionService implements FeatureActionService {

    private final List<FeatureAction> actions = Collections.unmodifiableList(Arrays.asList(
            new FeatureAction("pull-model", "Pull model", "Download a model from the Ollama library."),
            new FeatureAction("create-model", "Create model", "Create a model from a Modelfile or uploaded local files."),
            new FeatureAction("model-details", "Model details", "Show detailed metadata, parameters and modelfile content."),
            new FeatureAction("server-health", "Server health", "Ping Ollama, show version, latency and connection state."),
            new FeatureAction("vision-prompt", "Vision prompt", "Send an image and a prompt to a multimodal model."),
            new FeatureAction("tool-calling", "Tool calling", "Expose typed Java tools to compatible local models."),
            new FeatureAction("mcp-tools", "MCP tools", "Connect future MCP tool sets to local model actions.")
    ));

    @Override
    public List<FeatureAction> actions() {
        return actions;
    }

    @Override
    public void execute(String actionId, FeatureActionListener listener) {
        FeatureAction action = find(actionId);
        String title = action == null ? "Future action" : action.getTitle();
        String message = title + " is a UI placeholder. The service/API implementation will be added later.";
        listener.onAccepted(title, message);
    }

    private FeatureAction find(String actionId) {
        for (FeatureAction action : actions) {
            if (action.getId().equals(actionId)) {
                return action;
            }
        }
        return null;
    }
}

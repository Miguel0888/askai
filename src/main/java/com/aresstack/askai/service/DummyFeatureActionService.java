package com.aresstack.askai.service;

/**
 * Click-dummy implementation for future ollama4j-backed capabilities.
 */
final class DummyFeatureActionService implements FeatureActionService {

    @Override
    public void execute(String actionId, FeatureActionListener listener) {
        String title = titleFor(actionId);
        String message = title + " is a UI placeholder. The service/API implementation will be added later.";
        listener.onAccepted(title, message);
    }

    private static String titleFor(String actionId) {
        if ("pull-model".equals(actionId)) {
            return "Pull model";
        }
        if ("create-model".equals(actionId)) {
            return "Create model";
        }
        if ("model-details".equals(actionId)) {
            return "Model details";
        }
        if ("vision-prompt".equals(actionId)) {
            return "Vision prompt";
        }
        if ("tool-calling".equals(actionId)) {
            return "Tool calling";
        }
        if ("mcp-tools".equals(actionId)) {
            return "MCP tools";
        }
        if ("server-health".equals(actionId)) {
            return "Server health";
        }
        return "Future action";
    }
}

package com.aresstack.askai.service;

import com.aresstack.askai.client.OllamaModelInfo;
import com.aresstack.askai.client.OllamaRunningModelInfo;

import java.util.List;

/**
 * UI-facing Ollama service boundary. Swing panels depend on this interface,
 * not on HTTP clients, JSON, or a concrete Ollama library.
 */
public interface OllamaService {

    Task listModelNames(ModelNamesListener listener);

    Task listInstalledModels(InstalledModelsListener listener);

    Task listRunningModels(RunningModelsListener listener);

    Task getServerVersion(ServerVersionListener listener);

    Task deleteModel(String modelName, ActionListener listener);

    Task streamChat(ChatRequest request, ChatListener listener);

    interface Task {
        void cancel();
    }

    interface ModelNamesListener extends FailureListener {
        void onModelNames(List<String> names);
    }

    interface InstalledModelsListener extends FailureListener {
        void onInstalledModels(List<OllamaModelInfo> models);
    }

    interface RunningModelsListener extends FailureListener {
        void onRunningModels(List<OllamaRunningModelInfo> models);
    }

    interface ServerVersionListener extends FailureListener {
        void onServerVersion(String version);
    }

    interface ActionListener extends FailureListener {
        void onComplete(String message);
    }

    interface ChatListener extends FailureListener {
        void onContent(String content);

        void onStatus(String status);

        void onComplete(ChatResult result);
    }

    interface FailureListener {
        void onError(Exception ex);
    }

    final class ChatRequest {
        private final String modelName;
        private final String systemPrompt;
        private final String userPrompt;
        private final String keepAlive;

        public ChatRequest(String modelName, String systemPrompt, String userPrompt, String keepAlive) {
            this.modelName = modelName;
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            this.keepAlive = keepAlive;
        }

        public String getModelName() {
            return modelName;
        }

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public String getUserPrompt() {
            return userPrompt;
        }

        public String getKeepAlive() {
            return keepAlive;
        }
    }

    final class ChatResult {
        private final String fallbackText;
        private final long evalCount;
        private final long evalDurationNanos;

        public ChatResult(String fallbackText, long evalCount, long evalDurationNanos) {
            this.fallbackText = fallbackText == null ? "" : fallbackText;
            this.evalCount = evalCount;
            this.evalDurationNanos = evalDurationNanos;
        }

        public String getFallbackText() {
            return fallbackText;
        }

        public long getEvalCount() {
            return evalCount;
        }

        public long getEvalDurationNanos() {
            return evalDurationNanos;
        }

        public boolean hasMetrics() {
            return evalCount > 0L && evalDurationNanos > 0L;
        }
    }
}

package com.aresstack.askai.service;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.client.AskAiOllamaClient;
import com.aresstack.askai.client.OllamaModelDetails;
import com.aresstack.askai.client.OllamaModelInfo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real {@link FeatureActionService} backed by the {@code ollama4j} adapter.
 *
 * <p>Actions that need no extra user input run genuine ollama4j calls off the EDT and
 * report back through the listener: {@code server-health} (reachability + version +
 * round-trip) and {@code model-details} (installed models with their metadata). Actions
 * that require input (pull/create a specific model) or that AskAI keeps out of scope
 * (vision, tools, MCP) report an honest capability status rather than a fake placeholder.</p>
 */
public final class OllamaFeatureActionService implements FeatureActionService {

    private final AskAiModel model;
    private final ExecutorService executor;
    private final List<FeatureAction> actions = Collections.unmodifiableList(Arrays.asList(
            new FeatureAction("pull-model", "Pull model", "Download a model from the Ollama library."),
            new FeatureAction("create-model", "Create model", "Create a model from a Modelfile or uploaded local files."),
            new FeatureAction("model-details", "Model details", "Show detailed metadata, parameters and modelfile content."),
            new FeatureAction("server-health", "Server health", "Ping Ollama, show version, latency and connection state."),
            new FeatureAction("vision-prompt", "Vision prompt", "Send an image and a prompt to a multimodal model."),
            new FeatureAction("tool-calling", "Tool calling", "Expose typed Java tools to compatible local models."),
            new FeatureAction("mcp-tools", "MCP tools", "Connect future MCP tool sets to local model actions.")
    ));

    public OllamaFeatureActionService(AskAiModel model) {
        this.model = model;
        this.executor = Executors.newCachedThreadPool(new DaemonThreadFactory());
    }

    @Override
    public List<FeatureAction> actions() {
        return actions;
    }

    @Override
    public void execute(final String actionId, final FeatureActionListener listener) {
        final FeatureAction action = find(actionId);
        final String title = action == null ? "Future action" : action.getTitle();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    message = runAction(actionId);
                } catch (Exception ex) {
                    message = "Failed: " + ex.getMessage();
                }
                listener.onAccepted(title, message);
            }
        });
    }

    private String runAction(String actionId) throws Exception {
        if ("server-health".equals(actionId)) {
            return serverHealth();
        }
        if ("model-details".equals(actionId)) {
            return modelDetails();
        }
        if ("pull-model".equals(actionId)) {
            return "ollama4j can pull from the Ollama library, but a target model name is required. "
                    + "Use the Install tab to pull or import a specific model.";
        }
        if ("create-model".equals(actionId)) {
            return "Creating a model runs through the Install tab: it uploads local files as blobs and "
                    + "calls Ollama /api/create via the import client.";
        }
        if ("vision-prompt".equals(actionId) || "tool-calling".equals(actionId) || "mcp-tools".equals(actionId)) {
            return "Not wired in AskAI yet. This capability is intentionally out of scope for the current "
                    + "lean Ollama client; the ollama4j adapter can be extended later.";
        }
        return "Unknown action.";
    }

    private String serverHealth() {
        String baseUrl = model.getOllamaBaseUrl();
        AskAiOllamaClient client = new AskAiOllamaClient(baseUrl);
        long startNanos = System.nanoTime();
        try {
            String version = client.getVersion();
            long roundTripMillis = (System.nanoTime() - startNanos) / 1_000_000L;
            return "Ollama at " + baseUrl + " is reachable. Version " + version
                    + ". Round-trip " + roundTripMillis + " ms.";
        } catch (Exception ex) {
            return "Ollama at " + baseUrl + " is not reachable: " + ex.getMessage();
        }
    }

    private String modelDetails() throws Exception {
        String baseUrl = model.getOllamaBaseUrl();
        List<OllamaModelInfo> models = new AskAiOllamaClient(baseUrl).getInstalledModels().getModels();
        if (models.isEmpty()) {
            return "No models installed on " + baseUrl + ".";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(models.size()).append(" model(s) on ").append(baseUrl).append(':');
        for (OllamaModelInfo info : models) {
            OllamaModelDetails details = info.getDetails();
            builder.append("\n  - ").append(info.getDisplayName())
                    .append(" [").append(summarize(details)).append(", ").append(humanBytes(info.getSize())).append(']');
        }
        return builder.toString();
    }

    private static String summarize(OllamaModelDetails details) {
        StringBuilder builder = new StringBuilder();
        appendPart(builder, details.getFamily());
        appendPart(builder, details.getParameterSize());
        appendPart(builder, details.getQuantizationLevel());
        return builder.length() == 0 ? "unknown" : builder.toString();
    }

    private static void appendPart(StringBuilder builder, String part) {
        if (part != null && !part.isEmpty()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part);
        }
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0d;
        if (kb < 1024.0d) {
            return String.format("%.1f KB", kb);
        }
        double mb = kb / 1024.0d;
        if (mb < 1024.0d) {
            return String.format("%.1f MB", mb);
        }
        return String.format("%.2f GB", mb / 1024.0d);
    }

    private FeatureAction find(String actionId) {
        for (FeatureAction action : actions) {
            if (action.getId().equals(actionId)) {
                return action;
            }
        }
        return null;
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "askai-feature-action-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}

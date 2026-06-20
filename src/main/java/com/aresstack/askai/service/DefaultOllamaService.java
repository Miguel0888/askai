package com.aresstack.askai.service;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.client.AskAiOllamaClient;
import com.aresstack.askai.client.OllamaChatCompletion;
import com.aresstack.askai.client.OllamaChatStreamListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default service implementation. It hides the ollama4j adapter behind
 * the service interface so Swing panels never depend on REST/DTO details.
 */
public final class DefaultOllamaService implements OllamaService {

    private final AskAiModel model;
    private final ExecutorService executor;

    public DefaultOllamaService(AskAiModel model) {
        this.model = model;
        this.executor = Executors.newCachedThreadPool(new DaemonThreadFactory());
    }

    @Override
    public Task listModelNames(final ModelNamesListener listener) {
        return submit(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.onModelNames(client().getModelNames());
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }
        });
    }

    @Override
    public Task listInstalledModels(final InstalledModelsListener listener) {
        return submit(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.onInstalledModels(client().getInstalledModels().getModels());
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }
        });
    }

    @Override
    public Task listRunningModels(final RunningModelsListener listener) {
        return submit(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.onRunningModels(client().getRunningModels().getModels());
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }
        });
    }

    @Override
    public Task getServerVersion(final ServerVersionListener listener) {
        return submit(new Runnable() {
            @Override
            public void run() {
                try {
                    listener.onServerVersion(client().getVersion());
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }
        });
    }

    @Override
    public Task deleteModel(final String modelName, final ActionListener listener) {
        return submit(new Runnable() {
            @Override
            public void run() {
                try {
                    client().deleteModel(modelName);
                    listener.onComplete("Deleted " + modelName + ".");
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }
        });
    }

    @Override
    public Task streamChat(final ChatRequest request, final ChatListener listener) {
        return submit(new Runnable() {
            @Override
            public void run() {
                try {
                    client().streamChat(request.getModelName(), request.getSystemPrompt(),
                            request.getUserPrompt(), request.getKeepAlive(), new OllamaChatStreamListener() {
                                @Override
                                public void onContent(String content) {
                                    listener.onContent(content);
                                }

                                @Override
                                public void onStatus(String status) {
                                    listener.onStatus(status);
                                }

                                @Override
                                public void onComplete(OllamaChatCompletion completion) {
                                    listener.onComplete(toChatResult(completion));
                                }
                            });
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }
        });
    }

    private AskAiOllamaClient client() {
        return new AskAiOllamaClient(model.getOllamaBaseUrl());
    }

    private Task submit(Runnable runnable) {
        return new FutureTaskAdapter(executor.submit(runnable));
    }

    private static ChatResult toChatResult(OllamaChatCompletion completion) {
        if (completion == null) {
            return new ChatResult("", 0L, 0L);
        }
        return new ChatResult(completion.getContent(), completion.getEvalCount(), completion.getEvalDurationNanos());
    }

    private static final class FutureTaskAdapter implements Task {
        private final Future<?> future;

        private FutureTaskAdapter(Future<?> future) {
            this.future = future;
        }

        @Override
        public void cancel() {
            future.cancel(true);
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "askai-ollama-service-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}

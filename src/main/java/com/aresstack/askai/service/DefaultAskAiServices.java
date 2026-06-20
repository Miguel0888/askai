package com.aresstack.askai.service;

import com.aresstack.askai.AskAiModel;

/**
 * Default service registry. Opus can replace these implementations without touching UI panels.
 */
public final class DefaultAskAiServices implements AskAiServices {

    private final OllamaService ollamaService;
    private final ModelInstallService modelInstallService;
    private final FeatureActionService featureActionService;

    public DefaultAskAiServices(AskAiModel model) {
        this.ollamaService = new DefaultOllamaService(model);
        this.modelInstallService = new DefaultModelInstallService(model);
        this.featureActionService = new DummyFeatureActionService();
    }

    @Override
    public OllamaService ollama() {
        return ollamaService;
    }

    @Override
    public ModelInstallService modelInstall() {
        return modelInstallService;
    }

    @Override
    public FeatureActionService featureActions() {
        return featureActionService;
    }
}

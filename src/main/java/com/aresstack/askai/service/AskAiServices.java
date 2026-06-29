package com.aresstack.askai.service;

/**
 * Service registry injected into UI panels.
 */
public interface AskAiServices {

    OllamaService ollama();

    ModelInstallService modelInstall();

    ModelDownloadService modelDownload();

    FeatureActionService featureActions();
}

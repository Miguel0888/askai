package com.aresstack.askai.importing;

/**
 * Receives status updates from a model import use case.
 */
public interface OllamaImportListener {

    void onMessage(String message);

    void onProgress(int percent, String text);
}

package com.aresstack.askai.service;

/**
 * Boundary for future Ollama capabilities. Current implementation is a click dummy
 * so the UI can be completed without binding API behavior into Swing classes.
 */
public interface FeatureActionService {

    void execute(String actionId, FeatureActionListener listener);

    interface FeatureActionListener {
        void onAccepted(String title, String message);
    }
}

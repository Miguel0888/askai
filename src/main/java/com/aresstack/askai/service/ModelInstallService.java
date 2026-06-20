package com.aresstack.askai.service;

import com.aresstack.askai.importing.OllamaImportListener;
import com.aresstack.askai.importing.OllamaImportPlan;

/**
 * Boundary for installing local model files into Ollama.
 */
public interface ModelInstallService {

    String install(OllamaImportPlan plan, OllamaImportListener listener) throws Exception;
}

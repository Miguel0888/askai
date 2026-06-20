package com.aresstack.askai.service;

import com.aresstack.askai.AskAiModel;
import com.aresstack.askai.client.AskAiOllamaImportClient;
import com.aresstack.askai.importing.OllamaImportListener;
import com.aresstack.askai.importing.OllamaImportPlan;
import com.aresstack.askai.importing.OllamaImportUseCase;

/**
 * Default model-install implementation. Kept outside Swing panels so the import
 * implementation can be replaced independently of the UI.
 */
public final class DefaultModelInstallService implements ModelInstallService {

    private final AskAiModel model;

    public DefaultModelInstallService(AskAiModel model) {
        this.model = model;
    }

    @Override
    public String install(OllamaImportPlan plan, OllamaImportListener listener) throws Exception {
        OllamaImportUseCase useCase = new OllamaImportUseCase(new AskAiOllamaImportClient(model.getOllamaBaseUrl()));
        return useCase.execute(plan, listener);
    }
}

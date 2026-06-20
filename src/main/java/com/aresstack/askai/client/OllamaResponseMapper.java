package com.aresstack.askai.client;

import io.github.ollama4j.models.ps.ModelProcessesResult;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.ModelMeta;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Maps ollama4j response DTOs to the AskAI domain/UI models.
 *
 * <p>This is the single translation seam between the ollama4j library types and
 * the rest of AskAI. The UI never sees ollama4j classes or raw JSON.</p>
 */
final class OllamaResponseMapper {

    private OllamaResponseMapper() {
    }

    static OllamaModelListResponse toInstalledModels(List<Model> models) {
        ArrayList<OllamaModelInfo> mapped = new ArrayList<OllamaModelInfo>();
        if (models != null) {
            for (Model model : models) {
                if (model != null) {
                    mapped.add(toModelInfo(model));
                }
            }
        }
        return new OllamaModelListResponse(mapped);
    }

    static OllamaModelInfo toModelInfo(Model model) {
        return new OllamaModelInfo(
                model.getName(),
                model.getModel(),
                format(model.getModifiedAt()),
                model.getSize(),
                model.getDigest(),
                toDetails(model.getModelMeta()));
    }

    static OllamaRunningModelListResponse toRunningModels(ModelProcessesResult result) {
        ArrayList<OllamaRunningModelInfo> mapped = new ArrayList<OllamaRunningModelInfo>();
        if (result != null && result.getModels() != null) {
            for (ModelProcessesResult.ModelProcess process : result.getModels()) {
                if (process != null) {
                    mapped.add(toRunningModelInfo(process));
                }
            }
        }
        return new OllamaRunningModelListResponse(mapped);
    }

    static OllamaRunningModelInfo toRunningModelInfo(ModelProcessesResult.ModelProcess process) {
        return new OllamaRunningModelInfo(
                process.getName(),
                process.getModel(),
                process.getExpiresAt(),
                process.getSize(),
                process.getSizeVram(),
                toDetails(process.getDetails()));
    }

    static OllamaModelDetails toDetails(ModelMeta meta) {
        if (meta == null) {
            return OllamaModelDetails.empty();
        }
        return new OllamaModelDetails(meta.getFormat(), meta.getFamily(),
                meta.getParameterSize(), meta.getQuantizationLevel());
    }

    static OllamaModelDetails toDetails(ModelProcessesResult.ModelDetails details) {
        if (details == null) {
            return OllamaModelDetails.empty();
        }
        return new OllamaModelDetails(details.getFormat(), details.getFamily(),
                details.getParameterSize(), details.getQuantizationLevel());
    }

    private static String format(OffsetDateTime value) {
        return value == null ? "" : value.toString();
    }
}

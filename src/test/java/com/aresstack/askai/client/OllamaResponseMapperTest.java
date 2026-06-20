package com.aresstack.askai.client;

import io.github.ollama4j.models.ps.ModelProcessesResult;
import io.github.ollama4j.models.response.Model;
import io.github.ollama4j.models.response.ModelMeta;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that ollama4j response DTOs are mapped into AskAI domain models,
 * replacing the former manual JSON parsing of /api/tags and /api/ps.
 */
class OllamaResponseMapperTest {

    @Test
    void mapsInstalledModelsFromOllama4jModels() {
        Model qwen = newModel("qwen-local:latest", 123L, "gguf", "qwen2", "0.5B", "");
        Model gemma = newModel("gemma-local:latest", 456L, "gguf", "gemma3", "", "Q4_0");

        OllamaModelListResponse response = OllamaResponseMapper.toInstalledModels(List.of(qwen, gemma));

        assertEquals(2, response.getModels().size());
        OllamaModelInfo first = response.getModels().get(0);
        assertEquals("qwen-local:latest", first.getName());
        assertEquals(123L, first.getSize());
        assertEquals("qwen2", first.getDetails().getFamily());
        assertEquals("0.5B", first.getDetails().getParameterSize());
        OllamaModelInfo second = response.getModels().get(1);
        assertEquals("gemma-local:latest", second.getName());
        assertEquals("Q4_0", second.getDetails().getQuantizationLevel());
    }

    @Test
    void mapsRunningModelsFromProcessesResult() {
        ModelProcessesResult.ModelDetails details = new ModelProcessesResult.ModelDetails();
        details.setFamily("qwen2");
        details.setParameterSize("0.5B");
        details.setQuantizationLevel("Q4_0");

        ModelProcessesResult.ModelProcess process = new ModelProcessesResult.ModelProcess();
        process.setName("qwen-local:latest");
        process.setModel("qwen-local:latest");
        process.setExpiresAt("2026-06-20T05:00:00Z");
        process.setSize(2048L);
        process.setSizeVram(1024L);
        process.setDetails(details);

        ModelProcessesResult result = new ModelProcessesResult();
        result.setModels(List.of(process));

        OllamaRunningModelListResponse mapped = OllamaResponseMapper.toRunningModels(result);

        assertEquals(1, mapped.getModels().size());
        OllamaRunningModelInfo info = mapped.getModels().get(0);
        assertEquals("qwen-local:latest", info.getDisplayName());
        assertEquals(2048L, info.getSize());
        assertEquals(1024L, info.getSizeVram());
        assertEquals("qwen2", info.getDetails().getFamily());
        assertEquals("0.5B", info.getDetails().getParameterSize());
    }

    @Test
    void emptyResultsMapToEmptyLists() {
        assertTrue(OllamaResponseMapper.toInstalledModels(null).getModels().isEmpty());
        assertTrue(OllamaResponseMapper.toRunningModels(null).getModels().isEmpty());
        assertTrue(OllamaResponseMapper.toRunningModels(new ModelProcessesResult()).getModels().isEmpty());
    }

    private static Model newModel(String name, long size, String format, String family,
                                  String parameterSize, String quantizationLevel) {
        ModelMeta meta = new ModelMeta();
        meta.setFormat(format);
        meta.setFamily(family);
        meta.setParameterSize(parameterSize);
        meta.setQuantizationLevel(quantizationLevel);

        Model model = new Model();
        model.setName(name);
        model.setModel(name);
        model.setSize(size);
        model.setDigest("abc");
        model.setModifiedAt(OffsetDateTime.of(2026, 6, 20, 2, 0, 0, 0, ZoneOffset.UTC));
        model.setModelMeta(meta);
        return model;
    }
}

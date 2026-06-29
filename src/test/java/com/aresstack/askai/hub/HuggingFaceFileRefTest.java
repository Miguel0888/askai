package com.aresstack.askai.hub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that AskAI catalog file URLs are parsed into the repo/revision/path inputs
 * huggingface4j expects, including multi-segment repo ids and nested paths.
 */
class HuggingFaceFileRefTest {

    @Test
    void parsesRootFileUrl() {
        HuggingFaceFileRef ref = HuggingFaceFileRef.parse(
                "https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct/resolve/main/model.safetensors");

        assertEquals("https://huggingface.co", ref.getEndpoint());
        assertEquals("Qwen/Qwen2.5-Coder-0.5B-Instruct", ref.getRepoId());
        assertEquals("main", ref.getRevision());
        assertEquals("model.safetensors", ref.getPath());
    }

    @Test
    void parsesNestedSubdirectoryPath() {
        HuggingFaceFileRef ref = HuggingFaceFileRef.parse(
                "https://huggingface.co/onnx-community/Qwen2.5-Coder-0.5B-Instruct/resolve/main/onnx/model_q4f16.onnx");

        assertEquals("onnx-community/Qwen2.5-Coder-0.5B-Instruct", ref.getRepoId());
        assertEquals("main", ref.getRevision());
        assertEquals("onnx/model_q4f16.onnx", ref.getPath());
    }

    @Test
    void parsesNonDefaultRevisionAndEndpoint() {
        HuggingFaceFileRef ref = HuggingFaceFileRef.parse(
                "https://hf.mirror.example/google/gemma-3-270m-it/resolve/v1.0/config.json");

        assertEquals("https://hf.mirror.example", ref.getEndpoint());
        assertEquals("google/gemma-3-270m-it", ref.getRepoId());
        assertEquals("v1.0", ref.getRevision());
        assertEquals("config.json", ref.getPath());
    }

    @Test
    void returnsNullForNonResolveUrls() {
        assertNull(HuggingFaceFileRef.parse(null));
        assertNull(HuggingFaceFileRef.parse("https://huggingface.co/Qwen/Qwen2.5/tree/main"));
        assertNull(HuggingFaceFileRef.parse("not a url"));
        assertNull(HuggingFaceFileRef.parse("https://huggingface.co/resolve/main/x"));
    }
}

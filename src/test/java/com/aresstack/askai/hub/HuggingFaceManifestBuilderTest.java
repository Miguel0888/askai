package com.aresstack.askai.hub;

import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;
import com.aresstack.windirectml.workbench.download.ModelFileDescriptor;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the repo-file-listing → manifest logic that replaces the old static URL
 * catalogs: only relevant model files are kept, requiredness is classified sensibly,
 * and resolve URLs are generated internally.
 */
class HuggingFaceManifestBuilderTest {

    @Test
    void keepsModelRelevantFilesAndDropsTheRest() {
        assertTrue(HuggingFaceManifestBuilder.isRelevant("model.safetensors"));
        assertTrue(HuggingFaceManifestBuilder.isRelevant("model.safetensors.index.json"));
        assertTrue(HuggingFaceManifestBuilder.isRelevant("model-00001-of-00002.safetensors"));
        assertTrue(HuggingFaceManifestBuilder.isRelevant("Qwen2.5.gguf"));
        assertTrue(HuggingFaceManifestBuilder.isRelevant("config.json"));
        assertTrue(HuggingFaceManifestBuilder.isRelevant("tokenizer.json"));
        assertTrue(HuggingFaceManifestBuilder.isRelevant("special_tokens_map.json"));
        assertTrue(HuggingFaceManifestBuilder.isRelevant("chat_template.jinja"));

        assertFalse(HuggingFaceManifestBuilder.isRelevant("README.md"));
        assertFalse(HuggingFaceManifestBuilder.isRelevant("model.onnx"));
        assertFalse(HuggingFaceManifestBuilder.isRelevant(".gitattributes"));
        assertFalse(HuggingFaceManifestBuilder.isRelevant("images/logo.png"));
    }

    @Test
    void classifiesWeightsConfigAndCoreTokenizerAsRequired() {
        assertTrue(HuggingFaceManifestBuilder.isRequired("model.safetensors"));
        assertTrue(HuggingFaceManifestBuilder.isRequired("model.gguf"));
        assertTrue(HuggingFaceManifestBuilder.isRequired("config.json"));
        assertTrue(HuggingFaceManifestBuilder.isRequired("tokenizer.json"));
        assertTrue(HuggingFaceManifestBuilder.isRequired("tokenizer_config.json"));

        // Qwen rule: special_tokens_map.json stays optional so a missing file never aborts.
        assertFalse(HuggingFaceManifestBuilder.isRequired("special_tokens_map.json"));
        assertFalse(HuggingFaceManifestBuilder.isRequired("generation_config.json"));
        assertFalse(HuggingFaceManifestBuilder.isRequired("added_tokens.json"));
    }

    @Test
    void buildsManifestWithGeneratedResolveUrls() {
        List<HuggingFaceModelLoader.HuggingFaceModelFile> files = Arrays.asList(
                file("model.safetensors", 1000L),
                file("config.json", 10L),
                file("tokenizer.json", 20L),
                file("special_tokens_map.json", 5L),
                file("README.md", 1L));

        ModelDownloadManifest manifest = HuggingFaceManifestBuilder.build(
                "Qwen/Qwen2.5-Coder-0.5B-Instruct", "main", files);

        assertEquals("Qwen-Qwen2.5-Coder-0.5B-Instruct", manifest.modelId());
        assertEquals(4, manifest.files().size(), "README.md must be excluded");

        ModelFileDescriptor weights = find(manifest, "model.safetensors");
        assertNotNull(weights);
        assertTrue(weights.required());
        assertEquals("https://huggingface.co/Qwen/Qwen2.5-Coder-0.5B-Instruct/resolve/main/model.safetensors",
                weights.currentUrl());

        assertFalse(find(manifest, "special_tokens_map.json").required());
        assertNull(find(manifest, "README.md"));
    }

    @Test
    void sanitizesRepoIdIntoLocalDirectoryName() {
        assertEquals("google-gemma-3-270m-it",
                HuggingFaceManifestBuilder.safeLocalDirName("google/gemma-3-270m-it"));
        assertEquals("hf-model", HuggingFaceManifestBuilder.safeLocalDirName("   "));
    }

    private static HuggingFaceModelLoader.HuggingFaceModelFile file(String path, long size) {
        return new HuggingFaceModelLoader.HuggingFaceModelFile(path, size);
    }

    private static ModelFileDescriptor find(ModelDownloadManifest manifest, String localFilename) {
        for (ModelFileDescriptor descriptor : manifest.files()) {
            if (localFilename.equals(descriptor.localFilename())) {
                return descriptor;
            }
        }
        return null;
    }
}

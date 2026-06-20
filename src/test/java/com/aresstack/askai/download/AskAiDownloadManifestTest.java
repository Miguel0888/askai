package com.aresstack.askai.download;

import com.aresstack.windirectml.workbench.download.DownloadOverrideStore;
import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;
import com.aresstack.windirectml.workbench.download.ModelDownloadUrls;
import com.aresstack.windirectml.workbench.download.ModelFileDescriptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AskAiDownloadManifestTest {

    @Test
    void qwenSafeTensorsSpecialTokensMapIsOptionalOverrideFile() {
        ModelDownloadManifest manifest = ModelDownloadUrls.manifestForQwenSafeTensors();
        assertDescriptor(manifest, "special_tokens_map.json", false,
                hf("Qwen/Qwen2.5-Coder-0.5B-Instruct", "special_tokens_map.json"));
    }

    @Test
    void qwenSafeTensorsDefaultUrlsMatchExpectedHuggingFaceRepo() {
        ModelDownloadManifest manifest = ModelDownloadUrls.manifestForQwenSafeTensors();
        String repo = "Qwen/Qwen2.5-Coder-0.5B-Instruct";
        assertDescriptor(manifest, "model.safetensors", true, hf(repo, "model.safetensors"));
        assertDescriptor(manifest, "config.json", true, hf(repo, "config.json"));
        assertDescriptor(manifest, "tokenizer.json", true, hf(repo, "tokenizer.json"));
        assertDescriptor(manifest, "tokenizer_config.json", true, hf(repo, "tokenizer_config.json"));
        assertDescriptor(manifest, "generation_config.json", false, hf(repo, "generation_config.json"));
        assertDescriptor(manifest, "merges.txt", false, hf(repo, "merges.txt"));
        assertDescriptor(manifest, "vocab.json", false, hf(repo, "vocab.json"));
    }

    @Test
    void qwenUrlOverridesAreAppliedBeforeDownload(@TempDir Path tempDir) throws IOException {
        Path storeFile = tempDir.resolve("download-overrides.json");
        DownloadOverrideStore store = new DownloadOverrideStore(storeFile);
        ModelDownloadManifest manifest = ModelDownloadUrls.manifestForQwenSafeTensors();
        String customUrl = "https://example.invalid/qwen/model.safetensors";

        store.storeOverrides(manifest.withFileUrl(0, customUrl));

        DownloadOverrideStore reloadedStore = new DownloadOverrideStore(storeFile);
        reloadedStore.load();
        ModelDownloadManifest reloadedManifest = reloadedStore.applyOverrides(manifest);
        assertEquals(customUrl, reloadedManifest.files().get(0).currentUrl());
        assertEquals(manifest.files().get(0).defaultUrl(), reloadedManifest.files().get(0).defaultUrl());
    }

    private static String hf(String repo, String file) {
        return "https://huggingface.co/" + repo + "/resolve/main/" + file;
    }

    private static void assertDescriptor(ModelDownloadManifest manifest, String localFilename,
                                         boolean required, String expectedUrl) {
        ModelFileDescriptor descriptor = manifest.files().stream()
                .filter(file -> file.localFilename().equals(localFilename))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing descriptor: " + localFilename));
        assertEquals(required, descriptor.required(), localFilename + " required flag");
        assertEquals(expectedUrl, descriptor.defaultUrl(), localFilename + " default URL");
        assertEquals(expectedUrl, descriptor.currentUrl(), localFilename + " current URL");
    }
}

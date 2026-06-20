package com.aresstack.askai.download;

import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;
import com.aresstack.windirectml.workbench.download.ModelDownloadUrls;
import com.aresstack.windirectml.workbench.download.ModelFileDescriptor;
import com.aresstack.windirectml.workbench.download.QwenModelDownloadConfig;
import com.aresstack.windirectml.workbench.download.QwenOnnxModelVariant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * For Qwen, {@code tokenizer.json} and {@code tokenizer_config.json} are required while
 * {@code special_tokens_map.json} is an optional override. A missing optional file must
 * not be treated as required (which would abort the install on a 404).
 */
class QwenSpecialTokensOptionalTest {

    @Test
    void safeTensorsManifestRequiresTokenizerAndMakesSpecialTokensOptional() {
        ModelDownloadManifest manifest = ModelDownloadUrls.manifestForQwenSafeTensors();

        assertTrue(isRequired(manifest, "tokenizer.json"),
                "tokenizer.json must be required");
        assertTrue(isRequired(manifest, "tokenizer_config.json"),
                "tokenizer_config.json must be required");

        ModelFileDescriptor specialTokens = find(manifest, "special_tokens_map.json");
        assertNotNull(specialTokens, "special_tokens_map.json must still be offered for download");
        assertFalse(specialTokens.required(),
                "special_tokens_map.json must be optional, not required");
    }

    @Test
    void onnxConfigKeepsSpecialTokensOutOfRequiredRootFiles() {
        QwenModelDownloadConfig config = QwenModelDownloadConfig.forVariant(QwenOnnxModelVariant.Q4F16);

        assertTrue(config.rootFiles().contains("tokenizer.json"));
        assertTrue(config.rootFiles().contains("tokenizer_config.json"));
        assertFalse(config.rootFiles().contains("special_tokens_map.json"),
                "special_tokens_map.json must not be a required root file");
        assertTrue(config.optionalFiles().contains("special_tokens_map.json"),
                "special_tokens_map.json must be listed as optional");
    }

    private static boolean isRequired(ModelDownloadManifest manifest, String fileName) {
        ModelFileDescriptor descriptor = find(manifest, fileName);
        return descriptor != null && descriptor.required();
    }

    private static ModelFileDescriptor find(ModelDownloadManifest manifest, String fileName) {
        for (ModelFileDescriptor descriptor : manifest.files()) {
            if (fileName.equals(descriptor.localFilename())) {
                return descriptor;
            }
        }
        return null;
    }
}

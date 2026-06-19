package com.aresstack.askai.catalog;

import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;
import com.aresstack.windirectml.workbench.download.ModelDownloadUrls;
import com.aresstack.windirectml.workbench.download.ModelFileDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides model candidates for the AI provider import.
 */
public final class OllamaModelCatalog {

    private OllamaModelCatalog() {
    }

    public static List<OllamaModelCandidate> candidates() {
        ArrayList<OllamaModelCandidate> candidates = new ArrayList<OllamaModelCandidate>();
        candidates.add(new OllamaModelCandidate(
                "Qwen2.5 Coder 0.5B Instruct SafeTensors (target)",
                ModelDownloadUrls.manifestForQwenSafeTensors(),
                "qwen2.5-coder-0.5b:latest",
                "Small target model for the next product test. Uses an explicit Qwen ChatML template for Ollama chat behavior.",
                true, OllamaModelImportProfile.qwenChatMl()));
        candidates.add(new OllamaModelCandidate(
                "Gemma 3 270M IT SafeTensors (small fallback)",
                ModelDownloadUrls.manifestForGemma3_270MInstruct(),
                "gemma3-270m-it:latest",
                "Very small fallback for verifying download, upload, install and chat on weak CPU-only servers.",
                false, OllamaModelImportProfile.plain()));
        candidates.add(new OllamaModelCandidate(
                "Gemma 3 270M SafeTensors (base)",
                ModelDownloadUrls.manifestForGemma3_270M(),
                "gemma3-270m:latest",
                "Small base model candidate. Chat quality is lower than the instruct variant.",
                false, OllamaModelImportProfile.plain()));
        candidates.add(new OllamaModelCandidate(
                "Phi-3 Mini 4K Instruct SafeTensors (larger fallback)",
                phi3Mini4kSafeTensors(),
                "phi3-mini-4k:latest",
                "Larger fallback model. It is more useful than tiny models but slower on CPU-only machines.",
                false, OllamaModelImportProfile.plain()));
        return Collections.unmodifiableList(candidates);
    }

    private static ModelDownloadManifest phi3Mini4kSafeTensors() {
        String repo = "microsoft/Phi-3-mini-4k-instruct";
        String localDir = "phi-3-mini-4k-instruct-safetensors";
        ArrayList<ModelFileDescriptor> files = new ArrayList<ModelFileDescriptor>();
        addRoot(files, repo, "config.json", true);
        addRoot(files, repo, "generation_config.json", false);
        addRoot(files, repo, "model-00001-of-00002.safetensors", true);
        addRoot(files, repo, "model-00002-of-00002.safetensors", true);
        addRoot(files, repo, "model.safetensors.index.json", true);
        addRoot(files, repo, "special_tokens_map.json", true);
        addRoot(files, repo, "tokenizer.json", true);
        addRoot(files, repo, "tokenizer.model", true);
        addRoot(files, repo, "tokenizer_config.json", true);
        return new ModelDownloadManifest(repo, localDir, Collections.unmodifiableList(files));
    }

    private static void addRoot(List<ModelFileDescriptor> files, String repo, String file, boolean required) {
        String url = "https://huggingface.co/" + repo + "/resolve/main/" + file;
        files.add(new ModelFileDescriptor(file, required, url, url, file));
    }
}

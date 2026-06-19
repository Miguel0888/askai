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
                "Gemma 3 270M IT SafeTensors (small spike candidate)",
                ModelDownloadUrls.manifestForGemma3_270MInstruct(),
                "gemma3-270m-it-safetensors:latest",
                "Smallest existing Workbench SafeTensors candidate. Requires an Ollama version that supports Gemma 3 imports/runs.",
                true));
        candidates.add(new OllamaModelCandidate(
                "Gemma 3 270M SafeTensors (base)",
                ModelDownloadUrls.manifestForGemma3_270M(),
                "gemma3-270m-safetensors:latest",
                "Small base model candidate. Chat quality is lower than the instruct variant.",
                false));
        candidates.add(new OllamaModelCandidate(
                "Phi-3 Mini 4K Instruct SafeTensors (large fallback)",
                phi3Mini4kSafeTensors(),
                "phi3-mini-4k-safetensors:latest",
                "Officially closer to Ollama's documented Phi3 SafeTensors support, but much larger than Gemma 270M.",
                false));
        candidates.add(new OllamaModelCandidate(
                "Qwen2.5 Coder 0.5B SafeTensors (negative/experimental)",
                ModelDownloadUrls.manifestForQwenSafeTensors(),
                "qwen2.5-coder-0.5b-safetensors:latest",
                "Qwen is not listed in Ollama's direct SafeTensors import docs. Keep this for a negative spike or later GGUF route.",
                false));
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

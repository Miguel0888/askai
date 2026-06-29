package com.aresstack.askai.hub;

import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;
import com.aresstack.windirectml.workbench.download.ModelFileDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds an AskAI download manifest from a Hugging Face repository file listing.
 *
 * <p>This replaces the old static URL catalogs: instead of hand-maintained per-file
 * URLs, the relevant model files are discovered from the repo structure and only the
 * files needed to run/import a model are kept (weights, config, tokenizer assets,
 * chat template). Resolve URLs are generated internally so the rest of the pipeline
 * (which downloads via {@link HuggingFaceFileRef}) keeps working unchanged; the user
 * never sees or edits a URL.</p>
 */
public final class HuggingFaceManifestBuilder {

    private static final String DEFAULT_ENDPOINT = "https://huggingface.co";

    /** Exact file names (matched on the basename) that are relevant to a model. */
    private static final List<String> RELEVANT_NAMES = List.of(
            "config.json",
            "generation_config.json",
            "tokenizer.json",
            "tokenizer.model",
            "tokenizer_config.json",
            "special_tokens_map.json",
            "added_tokens.json",
            "merges.txt",
            "vocab.json",
            "chat_template.jinja");

    /** Path suffixes (globs) that are relevant to a model. */
    private static final List<String> RELEVANT_SUFFIXES = List.of(
            ".safetensors",
            ".safetensors.index.json",
            ".gguf");

    private HuggingFaceManifestBuilder() {
    }

    public static ModelDownloadManifest build(String repoId, String revision,
                                              List<HuggingFaceModelLoader.HuggingFaceModelFile> files) {
        String effectiveRevision = revision == null || revision.isBlank() ? "main" : revision.trim();
        String localDir = safeLocalDirName(repoId);
        List<ModelFileDescriptor> descriptors = new ArrayList<ModelFileDescriptor>();
        if (files != null) {
            for (HuggingFaceModelLoader.HuggingFaceModelFile file : files) {
                String path = file.getPath();
                if (isRelevant(path)) {
                    String url = resolveUrl(repoId, effectiveRevision, path);
                    descriptors.add(new ModelFileDescriptor(path, isRequired(path), url, url, path));
                }
            }
        }
        return new ModelDownloadManifest(localDir, localDir, List.copyOf(descriptors));
    }

    /** Whether a repo file is one AskAI should download to run/import the model. */
    public static boolean isRelevant(String path) {
        if (path == null || path.isEmpty() || path.endsWith("/")) {
            return false;
        }
        String lower = path.toLowerCase();
        for (String suffix : RELEVANT_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        String name = basename(lower);
        return RELEVANT_NAMES.contains(name);
    }

    /** Weights, base config and the core tokenizer are required; auxiliary assets are optional. */
    public static boolean isRequired(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".safetensors") || lower.endsWith(".gguf")) {
            return true;
        }
        String name = basename(lower);
        return name.equals("config.json") || name.equals("tokenizer.json") || name.equals("tokenizer_config.json");
    }

    public static String safeLocalDirName(String repoId) {
        if (repoId == null || repoId.isBlank()) {
            return "hf-model";
        }
        String cleaned = repoId.trim().replaceAll("[^A-Za-z0-9._-]+", "-");
        cleaned = cleaned.replaceAll("-+", "-");
        cleaned = trimChar(cleaned, '-');
        return cleaned.isEmpty() ? "hf-model" : cleaned;
    }

    private static String resolveUrl(String repoId, String revision, String path) {
        return DEFAULT_ENDPOINT + "/" + repoId + "/resolve/" + revision + "/" + path;
    }

    private static String basename(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static String trimChar(String value, char ch) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == ch) {
            start++;
        }
        while (end > start && value.charAt(end - 1) == ch) {
            end--;
        }
        return value.substring(start, end);
    }
}

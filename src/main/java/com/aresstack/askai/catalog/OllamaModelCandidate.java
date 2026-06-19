package com.aresstack.askai.catalog;

import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;

/**
 * Describes one selectable model import candidate for the spike UI.
 */
public final class OllamaModelCandidate {

    private final String displayName;
    private final ModelDownloadManifest manifest;
    private final String defaultOllamaModelName;
    private final String compatibilityNote;
    private final boolean recommendedForSpike;

    public OllamaModelCandidate(String displayName, ModelDownloadManifest manifest,
                                String defaultOllamaModelName, String compatibilityNote,
                                boolean recommendedForSpike) {
        this.displayName = displayName;
        this.manifest = manifest;
        this.defaultOllamaModelName = defaultOllamaModelName;
        this.compatibilityNote = compatibilityNote;
        this.recommendedForSpike = recommendedForSpike;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ModelDownloadManifest getManifest() {
        return manifest;
    }

    public String getDefaultOllamaModelName() {
        return defaultOllamaModelName;
    }

    public String getCompatibilityNote() {
        return compatibilityNote;
    }

    public boolean isRecommendedForSpike() {
        return recommendedForSpike;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

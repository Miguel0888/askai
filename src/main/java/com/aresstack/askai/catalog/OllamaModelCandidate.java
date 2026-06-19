package com.aresstack.askai.catalog;

import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;

/**
 * Describes one selectable model import candidate for the install UI.
 */
public final class OllamaModelCandidate {

    private final String displayName;
    private final ModelDownloadManifest manifest;
    private final String defaultOllamaModelName;
    private final String compatibilityNote;
    private final boolean recommendedForSpike;
    private final OllamaModelImportProfile importProfile;

    public OllamaModelCandidate(String displayName, ModelDownloadManifest manifest,
                                String defaultOllamaModelName, String compatibilityNote,
                                boolean recommendedForSpike, OllamaModelImportProfile importProfile) {
        this.displayName = displayName;
        this.manifest = manifest;
        this.defaultOllamaModelName = defaultOllamaModelName;
        this.compatibilityNote = compatibilityNote;
        this.recommendedForSpike = recommendedForSpike;
        this.importProfile = importProfile == null ? OllamaModelImportProfile.plain() : importProfile;
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

    public OllamaModelImportProfile getImportProfile() {
        return importProfile;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

package com.aresstack.askai.service;

import com.aresstack.askai.hub.HuggingFaceDownloadException;
import com.aresstack.askai.hub.HuggingFaceModelSearchResult;
import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;

import java.nio.file.Path;
import java.util.List;

/**
 * Boundary for searching and loading model files from the Hugging Face Hub. The default
 * implementation is backed by {@code huggingface4j}; Swing panels depend only on this
 * interface and never on URLs or repo file structure.
 */
public interface ModelDownloadService {

    /** Searches the Hub for model repositories matching the query. */
    List<HuggingFaceModelSearchResult> searchModels(String query, String token) throws HuggingFaceDownloadException;

    /** Builds a download manifest from the relevant files of a repo revision. */
    ModelDownloadManifest createManifest(String repoId, String revision, String token) throws HuggingFaceDownloadException;

    Task download(ModelDownloadManifest manifest, Path targetDir, boolean force, String token, DownloadListener listener);

    interface Task {
        void cancel();
    }

    interface DownloadListener {
        void onLog(String message);

        void onProgress(int percent, String text);

        void onComplete();

        void onError(Exception ex);
    }
}

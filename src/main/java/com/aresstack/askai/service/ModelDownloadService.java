package com.aresstack.askai.service;

import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;

import java.nio.file.Path;

/**
 * Boundary for loading model files from the Hugging Face Hub. The default
 * implementation is backed by {@code huggingface4j}; Swing panels depend only on
 * this interface.
 */
public interface ModelDownloadService {

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

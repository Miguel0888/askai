package com.aresstack.askai.service;

import com.aresstack.askai.hub.HuggingFaceDownloadException;
import com.aresstack.askai.hub.HuggingFaceFileRef;
import com.aresstack.askai.hub.HuggingFaceModelLoader;
import com.aresstack.windirectml.workbench.download.ModelDownloadManifest;
import com.aresstack.windirectml.workbench.download.ModelFileDescriptor;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Loads model files from the Hugging Face Hub via {@code huggingface4j}.
 *
 * <p>Iterates a catalog manifest, turning each file URL into a
 * {@link HuggingFaceFileRef} and downloading it through {@link HuggingFaceModelLoader}.
 * Required files abort the run on error; optional files (e.g. a missing
 * {@code special_tokens_map.json}) are skipped so the download is not aborted. Progress
 * is aggregated across files and reported off the EDT.</p>
 */
public final class DefaultModelDownloadService implements ModelDownloadService {

    private final ExecutorService executor = Executors.newCachedThreadPool(new DaemonThreadFactory());

    @Override
    public Task download(final ModelDownloadManifest manifest, final Path targetDir, final boolean force,
                         final String token, final DownloadListener listener) {
        Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runDownload(manifest, targetDir, force, token, listener);
                    if (!Thread.currentThread().isInterrupted()) {
                        listener.onComplete();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    listener.onLog("Download cancelled.");
                } catch (Exception ex) {
                    listener.onError(ex);
                }
            }
        });
        return new FutureTaskAdapter(future);
    }

    private void runDownload(ModelDownloadManifest manifest, Path targetDir, boolean force, String token,
                             DownloadListener listener) throws Exception {
        HuggingFaceModelLoader loader = new HuggingFaceModelLoader(token);
        List<ModelFileDescriptor> files = manifest.files();
        final int total = files.size();
        int index = 0;
        for (final ModelFileDescriptor descriptor : files) {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            final int completed = index;
            HuggingFaceFileRef ref = HuggingFaceFileRef.parse(descriptor.currentUrl());
            if (ref == null) {
                if (descriptor.required()) {
                    throw new IllegalStateException("Not a Hugging Face file URL: " + descriptor.currentUrl());
                }
                listener.onLog("Skipping non-Hugging-Face optional file: " + descriptor.localFilename());
                index++;
                continue;
            }

            Path targetFile = targetDir.resolve(descriptor.localFilename());
            listener.onProgress(percent(completed, 0.0d, total), descriptor.localFilename());
            listener.onLog("Downloading " + descriptor.localFilename() + " from " + ref.getRepoId());
            try {
                boolean downloaded = loader.download(ref, targetFile, force, 0L,
                        new HuggingFaceModelLoader.FileProgressListener() {
                            @Override
                            public void onProgress(long bytesDownloaded, long totalBytes, int filePercent) {
                                double fraction = filePercent <= 0 ? 0.0d : Math.min(1.0d, filePercent / 100.0d);
                                listener.onProgress(percent(completed, fraction, total), descriptor.localFilename());
                            }
                        });
                listener.onLog((downloaded ? "Downloaded " : "Kept existing ") + descriptor.localFilename());
            } catch (HuggingFaceDownloadException ex) {
                if (descriptor.required()) {
                    throw ex;
                }
                listener.onLog("Optional file not available, skipping: " + descriptor.localFilename()
                        + " (" + ex.getMessage() + ")");
            }
            index++;
            listener.onProgress(percent(index, 0.0d, total), descriptor.localFilename());
        }
    }

    private static int percent(int completedFiles, double currentFraction, int total) {
        if (total <= 0) {
            return 100;
        }
        double value = (completedFiles + currentFraction) / total * 100.0d;
        return (int) Math.max(0L, Math.min(100L, Math.round(value)));
    }

    private static final class FutureTaskAdapter implements Task {
        private final Future<?> future;

        private FutureTaskAdapter(Future<?> future) {
            this.future = future;
        }

        @Override
        public void cancel() {
            future.cancel(true);
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "askai-hf-download-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}

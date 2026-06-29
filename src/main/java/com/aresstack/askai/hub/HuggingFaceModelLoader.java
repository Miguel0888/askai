package com.aresstack.askai.hub;

import com.aresstack.huggingface.hub.HuggingFaceHub;
import com.aresstack.huggingface.hub.HuggingFaceHubException;
import com.aresstack.huggingface.hub.download.DownloadProgress;
import com.aresstack.huggingface.hub.download.DownloadResult;
import com.aresstack.huggingface.hub.download.OverwritePolicy;
import com.aresstack.huggingface.hub.model.HubFile;
import com.aresstack.huggingface.hub.model.ModelSummary;
import com.aresstack.winproxy.ProxyConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AskAI adapter over the {@code huggingface4j} hub client.
 *
 * <p>This is the single seam where AskAI talks to {@code huggingface4j} to load model
 * files from the Hugging Face Hub. It maps catalog file URLs ({@link HuggingFaceFileRef})
 * to per-file downloads with progress, resume and optional size verification, and maps
 * the library's DTOs/exceptions to AskAI types so the rest of the app never sees
 * huggingface4j classes.</p>
 */
public class HuggingFaceModelLoader {

    private final String token;
    private final ProxyConfiguration proxyConfiguration;
    private final Map<String, HuggingFaceHub> hubByEndpoint = new HashMap<String, HuggingFaceHub>();

    public HuggingFaceModelLoader(String token, ProxyConfiguration proxyConfiguration) {
        this.token = token == null ? "" : token.trim();
        this.proxyConfiguration = proxyConfiguration;
    }

    /** Searches the Hub for model repositories, most-downloaded first. */
    public List<HuggingFaceModelSearchResult> searchModels(String query, int limit) throws HuggingFaceDownloadException {
        try {
            List<ModelSummary> models = hub("").models().search(query == null ? "" : query)
                    .sortByDownloads()
                    .limit(limit)
                    .execute()
                    .getModels();
            List<HuggingFaceModelSearchResult> mapped = new ArrayList<HuggingFaceModelSearchResult>();
            for (ModelSummary summary : models) {
                mapped.add(new HuggingFaceModelSearchResult(
                        summary.getRepoId() != null ? summary.getRepoId() : summary.getId(),
                        summary.getAuthor(),
                        summary.getTask(),
                        summary.getLibrary(),
                        summary.getDownloads() == null ? 0L : summary.getDownloads(),
                        summary.getLikes() == null ? 0L : summary.getLikes(),
                        Boolean.TRUE.equals(summary.getGated())));
            }
            return mapped;
        } catch (HuggingFaceHubException ex) {
            throw new HuggingFaceDownloadException("Hugging Face search failed: " + ex.getMessage(), ex);
        }
    }

    /** Lists the files of a model repository revision on the Hub. */
    public List<HuggingFaceModelFile> listFiles(String endpoint, String repoId, String revision)
            throws HuggingFaceDownloadException {
        try {
            List<HubFile> files = hub(endpoint).models().model(repoId).files()
                    .revision(revision == null || revision.isBlank() ? "main" : revision)
                    .execute();
            List<HuggingFaceModelFile> mapped = new ArrayList<HuggingFaceModelFile>();
            for (HubFile file : files) {
                mapped.add(new HuggingFaceModelFile(file.getPath(), file.getSize() == null ? -1L : file.getSize()));
            }
            return mapped;
        } catch (HuggingFaceHubException ex) {
            throw new HuggingFaceDownloadException("Could not list files for " + repoId + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Downloads a single file to {@code targetFile}. When {@code expectedSize} is positive it is
     * verified after download. Returns whether the file was actually downloaded ({@code false}
     * means an existing file was kept because {@code overwrite} was false).
     */
    public boolean download(HuggingFaceFileRef ref, Path targetFile, boolean overwrite, long expectedSize,
                            final FileProgressListener listener) throws HuggingFaceDownloadException {
        try {
            ensureParentDirectory(targetFile);
            var request = hub(ref.getEndpoint()).models()
                    .model(ref.getRepoId())
                    .file(ref.getPath())
                    .revision(ref.getRevision())
                    .downloadTo(targetFile)
                    .overwritePolicy(overwrite ? OverwritePolicy.OVERWRITE : OverwritePolicy.SKIP_IF_EXISTS)
                    .resume(true);
            if (expectedSize > 0L) {
                request = request.verifySize(expectedSize);
            }
            if (listener != null) {
                request = request.onProgress((DownloadProgress progress) ->
                        listener.onProgress(progress.getBytesDownloaded(), progress.getTotalBytes(), progress.getPercent()));
            }
            DownloadResult result = request.execute();
            return result == null || !result.isSkipped();
        } catch (HuggingFaceHubException ex) {
            throw new HuggingFaceDownloadException("Download of " + ref.getPath() + " failed: " + ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new HuggingFaceDownloadException("Could not prepare target for " + ref.getPath() + ": " + ex.getMessage(), ex);
        }
    }

    private HuggingFaceHub hub(String endpoint) {
        String key = endpoint == null || endpoint.isBlank() ? "" : endpoint;
        HuggingFaceHub existing = hubByEndpoint.get(key);
        if (existing != null) {
            return existing;
        }
        HuggingFaceHub.Builder builder = HuggingFaceHub.standard();
        if (!key.isEmpty()) {
            builder = builder.endpoint(key);
        }
        builder = token.isEmpty() ? builder.anonymous() : builder.accessToken(token);
        HuggingFaceProxySettings proxy = HuggingFaceProxySettings.from(proxyConfiguration);
        if (proxy.hasProxy()) {
            builder = builder.proxy(proxy.proxy());
        } else if (proxy.hasSelector()) {
            builder = builder.proxySelector(proxy.selector());
        }
        HuggingFaceHub hub = builder.build();
        hubByEndpoint.put(key, hub);
        return hub;
    }

    private static void ensureParentDirectory(Path targetFile) throws IOException {
        Path parent = targetFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    /** Receives byte-level progress for one file download. */
    public interface FileProgressListener {
        void onProgress(long bytesDownloaded, long totalBytes, int percent);
    }

    /** One file entry of a Hub model repository. */
    public static final class HuggingFaceModelFile {
        private final String path;
        private final long size;

        public HuggingFaceModelFile(String path, long size) {
            this.path = path == null ? "" : path;
            this.size = size;
        }

        public String getPath() {
            return path;
        }

        public long getSize() {
            return size;
        }
    }
}

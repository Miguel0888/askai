package com.aresstack.askai.hub;

/**
 * A Hugging Face file location parsed from a {@code /resolve/} download URL.
 *
 * <p>AskAI's model catalog stores file URLs such as
 * {@code https://huggingface.co/{repo}/resolve/{revision}/{path}}. The huggingface4j
 * client works with the repo id, revision and repo-relative path instead, so this type
 * is the seam that turns a catalog URL into the inputs huggingface4j expects.</p>
 */
public final class HuggingFaceFileRef {

    private static final String RESOLVE_MARKER = "/resolve/";

    private final String endpoint;
    private final String repoId;
    private final String revision;
    private final String path;

    public HuggingFaceFileRef(String endpoint, String repoId, String revision, String path) {
        this.endpoint = endpoint;
        this.repoId = repoId;
        this.revision = revision;
        this.path = path;
    }

    /**
     * Parses a Hugging Face {@code resolve} URL. Returns {@code null} when the URL is not
     * a recognizable Hugging Face file URL.
     */
    public static HuggingFaceFileRef parse(String url) {
        if (url == null) {
            return null;
        }
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        int pathStart = url.indexOf('/', schemeEnd + 3);
        if (pathStart < 0) {
            return null;
        }
        String endpoint = url.substring(0, pathStart);
        String path = url.substring(pathStart);
        int resolveIndex = path.indexOf(RESOLVE_MARKER);
        if (resolveIndex <= 0) {
            return null;
        }
        String repoId = trimSlashes(path.substring(0, resolveIndex));
        String afterResolve = path.substring(resolveIndex + RESOLVE_MARKER.length());
        int revisionSlash = afterResolve.indexOf('/');
        if (revisionSlash <= 0 || revisionSlash == afterResolve.length() - 1) {
            return null;
        }
        String revision = afterResolve.substring(0, revisionSlash);
        String remotePath = afterResolve.substring(revisionSlash + 1);
        if (repoId.isEmpty() || remotePath.isEmpty()) {
            return null;
        }
        return new HuggingFaceFileRef(endpoint, repoId, revision, remotePath);
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRepoId() {
        return repoId;
    }

    public String getRevision() {
        return revision;
    }

    public String getPath() {
        return path;
    }

    @Override
    public String toString() {
        return repoId + "@" + revision + "/" + path;
    }

    private static String trimSlashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '/') {
            start++;
        }
        while (end > start && value.charAt(end - 1) == '/') {
            end--;
        }
        return value.substring(start, end);
    }
}

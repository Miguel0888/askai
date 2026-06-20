package com.aresstack.askai.client;

/**
 * Normalizes Ollama blob digests so the {@code sha256:} algorithm prefix is set exactly once.
 *
 * <p>Internally AskAI computes plain 64-character hex digests (see
 * {@code Sha256DigestService}). The Ollama API expects {@code sha256:<hex>} on
 * {@code /api/blobs/:digest} and in the {@code /api/create} files map. Passing an
 * already-prefixed value back through the prefixing logic used to produce
 * {@code sha256:sha256:<hex>}, which Ollama rejects with HTTP 400.</p>
 */
public final class OllamaDigest {

    private static final String PREFIX = "sha256:";

    private OllamaDigest() {
    }

    /**
     * Returns the plain hex digest without any {@code sha256:} prefix.
     */
    public static String hex(String digest) {
        if (digest == null) {
            return "";
        }
        String trimmed = digest.trim();
        while (trimmed.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) {
            trimmed = trimmed.substring(PREFIX.length());
        }
        return trimmed;
    }

    /**
     * Returns the digest with exactly one {@code sha256:} prefix, regardless of whether
     * the input already carried one.
     */
    public static String prefixed(String digest) {
        return PREFIX + hex(digest);
    }
}

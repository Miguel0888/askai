package com.aresstack.askai.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the digest-prefix rule: the {@code sha256:} prefix must be applied exactly
 * once, so a blob check never sends {@code sha256:sha256:<hex>} and gets HTTP 400.
 */
class OllamaDigestTest {

    private static final String HEX = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void prefixesPlainHexExactlyOnce() {
        assertEquals("sha256:" + HEX, OllamaDigest.prefixed(HEX));
    }

    @Test
    void doesNotDoublePrefixWhenAlreadyPrefixed() {
        assertEquals("sha256:" + HEX, OllamaDigest.prefixed("sha256:" + HEX));
    }

    @Test
    void collapsesRepeatedPrefixes() {
        assertEquals("sha256:" + HEX, OllamaDigest.prefixed("sha256:sha256:" + HEX));
    }

    @Test
    void hexStripsAnyPrefix() {
        assertEquals(HEX, OllamaDigest.hex("sha256:" + HEX));
        assertEquals(HEX, OllamaDigest.hex(HEX));
    }

    @Test
    void prefixHandlingIsCaseInsensitiveAndTrimmed() {
        assertEquals("sha256:" + HEX, OllamaDigest.prefixed("  SHA256:" + HEX + "  "));
    }
}

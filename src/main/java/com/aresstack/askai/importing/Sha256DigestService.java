package com.aresstack.askai.importing;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes Ollama-compatible SHA256 blob digests.
 */
public final class Sha256DigestService {

    private static final int BUFFER_SIZE = 1024 * 1024;

    public String digest(Path file) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream inputStream = Files.newInputStream(file);
             DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
            while (digestInputStream.read(buffer) >= 0) {
                // Consume stream to update digest.
            }
        }
        return "sha256:" + toHex(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(Character.forDigit((value >> 4) & 0x0f, 16));
            builder.append(Character.forDigit(value & 0x0f, 16));
        }
        return builder.toString();
    }
}

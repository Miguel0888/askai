package com.aresstack.askai.importing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class Sha256DigestServiceTest {

    @Test
    void digestReturnsPlainHexWithoutOllamaPrefix(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("sample.bin");
        Files.write(file, "abc".getBytes(StandardCharsets.UTF_8));

        String digest = new Sha256DigestService().digest(file);

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", digest);
        assertFalse(digest.contains(":"));
    }
}

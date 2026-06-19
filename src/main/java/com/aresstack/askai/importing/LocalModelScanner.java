package com.aresstack.askai.importing;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scans a downloaded Hugging Face model directory for files to import into Ollama.
 */
public final class LocalModelScanner {

    public List<LocalModelFile> scanImportableFiles(Path modelDirectory) throws IOException {
        ArrayList<LocalModelFile> files = new ArrayList<LocalModelFile>();
        if (!Files.isDirectory(modelDirectory)) {
            return files;
        }
        try (Stream<Path> stream = Files.walk(modelDirectory)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isImportableFile)
                    .sorted(Comparator.comparing(path -> relativePath(modelDirectory, path)))
                    .forEach(path -> addFile(files, modelDirectory, path));
        }
        return files;
    }

    private void addFile(List<LocalModelFile> files, Path modelDirectory, Path path) {
        try {
            files.add(new LocalModelFile(path, relativePath(modelDirectory, path), Files.size(path)));
        } catch (IOException ignored) {
            // Skip files that cannot be read at scan time.
        }
    }

    private boolean isImportableFile(Path path) {
        String name = path.getFileName().toString();
        return !name.endsWith(".part") && !name.equalsIgnoreCase("desktop.ini");
    }

    private static String relativePath(Path modelDirectory, Path file) {
        URI base = modelDirectory.toUri();
        URI target = file.toUri();
        return base.relativize(target).getPath();
    }
}

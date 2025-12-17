package cz.mipit.sscc.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class DirectoryTreeParser {
    private final List<InputFile> files = new ArrayList<>();

    public DirectoryTreeParser(final Path root) throws IOException {
        getPaths(root);
    }

    private void getPaths(final Path root) throws IOException {
        try (Stream<Path> entries = Files.walk(root)) {
            entries.forEach(path -> {
                if (!Files.isRegularFile(path)) {
                    return;
                }

                final String name = path.getFileName().toString();
                if (!name.endsWith(".c") && !name.endsWith(".ssc")) {
                    return;
                }

                final Path abs = path.toAbsolutePath();
                final InputFile inputFile = InputFile.fromAbsolutePath(abs);
                files.add(inputFile);
            });
        }
    }

    public List<InputFile> getFiles() {
        return files;
    }
}

package cz.mipit.sscc.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class DirectoryTreeParser {
    public static Set<InputFile> getFilesInDirectory(final Path root, Set<String> extensions) throws IOException {
        final Set<InputFile> files = new HashSet<>();
        try (Stream<Path> entries = Files.walk(root)) {
            entries.forEach(path -> {
                if (!Files.isRegularFile(path)) {
                    return;
                }

                final String name = path.getFileName().toString();
                for (String extension : extensions) {
                    if (!name.endsWith(extension)) {
                        return;
                    }
                }

                final Path abs = path.toAbsolutePath();
                final InputFile inputFile = InputFile.fromPath(abs);
                files.add(inputFile);
            });
        }
        return files;
    }

    public static Set<InputFile> getFilesInDirectory(final Path root) throws IOException {
        return getFilesInDirectory(root, Set.of("ssc", "c"));
    }
}

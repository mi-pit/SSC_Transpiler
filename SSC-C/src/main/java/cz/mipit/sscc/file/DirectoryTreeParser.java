package cz.mipit.sscc.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class DirectoryTreeParser {
    public static Set<File> getFilesInDirectory(final Path root, Set<String> extensions) throws IOException {
        final Set<File> files = new HashSet<>();
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
                final File file = File.fromPath(abs);
                files.add(file);
            });
        }
        return files;
    }

}

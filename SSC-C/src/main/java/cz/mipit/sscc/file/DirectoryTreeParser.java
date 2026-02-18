package cz.mipit.sscc.file;

import cz.mipit.sscc.Logger;
import cz.mipit.sscc.util.ExitValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public final class DirectoryTreeParser {
    public static void getPathsInDirectory(final Path root, final Set<InputFile> files) throws IOException {
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
}

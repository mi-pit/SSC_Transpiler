package cz.mipit.sscc.file;

import cz.mipit.sscc.Logger;
import cz.mipit.sscc.util.ExitValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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

    public static Set<Path> getLibraryFiles(final Path root) {
        try {
            final Set<InputFile> files = new HashSet<>();
            getPathsInDirectory(root, files);
            return files.stream().map(InputFile::toPath).collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getLibraryRoot() {
        final String ssclibHome = System.getenv("SSCLIB_HOME");
        if (ssclibHome == null) {
            Logger.err(ExitValue.LIBRARY_NOT_FOUND, "could not find ssc library: SSCLIB_HOME not set");
            assert false : "unreachable";
        }

        final Path asPath = Path.of(ssclibHome);

        if (!Files.exists(asPath)) {
            Logger.err(ExitValue.LIBRARY_NOT_FOUND, "could not find ssc library: " + ssclibHome);
        }

        if (!Files.isDirectory(asPath)) {
            Logger.err(ExitValue.LIBRARY_NOT_FOUND, "not a directory: " + ssclibHome);
        }

        return asPath;
    }
}

package cz.mipit.sscc.file;

import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable class.
 * Objects are made up of
 * <ul>
 *   <li>Directory: {@link java.nio.file.Path}</li>
 *   <li>Name: {@link java.lang.String}</li>
 *   <li>Suffix: {@link java.lang.String} (null means no file extension)</li>
 * </ul>
 *
 * @apiNote Sibling to {@link Path}, may be converted to and from a {@link Path}
 * @implNote All getters have their data cached.
 */
public final class InputFile {
    private final FileType fileType;

    private final Path dir;
    private final String name;
    private final @Nullable String suffix;

    private final String fullName;
    private final Path path;
    private final Path absolutePath;

    private final String absolutePathString;

    private InputFile(FileType fileType,
                      Path dir, String name,
                      @Nullable String suffix,
                      Path path, Path absolutePath) {
        this.fileType = Objects.requireNonNull(fileType);
        this.dir = Objects.requireNonNull(dir, "File must have a directory");
        this.name = Objects.requireNonNull(name, "File must have a name");
        this.suffix = suffix;

        this.fullName = suffix == null ? name : name + "." + suffix;
        this.path = path;
        this.absolutePath = absolutePath;
        this.absolutePathString = absolutePath.toString();
    }

    public static InputFile create(Path dir, String name, @Nullable String suffix) {
        final String fullName = suffix == null ? name : name + "." + suffix;
        final Path path = Path.of(dir.toString(), fullName);
        final Path absolutePath = path.toAbsolutePath();

        return new InputFile(
                FileType.fromString(suffix), dir, name, suffix,
                path, absolutePath
        );
    }


    private record FileData(Path dir, String name, String suffix) {
        private static @NotNull FileData fromPath(Path path) {
            final Path dir = Objects.requireNonNullElseGet(
                    path.getParent(),
                    () -> Path.of(".")
            ).toAbsolutePath().normalize();

            final String fullName = path.getFileName().toString();

            final int dotIndex = fullName.lastIndexOf('.');
            final String name = dotIndex == -1 ? fullName : fullName.substring(0, dotIndex);
            final String suffix = dotIndex == -1 ? null : fullName.substring(dotIndex + 1);
            return new FileData(dir, name, suffix);
        }
    }

    public static InputFile fromPath(FileType fileType, Path path) {
        FileData result = FileData.fromPath(path);
        return new InputFile(
                fileType, result.dir(), result.name(),
                result.suffix(), path, path.toAbsolutePath()
        );
    }


    public static InputFile fromPath(final Path path) {
        final FileData result = FileData.fromPath(path);

        return new InputFile(
                FileType.fromString(result.suffix()),
                result.dir(), result.name(), result.suffix(),
                path, path.toAbsolutePath()
        );
    }

    /// Creates a new object with the same directory and name and changed extension
    public InputFile getChangedSuffix(final @Nullable String newSuffix) {
        return create(dir, name, newSuffix);
    }

    public Path toPath() {
        return path;
    }

    public Path toAbsolutePath() {
        return absolutePath;
    }

    public String absolutePathString() {
        return absolutePathString;
    }

    public Path directory() {
        return dir;
    }

    public String name() {
        return name;
    }

    public String fullName() {
        return fullName;
    }

    public String suffix() {
        return suffix;
    }

    public FileType getFileType() {
        return fileType;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof InputFile inputFile) {
            return Objects.equals(dir, inputFile.dir)
                    && Objects.equals(name, inputFile.name)
                    && Objects.equals(suffix, inputFile.suffix);
        }
        if (o instanceof Path p) {
            return equals(p);
        }
        return false;
    }

    public boolean equals(Path path) {
        return Objects.equals(path, this.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dir, name, suffix);
    }

    @Override
    public String toString() {
        return absolutePathString();
    }
}

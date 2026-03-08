package cz.mipit.sscc.file;

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
    private final Path dir;
    private final String name;
    private final @Nullable String suffix;

    private final String fullName;
    private final Path path;
    private final Path absolutePath;

    public InputFile(Path dir, String name, String suffix) {
        this.dir = Objects.requireNonNull(dir, "File must have a directory");
        this.name = Objects.requireNonNull(name, "File must have a name");
        this.suffix = suffix;

        fullName = suffix == null ? name : name + "." + suffix;
        path = Path.of(dir.toString(), fullName());
        absolutePath = path.toAbsolutePath();
    }

    public static InputFile fromPath(final Path path) {
        final Path dir = Objects.requireNonNullElseGet(
                path.getParent(),
                () -> Path.of(".").toAbsolutePath().normalize()
        );

        final String fullName = path.getFileName().toString();

        final int dotIndex = fullName.lastIndexOf('.');
        final String name = dotIndex == -1 ? fullName : fullName.substring(0, dotIndex);
        final String suffix = dotIndex == -1 ? null : fullName.substring(dotIndex + 1);

        return new InputFile(dir, name, suffix);
    }

    /// Creates a new object with the same directory and name and changed extension
    public InputFile getChangedSuffix(final @Nullable String newSuffix) {
        return new InputFile(dir, name, newSuffix);
    }

    public Path toPath() {
        return path;
    }

    public Path toAbsolutePath() {
        return absolutePath;
    }

    public String absolutePathString() {
        return absolutePath.toString();
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


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof InputFile inputFile)) {
            return false;
        }
        return Objects.equals(dir, inputFile.dir)
                && Objects.equals(name, inputFile.name)
                && Objects.equals(suffix, inputFile.suffix);
    }

    public boolean equals(Path path) {
        assert Objects.equals(this.path, this.absolutePath);
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

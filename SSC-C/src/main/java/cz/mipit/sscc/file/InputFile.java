package cz.mipit.sscc.file;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable class.
 * <p>
 * Sibling to {@link Path}
 * </p>
 */
public final class InputFile {
    private final Path dir;
    private final String name;
    private final String suffix;

    private final String fullName;
    private final Path path;
    private final Path absolutePath;

    public InputFile(Path dir, String name, String suffix) {
        this.dir = Objects.requireNonNull(dir, "File must have a directory");
        this.name = Objects.requireNonNull(name, "File must have a name");
        this.suffix = suffix;

        fullName = suffix == null ? name : name + "." + suffix;
        path = Path.of(dir.toString(), getFullName());
        absolutePath = path.toAbsolutePath();
    }

    public static InputFile fromAbsolutePath(final Path fileAbsolutePath) {
        final Path dir = fileAbsolutePath.getParent();
        final String fullName = fileAbsolutePath.getFileName().toString();

        final int dotIndex = fullName.lastIndexOf('.');
        if (dotIndex == -1) {
            return new InputFile(dir, fullName, null);
        }

        return new InputFile(
                dir == null ? Path.of(".").toAbsolutePath() : dir,
                fullName.substring(0, dotIndex),
                fullName.substring(dotIndex + 1)
        );
    }

    public Path toPath() {
        return path;
    }

    public Path toAbsolutePath() {
        return absolutePath;
    }

    public String getFullName() {
        return fullName;
    }

    public InputFile getChangedSuffix(final String newSuffix) {
        return new InputFile(dir, name, newSuffix);
    }

    public String absolutePathString() {
        return absolutePath.toString();
    }

    public Path dir() {
        return dir;
    }

    public String name() {
        return name;
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
        return absolutePath.toString();
    }
}

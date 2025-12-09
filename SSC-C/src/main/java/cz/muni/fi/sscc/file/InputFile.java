package cz.muni.fi.sscc.file;

import java.nio.file.Path;
import java.util.Objects;

public record InputFile(Path dir, String name, String suffix) {
    public InputFile(Path dir, String name, String suffix) {
        this.dir = Objects.requireNonNull(dir, "File must have a directory");
        this.name = Objects.requireNonNull(name, "File must have a name");
        this.suffix = suffix;
    }

    public static InputFile fromAbsolutePath(final Path fileAbsolutePath) {
        final Path dir = fileAbsolutePath.getParent();

        final String fullName = fileAbsolutePath.getFileName().toString();

        final int dotIndex = fullName.lastIndexOf('.');
        if (dotIndex == -1) {
            return new InputFile(dir, fullName, null);
        }

        return new InputFile(
                dir == null ? Path.of(".") : dir,
                fullName.substring(0, dotIndex),
                fullName.substring(dotIndex + 1)
        );
    }

    public Path toAbsolutePath() {
        return Path.of(dir.toString(), getFullName());
    }

    public String getFullName() {
        return suffix == null ? name : name + "." + suffix;
    }

    public InputFile getChangedSuffix(final String newSuffix) {
        return new InputFile(dir, name, newSuffix);
    }

    public String absolutePathString() {
        return String.format("%s/%s", dir, getFullName());
    }
}

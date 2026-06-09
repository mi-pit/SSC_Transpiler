package cz.mipit.sscc.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestFile {
    public static final String DIRNAME = "/Users/user/";
    public static final String FILENAME = "data";
    public static final String EXTENSION = "csv";

    @Test
    void create() {
        final File constructed = File.create(Path.of(DIRNAME), FILENAME, EXTENSION);
        assertEquals(FILENAME + "." + EXTENSION, constructed.fullName());
        assertEquals(FILENAME, constructed.name());
        assertEquals(EXTENSION, constructed.suffix());

        final Path path = Path.of(DIRNAME, FILENAME + "." + EXTENSION);
        final File fromAbsolutePath = File.fromPath(path);
        assertEquals(constructed, fromAbsolutePath);

        assertEquals(fromAbsolutePath.toAbsolutePath(), path.toAbsolutePath());
    }
}

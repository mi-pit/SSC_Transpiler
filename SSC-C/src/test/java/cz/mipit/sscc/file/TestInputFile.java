package cz.mipit.sscc.file;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInputFile {
    public static final String DIRNAME = "/Users/user/";
    public static final String FILENAME = "data";
    public static final String EXTENSION = "csv";

    @Test
    void create() {
        final InputFile constructed = InputFile.create(Path.of(DIRNAME), FILENAME, EXTENSION);
        assertEquals(FILENAME + "." + EXTENSION, constructed.fullName());
        assertEquals(FILENAME, constructed.name());
        assertEquals(EXTENSION, constructed.suffix());

        final Path path = Path.of(DIRNAME, FILENAME + "." + EXTENSION);
        final InputFile fromAbsolutePath = InputFile.fromPath(path);
        assertEquals(constructed, fromAbsolutePath);

        assertEquals(fromAbsolutePath.toAbsolutePath(), path.toAbsolutePath());
    }
}

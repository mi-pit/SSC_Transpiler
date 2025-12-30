package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.preprocessor.Preprocessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestPreprocessor {
    private static final Path testFilePath = Paths.get("test.ssc").normalize().toAbsolutePath();
    private static final Path outputFilePath = Path.of("test.c").normalize().toAbsolutePath();
    private static final String code = """
            #  include <stdio.h>
            
            int main ( void ) {
                printf("Hello, world!");
                return 0;
            }
            """;
    private static final List<String> lines = List.of(code.split(System.lineSeparator()));

    @BeforeEach
    void setUp() throws IOException {
        Files.writeString(testFilePath, code);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(testFilePath);
        Files.deleteIfExists(outputFilePath);
    }

    @Test
    void testInputFile() {
        final Path dir = Path.of(".");
        final String[] fileName = testFilePath.getFileName().toString().split("\\.");
        assertTrue(fileName.length >= 2);

        final String name = fileName[fileName.length - 2];
        final String extension = fileName[fileName.length - 1];

        final InputFile inputFile = new InputFile(dir, name, extension);
        final Path inputFileAbsolutePath = inputFile.toAbsolutePath().normalize();

        assertTrue(inputFileAbsolutePath.isAbsolute());
        assertEquals(inputFileAbsolutePath, testFilePath.toAbsolutePath());
        assertEquals(inputFile.getFullName(), testFilePath.getFileName().toString());

        final InputFile changedSuffix = inputFile.getChangedSuffix("c");
        assertNotEquals(changedSuffix, inputFile);
        assertEquals("test.c", changedSuffix.getFullName());

        final InputFile fromAbsolutePath = InputFile.fromAbsolutePath(inputFileAbsolutePath);
        assertEquals(fromAbsolutePath.toAbsolutePath(), testFilePath.toAbsolutePath());
    }

    @Test
    void testPreprocessor() {
        try {
            Preprocessor.preprocessSSC(InputFile.fromAbsolutePath(testFilePath), outputFilePath);

            assertTrue(Files.exists(outputFilePath));

            assertEquals(code.trim(), Files.readString(outputFilePath).trim());
        } catch (IOException e) {
            Assertions.fail("IO Exception");
        }
    }
}

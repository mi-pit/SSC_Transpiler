package cz.mipit.sscc.ssc.code;

import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.DirectoryTreeParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.SSCCompiler;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.collection.Box;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class TestStubs {
    private static final String DIRECTORY = "../ssc-examples";
    private static final Path VALID = Path.of(DIRECTORY, "valid");
    private static final Path INVALID = Path.of(DIRECTORY, "invalid");

    @BeforeAll
    static void ensureSSCLib() {
        Assertions.assertNotNull(SSCCompiler.SSCLIB_HOME);
    }

    @AfterAll
    static void deleteJunkFiles() {
        final Set<InputFile> files = new HashSet<>();
        files.addAll(getInputFiles(VALID));
        files.addAll(getInputFiles(INVALID));

        for (InputFile file : files) {
            Assertions.assertEquals("ssc", file.suffix());
            final InputFile correspondingCFile = file.getChangedSuffix("c");
            Assertions.assertDoesNotThrow(() -> Files.deleteIfExists(correspondingCFile.toPath()));
        }
    }

    @Test
    void testSuccesses() {
        final Set<InputFile> files = getInputFiles(VALID);
        for (final InputFile fileName : files) {
            final SSCCompiler compiler = getCompilerOfFile(fileName);

            final Box<ExitValue> exitValue = new Box<>();
            Assertions.assertDoesNotThrow(() -> {
                exitValue.item = (compiler.run());
            });
            Assertions.assertSame(ExitValue.SUCCESS, exitValue.item, "In file `%s`".formatted(fileName));
        }
    }

    @Test
    void testFailures() {
        final Set<InputFile> files = getInputFiles(INVALID);
        for (final InputFile fileName : files) {
            final SSCCompiler compiler = getCompilerOfFile(fileName);

            final Box<ExitValue> exitValue = new Box<>();
            Assertions.assertDoesNotThrow(() -> {
                exitValue.item = compiler.run();
            });
            Assertions.assertTrue(
                    exitValue.item == ExitValue.C_COMPILATION_FAIL
                            || exitValue.item == ExitValue.TRANSPILATION_FAIL,
                    "`%s`".formatted(fileName)
            );
        }
    }

    private static Set<InputFile> getInputFiles(Path dir) {
        final Set<InputFile> files;
        try {
            files = DirectoryTreeParser.getFilesInDirectory(dir, Set.of("ssc"));
        } catch (IOException e) {
            Assertions.fail(e);
            throw new AssertionError("unreachable");
        }
        return files;
    }

    private static SSCCompiler getCompilerOfFile(InputFile inFile) {
        final SSCCOptions options = SSCCOptions.newWithDefaults();
        options.addFile(inFile);

        return new SSCCompiler(options);
    }
}

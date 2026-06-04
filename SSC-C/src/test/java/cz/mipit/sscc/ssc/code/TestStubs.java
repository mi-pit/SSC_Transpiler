package cz.mipit.sscc.ssc.code;

import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.DirectoryTreeParser;
import cz.mipit.sscc.file.File;
import cz.mipit.sscc.ssc.compiler.SSCCompiler;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.collection.Box;
import cz.mipit.sscc.util.collection.builder.HashSetBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class TestStubs {
    private static final String DIRECTORY = "../ssc-examples";
    private static final Path VALID = Path.of(DIRECTORY, "valid");
    private static final Path INVALID = Path.of(DIRECTORY, "invalid");

    @AfterAll
    static void deleteJunkFiles() {
        final Set<File> files = HashSetBuilder
                .from(getInputFiles(VALID))
                .plusMany(getInputFiles(INVALID))
                .build();

        for (final File file : files) {
            Assertions.assertEquals("ssc", file.suffix());
            final File correspondingCFile = file.getChangedSuffix("c");
            Assertions.assertDoesNotThrow(() -> Files.deleteIfExists(correspondingCFile.toPath()));
        }
    }

    @Test
    void testSuccesses() {
        final Set<File> files = getInputFiles(VALID);
        for (final File fileName : files) {
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
        final Set<File> files = getInputFiles(INVALID);
        for (final File fileName : files) {
            final SSCCompiler compiler = getCompilerOfFile(fileName);

            final String debugFilename = "'%s'".formatted(fileName);

            final Box<ExitValue> exitValue = new Box<>();
            Assertions.assertDoesNotThrow(
                    () -> exitValue.item = compiler.run(),
                    debugFilename
            );

            final String debugMsg = "ex=%s: %s".formatted(exitValue, debugFilename);
            Assertions.assertTrue(
                    exitValue.item == ExitValue.C_COMPILATION_FAIL
                    || exitValue.item == ExitValue.TRANSPILATION_FAIL,
                    debugMsg
            );
        }
    }

    private static Set<File> getInputFiles(Path dir) {
        final Set<File> files;
        try {
            files = DirectoryTreeParser.getFilesInDirectory(dir, Set.of("ssc"));
        } catch (IOException e) {
            Assertions.fail(e);
            throw new AssertionError("unreachable");
        }
        return files;
    }

    private static SSCCompiler getCompilerOfFile(File inFile) {
        final SSCCOptions options = SSCCOptions.newWithDefaults();
        options.addFile(inFile);

        return new SSCCompiler(options);
    }
}

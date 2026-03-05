package cz.mipit.sscc.ssc.code;

import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.SSCCompiler;
import cz.mipit.sscc.util.ExitValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

public class TestStubs {
    private static final String F_TYPEDEF = "typedef.ssc";
    private static final String F_SSCLIB = "ssclib-include.ssc";

    private static final String DIRECTORY = "../ssc-examples";

    private static final String[] SUCCESSES = new String[]{
            F_TYPEDEF,
            F_SSCLIB,
    };

    private static final Set<String> FAILURES = Set.of(
            "inval_method.ssc",
            "antlr-error.ssc",
            "ternary.ssc",
            "flagset.ssc"
    );


    @BeforeEach
    void setUp() {
        Assertions.assertNotNull(SSCCompiler.SSCLIB_HOME);
    }

    @Test
    void testSuccesses() {
        for (final String fileName : SUCCESSES) {
            final SSCCompiler compiler = getCompilerOfFile(fileName);

            Assertions.assertDoesNotThrow(() -> {
                final var exitValue = compiler.run();
                Assertions.assertSame(ExitValue.SUCCESS, exitValue, "`%s`".formatted(fileName));
            });
        }
    }

    @Test
    void testFailures() {
        for (final String fileName : FAILURES) {
            final SSCCompiler compiler = getCompilerOfFile(fileName);

            Assertions.assertDoesNotThrow(() -> {
                final ExitValue exitValue = compiler.run();
                Assertions.assertTrue(
                        exitValue != ExitValue.LIBRARY_NOT_FOUND
                                && exitValue != ExitValue.INVALID_ARGUMENTS
                );

                Assertions.assertNotSame(ExitValue.SUCCESS, exitValue, "`%s`".formatted(fileName));
            });
        }
    }

    private static SSCCompiler getCompilerOfFile(String fileName) {
        final InputFile inFile = InputFile.fromAbsolutePath(Path.of(DIRECTORY, fileName));
        final SSCCOptions options = SSCCOptions.newWithDefaults();
        options.addFile(inFile);
        options.setCompileTarget(inFile.getChangedSuffix(null).absolutePathString());

        return new SSCCompiler(options);
    }
}

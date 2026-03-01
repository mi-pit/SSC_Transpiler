package cz.mipit.sscc.ssc.code;

import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.SSCCompiler;
import cz.mipit.sscc.util.ExitValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

public class TestStubs {
    private ExitValue exitValue;

    private static final String F_TYPEDEF = "typedef.ssc";
    private static final String F_SSCLIB = "ssclib-include.ssc";

    private static final String DIRECTORY = "../ssc-examples";

    private static final String[] SUCCESSES = new String[]{
            F_TYPEDEF,
            F_SSCLIB,
    };

    private static final Map<String, ExitValue> FAILURES = Map.of(
            "inval_method.ssc", ExitValue.TRANSPILATION_FAIL,
            "antlr-error.ssc", ExitValue.TRANSPILATION_FAIL
    );


    @BeforeEach
    void setUp() {
        Assertions.assertNotNull(SSCCompiler.SSCLIB_HOME);
    }

    @Test
    void testSuccesses() {
        for (final String fileName : SUCCESSES) {
            final SSCCompiler compiler = getSscCompiler(fileName);

            Assertions.assertDoesNotThrow(() -> {exitValue = compiler.run();});
            Assertions.assertSame(ExitValue.SUCCESS, exitValue);
        }
    }

    @Test
    void testFailures() {
        for (final Map.Entry<String, ExitValue> entry : FAILURES.entrySet()) {
            final SSCCompiler compiler = getSscCompiler(entry.getKey());

            Assertions.assertDoesNotThrow(() -> {exitValue = compiler.run();});
            Assertions.assertSame(entry.getValue(), exitValue, "`%s`".formatted(entry.getKey()));
        }
    }

    private static SSCCompiler getSscCompiler(String fileName) {
        final InputFile inFile = InputFile.fromAbsolutePath(Path.of(DIRECTORY, fileName));
        final SSCCOptions options = SSCCOptions.newWithDefaults();
        options.addFile(inFile);
        options.setCompileTarget(inFile.getChangedSuffix(null).absolutePathString());

        return new SSCCompiler(options);
    }
}

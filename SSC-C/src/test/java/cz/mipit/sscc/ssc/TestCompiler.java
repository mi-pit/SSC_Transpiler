package cz.mipit.sscc.ssc;

import cz.mipit.sscc.args.ArgumentParser;
import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.SSCCompiler;
import cz.mipit.sscc.util.ExitValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestCompiler {
    public static final String FILENAME = "test-code.ssc";
    public static final Path PATH = Path.of(FILENAME);

    @BeforeEach
    void setUp() throws IOException {
        if (Files.notExists(PATH)) {
            Files.createFile(PATH);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(PATH);
    }

    @Test
    void options() {
        Assertions.assertDoesNotThrow(() -> (ArgumentParser.parse(new String[]{FILENAME, "-v"})));
    }

    @Test
    void test() {
        final SSCCOptions options = SSCCOptions.newWithDefaults();
        options.addFile(InputFile.fromPath(PATH));
        final Compiler compiler = new SSCCompiler(options);
        final ExitValue returnValue;
        try {
            returnValue = compiler.run();
        } catch (Exception e) {
            Assertions.fail(e);
            throw new AssertionError("Unreachable");
        }
        Assertions.assertTrue(switch (returnValue) {
            case SUCCESS, C_COMPILATION_FAIL, TRANSPILATION_FAIL, IO_EXCEPTION -> true;
            case INVALID_ARGUMENTS, LIBRARY_NOT_FOUND -> false;
        });
    }
}

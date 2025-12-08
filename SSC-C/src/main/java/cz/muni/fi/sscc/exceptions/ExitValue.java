package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.Colors;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    TRANSPILATION_FAIL,
    C_PREPROCESSING_FAIL,
    C_VERIFICATION_FAIL,
    C_COMPILATION_FAIL,
    IO_EXCEPTION,
    ;

    private static final String COLOR_WARN = Colors.create(Colors.Ground.FORE, Colors.Color.RED);

    public static void err(final ExitValue exitCode, String message) {
        warn(message);
        System.exit(exitCode.ordinal());
    }

    public static void warn(final String message) {
        System.err.print(COLOR_WARN);
        System.err.print("SSC Transpiler: warning: " + message);
        System.err.println(Colors.COLOR_RESET);
    }
}

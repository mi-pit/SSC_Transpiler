package cz.muni.fi.sscc.exceptions;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    TRANSPILATION_FAIL,
    C_PREPROCESSING_FAIL,
    C_VERIFICATION_FAIL,
    C_COMPILATION_FAIL,
    IO_EXCEPTION,
    ;

    public static void err(final ExitValue exitCode, String message) {
        warn(message);
        System.exit(exitCode.ordinal());
    }

    public static void warn(final String message) {
        System.err.println("SSC Transpiler: " + message);
    }
}

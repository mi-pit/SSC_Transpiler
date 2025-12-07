package cz.muni.fi.sscc;

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
        System.err.println("SSC Transpiler: " + message + ": " + exitCode.name().toLowerCase().replace("_", " "));
        System.exit(exitCode.ordinal());
    }
}

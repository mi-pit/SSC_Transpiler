package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.UnixTerminalColors;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    TRANSPILATION_FAIL,
    C_PREPROCESSING_FAIL,
    C_VERIFICATION_FAIL,
    C_COMPILATION_FAIL,
    IO_EXCEPTION,
    ;

    private static final String COLOR_WARN = UnixTerminalColors.create(UnixTerminalColors.Ground.FORE, UnixTerminalColors.Color.RED);

    public static void err(final ExitValue exitCode, String message) {
        warnInternal(true, message);
        warnInternalFmt(true, "Exiting with code %d (%s)", exitCode.ordinal(), exitCode.toString());
        System.exit(exitCode.ordinal());
    }

    public static void warn(final String message) {
        warnInternal(false, message);
    }

    private static void warnInternal(final boolean isError, final String message) {
        warnInternalFmt(isError, "%s", message);
    }

    private static void warnInternalFmt(final boolean isError, final String fmtstr, Object... args) {
        System.err.print(COLOR_WARN);

        final String warningString = isError ? "error" : "warning";
        System.err.print("SSC Transpiler: " + warningString + ": ");
        System.out.printf("%s", String.format(fmtstr, args));

        System.err.println(UnixTerminalColors.COLOR_RESET);
    }
}

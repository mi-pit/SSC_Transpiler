package cz.mipit.sscc.util;

import cz.mipit.sscc.util.color.ConsoleColor;

import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.create;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    TRANSPILATION_FAIL,
    C_PREPROCESSING_FAIL,
    C_VERIFICATION_FAIL,
    C_COMPILATION_FAIL,
    IO_EXCEPTION,
    ;


    private static final ConsoleColor COLOR_WARN = create(Ground.BACK, Color.YELLOW);
    private static final ConsoleColor COLOR_ERROR = create(Ground.FORE, Color.RED);


    /* TODO: replace with formatted exceptions */
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
        final ConsoleColor color = isError ? COLOR_ERROR : COLOR_WARN;
        color.setConsoleColor();

        final String warningString = isError ? "error" : "warning";
        System.err.print("SSC Transpiler: " + warningString + ": ");
        System.out.printf("%s", String.format(fmtstr, args));

        COLOR_DEFAULT.setConsoleColor();
    }

    @Override
    public String toString() {
        return super.toString().replaceAll("_", " ");
    }
}

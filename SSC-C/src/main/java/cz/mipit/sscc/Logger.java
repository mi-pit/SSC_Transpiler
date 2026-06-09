package cz.mipit.sscc;

import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.color.ConsoleColor;

import java.io.PrintStream;
import java.util.function.Supplier;

import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.create;

public final class Logger {
    public static final ConsoleColor COLOR_WARN = create(Ground.FORE, Color.YELLOW);
    public static final ConsoleColor COLOR_ERROR = create(Ground.FORE, Color.RED);

    public static final ConsoleColor DEBUG_COLOR = create(Ground.FORE, Color.MAGENTA);
    public static final ConsoleColor VERBOSE_COLOR = create(Ground.FORE, Color.YELLOW);


    private SSCCOptions options;

    public Logger(SSCCOptions opts) {
        options = opts;
    }

    public Logger() {
        this(SSCCOptions.newWithDefaults());
    }

    public void setOptions(final SSCCOptions options) {
        this.options = options;
    }


    public static void errExit(final ExitValue exitCode, String fmt, Object... args) {
        errReturn(exitCode, fmt, args);
        errReturn(exitCode, "Exiting with code %d", exitCode.ordinal());
        System.exit(exitCode.ordinal());
    }

    public static void errExit(final ExitValue exitCode, String message) {
        errExit(exitCode, "%s", message);
    }

    public static void warn(final String message, final Object... args) {
        _log(COLOR_WARN, "warning", System.err, message, args);
    }

    public static void warn(final String message) {
        warn("%s", message);
    }

    public static void info(final String message) {
        info("%s", message);
    }

    public static void info(final String format, final Object... args) {
        _log(COLOR_DEFAULT, "info", System.out, format, args);
    }

    public static ExitValue errReturn(final ExitValue exitValue, String fmt, Object... args) {
        _log(COLOR_ERROR, "error: " + exitValue.humanReadable(), System.err, fmt, args);
        return exitValue;
    }

    public static ExitValue errReturn(final ExitValue exitValue, String message) {
        return errReturn(exitValue, "%s", message);
    }

    synchronized private static void _log(final ConsoleColor color,
                                          final String typeString,
                                          final PrintStream stream,
                                          final String fmtstr,
                                          Object... args) {
        color.printf(stream, Main.SSCC_NAME + ": " + typeString + ": " + fmtstr, args);
        stream.println();
    }

    public void printDebug(String message) {
        printDebug("%s", message);
    }

    public void printDebug(Supplier<String> supplier) {
        printDebug("%s", supplier.get());
    }

    synchronized public void printDebug(String fmt, Object... objects) {
        if (!options.debug()) {
            return;
        }
        DEBUG_COLOR.print("[DEBUG] ");
        DEBUG_COLOR.printf(fmt, objects);
        System.out.println();
    }

    synchronized public void printVerbose(String fmt, Object... objects) {
        if (!options.verbose()) {
            return;
        }
        VERBOSE_COLOR.printf(fmt, objects);
        System.out.println();
    }

    public void printVerboseFilename(String message, String fileName) {
        printVerbose("%s: '" + COLOR_DEFAULT + "%s" + VERBOSE_COLOR + "'", message, fileName);
    }

    synchronized private void printException(final boolean isUnexpected, Throwable e) {
        System.out.flush();

        System.err.print(Main.SSCC_NAME + ": ");

        if (isUnexpected) {
            System.err.print("Unexpected exception caught: ");
        }

        if (isUnexpected && (options.debug() || options.verbose())) {
            e.printStackTrace(System.err);
            return;
        }

        System.err.print(e.getMessage());

        if (isUnexpected && !options.verbose()) {
            System.err.print(" (for stack trace, run with `" + SSCCOptions.OPTSTR_VERBOSE_SHORT + "` option)");
        }

        System.err.println();
        System.err.flush();
    }

    /**
     * Prints an error message to {@code System.err}.
     * If ran with the debug option, this method also prints the stack trace.
     *
     * @param e Any valid exception
     */
    public void printException(Throwable e) {
        printException(true, e);
    }

    public void printException(SSCTranspilerException e) {
        printException(false, e);
    }
}

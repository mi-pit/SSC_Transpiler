package cz.mipit.sscc;

import cz.mipit.sscc.args.SSCCOptions;
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

    public static void err(final ExitValue exitCode, String fmt, Object... args) {
        log(COLOR_ERROR, "error", System.err, fmt, args);
        log(COLOR_ERROR, "error", System.err,
                "Exiting with code %d (%s)", exitCode.ordinal(), exitCode.toString());
        System.exit(exitCode.ordinal());
    }

    public static void err(final ExitValue exitCode, String message) {
        err(exitCode, "%s", message);
    }

    public static void warn(final String message, final Object... args) {
        log(COLOR_WARN, "warning", System.err, message, args);
    }

    public static void warn(final String message) {
        warn("%s", message);
    }

    public static void info(final String message) {
        info("%s", message);
    }

    public static void info(final String format, final Object... args) {
        log(COLOR_DEFAULT, "info", System.out, format, args);
    }

    public static ExitValue errNoExit(final ExitValue exitValue, String message) {
        log(COLOR_ERROR, exitValue.toString(), System.err, "%s", message);
        return exitValue;
    }

    private static void log(final ConsoleColor color,
                            final String typeString,
                            final PrintStream stream,
                            final String fmtstr,
                            Object... args) {
        color.printf(stream, Main.SSCC_NAME + ": " + typeString + ": " + fmtstr, args);
        stream.println();
    }


    private SSCCOptions options;

    public Logger(SSCCOptions opts) {
        options = opts;
    }

    public Logger() {
        this(SSCCOptions.DEFAULT);
    }

    public void setOptions(final SSCCOptions options) {
        this.options = options;
    }


    private static final ConsoleColor DEBUG_COLOR = create(
            Ground.FORE,
            Color.MAGENTA
    );
    private static final ConsoleColor VERBOSE_COLOR = create(
            Ground.FORE,
            Color.YELLOW
    );

    public void printDebug(String message) {
        printDebug("%s", message);
    }

    public void printDebug(Supplier<String> supplier) {
        printDebug("%s", supplier.get());
    }

    public void printDebug(String fmt, Object... objects) {
        if (!options.debug()) {
            return;
        }
        DEBUG_COLOR.print("[DEBUG] ");
        DEBUG_COLOR.printf(fmt, objects);
        System.out.println();
    }

    public void printVerbose(String fmt, Object... objects) {
        if (!options.verbose()) {
            return;
        }
        VERBOSE_COLOR.printf(fmt, objects);
        System.out.println();
    }

    public void printVerbose(Supplier<String> supplier) {
        printVerbose("%s", supplier.get());
    }

    public void printVerboseFilename(String a, String b) {
        printVerbose("%s'" + COLOR_DEFAULT + "%s" + VERBOSE_COLOR + "'", a, b);
    }
}

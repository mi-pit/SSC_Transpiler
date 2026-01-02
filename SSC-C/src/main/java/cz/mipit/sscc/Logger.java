package cz.mipit.sscc;

import cz.mipit.sscc.args.Options;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.color.ConsoleColor;

import java.io.PrintStream;

import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.create;

public final class Logger {
    public static final ConsoleColor COLOR_WARN = create(Ground.BACK, Color.YELLOW);
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
        log(COLOR_WARN, "warn", System.err, message, args);
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

    private static void log(final ConsoleColor color,
                            final String typeString,
                            final PrintStream stream,
                            final String fmtstr,
                            Object... args) {
        color.printf(stream, "SSC Transpiler: " + typeString + ": " + fmtstr, args);
    }


    private Options options;

    public Logger(Options opts) {
        options = opts;
    }

    public Logger() {
        this(Options.DEFAULT);
    }

    public void setOptions(final Options options) {
        this.options = options;
    }

    public void printDebug(String string) {
        printDebug("%s", string);
    }

    private static final ConsoleColor DEBUG_COLOR = create(
            Ground.FORE,
            Color.MAGENTA
    );
    private static final ConsoleColor VERBOSE_COLOR = create(
            Ground.FORE,
            Color.YELLOW
    );

    public void printDebug(String fmt, Object... objects) {
        if (!options.debug()) {
            return;
        }
        DEBUG_COLOR.printf(fmt, objects);
    }

    public void printVerbose(String fmt, Object... objects) {
        if (!options.verbose()) {
            return;
        }
        VERBOSE_COLOR.printf(fmt, objects);
    }

    public void printVerbose(String string) {
        printVerbose("%s", (Object) string);
    }

    public void printVerbose(String a, String b) {
        printVerbose("%s'" + COLOR_DEFAULT + "%s" + VERBOSE_COLOR + "'", a, b);
    }
}

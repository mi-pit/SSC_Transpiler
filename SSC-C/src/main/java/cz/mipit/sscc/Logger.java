package cz.mipit.sscc;

import cz.mipit.sscc.args.Options;
import cz.mipit.sscc.util.UnixTerminalColors;

public final class Logger {
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

    private static final String DEBUG_COLOR = UnixTerminalColors.create(
            UnixTerminalColors.Ground.FORE,
            UnixTerminalColors.Color.MAGENTA
    );
    private static final String VERBOSE_COLOR = UnixTerminalColors.create(
            UnixTerminalColors.Ground.FORE,
            UnixTerminalColors.Color.YELLOW
    );

    public void printDebug(String fmt, Object... objects) {
        if (!options.debug()) {
            return;
        }
        System.out.print(DEBUG_COLOR + "[DEBUG] ");
        System.out.printf(fmt, objects);
        System.out.println(UnixTerminalColors.COLOR_RESET);
    }

    public void printVerbose(String fmt, Object... objects) {
        if (!options.verbose()) {
            return;
        }
        System.out.print(VERBOSE_COLOR);
        System.out.printf(fmt, objects);
        System.out.println(UnixTerminalColors.COLOR_RESET);
    }

    public void printVerbose(String string) {
        printVerbose("%s", (Object) string);
    }

    public void printVerbose(String a, String b) {
        printVerbose("%s'" + UnixTerminalColors.COLOR_RESET + "%s" + VERBOSE_COLOR + "'", a, b);
    }
}

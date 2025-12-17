package cz.mipit.sscc;

import cz.mipit.sscc.args.CommandLineArguments;
import cz.mipit.sscc.util.UnixTerminalColors;

public final class Logger {
    private final CommandLineArguments args;

    public Logger(CommandLineArguments args) {
        this.args = args;
    }

    public void printDebug(String string) {
        printDebug("%s", string);
    }

    private static final String DEBUG_COLOR = UnixTerminalColors.create(UnixTerminalColors.Ground.FORE, UnixTerminalColors.Color.MAGENTA);
    private static final String VERBOSE_COLOR = UnixTerminalColors.create(UnixTerminalColors.Ground.FORE, UnixTerminalColors.Color.YELLOW);

    public void printDebug(String fmt, Object... objects) {
        if (args.isPrintDebug()) {
            System.out.print(DEBUG_COLOR + "[DEBUG] ");
            System.out.printf(fmt, objects);
            System.out.println(UnixTerminalColors.COLOR_RESET);
        }
    }

    public void printVerbose(String fmt, Object... objects) {
        if (args.isVerbose()) {
            System.out.print(VERBOSE_COLOR);
            System.out.printf(fmt, objects);
            System.out.println(UnixTerminalColors.COLOR_RESET);
        }
    }

    public void printVerbose(String string) {
        printVerbose("%s", string);
    }
}

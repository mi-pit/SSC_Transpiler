package cz.muni.fi.sscc;

import cz.muni.fi.sscc.args.CommandLineArguments;

public final class Logger {
    private final CommandLineArguments args;

    public Logger(CommandLineArguments args) {
        this.args = args;
    }

    public void printDebug(String string) {
        printDebug("%s\n", string);
    }

    public void printDebug(String fmt, Object... objects) {
        if (args.isPrintDebug()) {
            System.out.print("[DEBUG] ");
            System.out.printf(fmt, objects);
        }
    }

    public void printVerbose(String fmt, Object... objects) {
        if (args.isVerbose()) {
            System.out.printf(fmt, objects);
            System.out.println();
        }
    }

    public void printVerbose(String string) {
        printVerbose("%s", string);
    }
}

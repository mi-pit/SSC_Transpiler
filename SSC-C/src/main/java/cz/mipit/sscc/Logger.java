package cz.mipit.sscc;

import cz.mipit.sscc.args.Options;
import cz.mipit.sscc.util.color.ConsoleColor;

import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.create;

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
        System.out.print(DEBUG_COLOR + "[DEBUG] ");
        System.out.printf(fmt, objects);
        System.out.println(COLOR_DEFAULT);
    }

    public void printVerbose(String fmt, Object... objects) {
        if (!options.verbose()) {
            return;
        }
        System.out.print(VERBOSE_COLOR);
        System.out.printf(fmt, objects);
        System.out.println(COLOR_DEFAULT);
    }

    public void printVerbose(String string) {
        printVerbose("%s", (Object) string);
    }

    public void printVerbose(String a, String b) {
        printVerbose("%s'" + COLOR_DEFAULT + "%s" + VERBOSE_COLOR + "'", a, b);
    }
}

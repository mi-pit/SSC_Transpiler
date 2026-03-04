package cz.mipit.sscc.util.color;

import java.io.PrintStream;

/**
 * Does jack
 */
final class UnsupportedConsoleColor extends ConsoleColor {
    static final UnsupportedConsoleColor DEFAULT = new UnsupportedConsoleColor();

    private UnsupportedConsoleColor() {
    }

    public static UnsupportedConsoleColor create(ConsoleColorFactory.Ground ignoredG, ConsoleColorFactory.Color ignoredC) {
        return DEFAULT;
    }

    @Override
    public void setConsoleColor(PrintStream stream) {
    }

    @Override
    public String toString() {
        return "";
    }
}

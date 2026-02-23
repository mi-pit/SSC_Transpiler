package cz.mipit.sscc.util.color;

import java.io.PrintStream;
import java.util.Locale;
import java.util.function.BiFunction;

public abstract class ConsoleColorFactory {
    private static final ConsoleColorFactory UNIX = new ConsoleColorFactory() {
        @Override
        protected BiFunction<Ground, Color, ConsoleColor> getFactory() {
            return UnixTerminalColor::new;
        }

        @Override
        protected ConsoleColor defaultColor() {
            return UnixTerminalColor.DEFAULT;
        }
    };

    private static final ConsoleColorFactory OTHER = new ConsoleColorFactory() {
        @Override
        protected BiFunction<Ground, Color, ConsoleColor> getFactory() {
            return UnsupportedConsoleColor::create;
        }

        @Override
        protected ConsoleColor defaultColor() {
            return UnsupportedConsoleColor.DEFAULT;
        }
    };

    /// TODO?
    private static final ConsoleColorFactory WINDOWS = OTHER;


    abstract protected BiFunction<Ground, Color, ConsoleColor> getFactory();

    abstract protected ConsoleColor defaultColor();


    private static final ConsoleColorFactory FROM_OS;

    static {
        final String lowercase = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);

        if (lowercase.contains("win")) {
            FROM_OS = WINDOWS;
        } else if (lowercase.contains("mac") || lowercase.contains("linux")) {
            FROM_OS = UNIX;
        } else {
            FROM_OS = OTHER;
        }
    }

    public static final ConsoleColor COLOR_DEFAULT = FROM_OS.defaultColor();

    public static ConsoleColor create(final ConsoleColorFactory factory,
                                      final Ground ground,
                                      final Color color) {
        return factory.getFactory().apply(ground, color);
    }

    public static ConsoleColor create(final Ground ground, final Color color) {
        return create(FROM_OS, ground, color);
    }

    public enum Ground {
        FORE,
        BACK,
    }

    public enum Color {
        BLACK,
        RED,
        GREEN,
        YELLOW,
        BLUE,
        MAGENTA,
        CYAN,
        WHITE,
    }


    /**
     * Does jack
     */
    private static final class UnsupportedConsoleColor extends ConsoleColor {
        private static final UnsupportedConsoleColor DEFAULT = new UnsupportedConsoleColor();

        private UnsupportedConsoleColor() {
        }

        @Override
        public void setConsoleColor(PrintStream stream) {
        }

        @Override
        public String toString() {
            return "";
        }

        private static UnsupportedConsoleColor create(Ground ignoredG, Color ignoredC) {
            return DEFAULT;
        }
    }
}

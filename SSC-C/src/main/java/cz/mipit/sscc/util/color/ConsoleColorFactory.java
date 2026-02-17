package cz.mipit.sscc.util.color;

import java.io.PrintStream;
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


    protected abstract BiFunction<Ground, Color, ConsoleColor> getFactory();

    protected abstract ConsoleColor defaultColor();


    public static final ConsoleColor COLOR_DEFAULT = fromOS().defaultColor();

    public static ConsoleColor create(final ConsoleColorFactory factory,
                                      final Ground ground,
                                      final Color color) {
        return factory.getFactory().apply(ground, color);
    }

    public static ConsoleColor create(final Ground ground, final Color color) {
        return create(fromOS(), ground, color);
    }

    private static ConsoleColorFactory fromOS() {
        final String property = System.getProperty("os.name");
        final String lowercase = property.toLowerCase();

        if (property.toLowerCase().contains("windows")) {
            return WINDOWS;
        } else if (lowercase.contains("mac") || lowercase.contains("linux")) {
            return UNIX;
        } else {
            return OTHER;
        }
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

package cz.mipit.sscc.util.color;

import java.util.function.BiFunction;

public enum ConsoleColorFactory {
    UNIX {
        @Override
        protected BiFunction<Ground, Color, ConsoleColor> getFactory() {
            return UnixTerminalColor::new;
        }

        @Override
        public ConsoleColor defaultColor() {
            return UnixTerminalColor.DEFAULT;
        }
    },
    WINDOWS {
        @Override
        protected BiFunction<Ground, Color, ConsoleColor> getFactory() {
            return UnsupportedConsoleColor::create;
        }

        @Override
        public ConsoleColor defaultColor() {
            return UnsupportedConsoleColor.DEFAULT;
        }
    };


    protected abstract BiFunction<Ground, Color, ConsoleColor> getFactory();

    public abstract ConsoleColor defaultColor();

    public static ConsoleColor getDefault(final ConsoleColorFactory factory) {
        return factory.defaultColor();
    }

    public static final ConsoleColor COLOR_DEFAULT = getDefault(fromOS());

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

        if (property.toLowerCase().contains("windows")) {
            return WINDOWS;
        } else {
            return UNIX;
        }
    }

    public enum Ground {FORE, BACK}

    public enum Color {BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE}


    /**
     * Does jack
     */
    private static class UnsupportedConsoleColor extends ConsoleColor {
        /**
         * Only one object, since it doesn't do anything
         */
        private static final UnsupportedConsoleColor DEFAULT = new UnsupportedConsoleColor();

        private UnsupportedConsoleColor() {
        }

        /**
         * Does nothing
         */
        @Override
        public void setConsoleColor() {
        }

        /**
         * @return empty string
         */
        @Override
        public String toString() {
            return "";
        }

        private static UnsupportedConsoleColor create(Ground ignoredG, Color ignoredC) {
            return DEFAULT;
        }
    }
}

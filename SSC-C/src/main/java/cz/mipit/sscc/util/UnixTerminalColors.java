package cz.mipit.sscc.util;

public final class UnixTerminalColors {
    private UnixTerminalColors() {
    }

    public enum Ground {
        FORE, BACK;

        public String getCode() {
            return String.valueOf(ordinal() + 3);
        }
    }

    public enum Color {
        BLACK, RED, GREEN, YELLOW, BLUE, MAGENTA, CYAN, WHITE;

        public String getCode() {
            return String.valueOf(ordinal());
        }
    }

    public static String create(Ground ground, Color color) {
        return "\u001B[" + ground.getCode() + color.getCode() + "m";
    }

    public static final String COLOR_RESET = "\u001B[0m";
}

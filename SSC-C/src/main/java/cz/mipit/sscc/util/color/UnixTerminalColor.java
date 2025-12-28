package cz.mipit.sscc.util.color;

import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;

public class UnixTerminalColor extends ConsoleColor {
    private static final String RESET = "\u001B[0m";
    public static final UnixTerminalColor DEFAULT = new UnixTerminalColor(RESET);

    private final String repre;

    protected UnixTerminalColor(String repre) {
        this.repre = repre;
    }

    protected UnixTerminalColor(Ground ground, Color color) {
        this(createString(ground, color));
    }

    @Override
    public String toString() {
        return repre;
    }

    private static String createString(Ground ground, Color color) {
        return "\u001B[" + code(ground) + code(color) + "m";
    }

    @Override
    public void setConsoleColor() {
        System.out.print(repre);
    }

    private static String code(Ground ground) {
        return String.valueOf(ground.ordinal() + 3);
    }

    private static String code(Color color) {
        return String.valueOf(color.ordinal());
    }
}

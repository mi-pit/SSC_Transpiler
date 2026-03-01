package cz.mipit.sscc.util.color;

import java.io.PrintStream;

import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;

public class UnixTerminalColor extends ConsoleColor {
    private static final String RESET = "\u001B[0m";
    public static final UnixTerminalColor DEFAULT = new UnixTerminalColor(RESET);

    private static final String SEQUENCE_START = "\u001B[";
    private static final String SEQUENCE_END = "m";


    private final String repre;

    public UnixTerminalColor(String repre) {
        this.repre = repre;
    }

    UnixTerminalColor(Ground ground, Color color) {
        this(createString(ground, color));
    }

    @Override
    public String toString() {
        return repre;
    }

    private static String createString(Ground ground, Color color) {
        return SEQUENCE_START + code(ground) + code(color) + SEQUENCE_END;
    }

    @Override
    public void setConsoleColor(PrintStream stream) {
        stream.print(repre);
    }


    private static String code(Ground ground) {
        return String.valueOf(ground.ordinal() + 3);
    }

    private static String code(Color color) {
        return String.valueOf(color.ordinal());
    }
}

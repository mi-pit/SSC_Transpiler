package cz.mipit.sscc.util.color;

import java.io.PrintStream;

/**
 * An OS-agnostic interface for setting the color of ({@code System.out.print(...)}).
 * <p>
 * These objects must have a well-defined {@link ConsoleColor#toString()} method, even if not applicable
 * (see {@link ConsoleColorFactory.UnsupportedConsoleColor}).
 */
public abstract class ConsoleColor {
    /**
     * Sets the color of the console to the color represented by this object.
     */
    final public void setConsoleColor() {
        setConsoleColor(System.out);
    }

    abstract public void setConsoleColor(PrintStream stream);


    public void printf(PrintStream stream, String format, Object... args) {
        setConsoleColor();
        stream.printf(format, args);
        ConsoleColorFactory.COLOR_DEFAULT.setConsoleColor();
    }

    final public void printf(String format, Object... args) {
        printf(System.out, format, args);
    }

    final public void print(PrintStream stream, String message) {
        printf(stream, "%s", message);
    }

    final public void print(String message) {
        print(System.out, message);
    }

    final public void println(PrintStream stream, String message) {
        print(stream, message);
        stream.println();
    }

    final public void println(String message) {
        println(System.out, message);
    }


    /**
     * Made abstract so that an inheritor HAS to think about the implementation.
     * <p>
     * If the color is impossible to be set this way, this method should return an empty string or similar
     * </p>
     *
     * @see ConsoleColorFactory.UnsupportedConsoleColor
     */
    abstract public String toString();
}

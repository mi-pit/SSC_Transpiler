package cz.mipit.sscc.util.color;

import java.io.PrintStream;

/**
 * An OS-agnostic interface for setting the color of the console/terminal ({@code System.out.print(...)}).
 * <p>
 * These objects must have a well-defined {@link ConsoleColor#toString()} method, even if not applicable
 * (see {@link UnsupportedConsoleColor}).
 */
public interface ConsoleColor {
    /**
     * Inheritor has to think about the implementation.
     * <p>
     * If the color is impossible to be set this way, this method should return an empty string or similar
     * </p>
     */
    String toString();

    void setConsoleColor(PrintStream stream);

    /**
     * Sets the color of the console to the color represented by this object.
     */
    default void setConsoleColor() {
        setConsoleColor(System.out);
    }


    default void printf(PrintStream stream, String format, Object... args) {
        synchronized (this) {
            this.setConsoleColor();
            stream.printf(format, args);
            ConsoleColorFactory.COLOR_DEFAULT.setConsoleColor();
        }
    }

    default void printf(String format, Object... args) {
        printf(System.out, format, args);
    }

    default void print(PrintStream stream, String message) {
        printf(stream, "%s", message);
    }

    default void print(String message) {
        print(System.out, message);
    }

    default void println(PrintStream stream, String message) {
        print(stream, message);
        stream.println();
    }

    default void println(String message) {
        println(System.out, message);
    }
}

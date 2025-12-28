package cz.mipit.sscc.util.color;

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
    abstract public void setConsoleColor();

    /**
     * Made abstract so that an inheritor HAS to think about the implementation.
     * {@link ConsoleColorFactory.UnsupportedConsoleColor#toString()}
     */
    abstract public String toString();
}

package cz.mipit.sscc.ssc.exceptions.data;

import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;
import cz.mipit.sscc.util.color.ConsoleColorFactory;

import java.util.List;

import static java.lang.System.lineSeparator;

public record ErrorMessage(
        @Nullable String message,
        @NotNull ErrorContext context,
        @Nullable Locator locator
) {
    @Override
    public String toString() {
        return "    "
               + message
               + ConsoleColorFactory.COLOR_DEFAULT
               + lineSeparator()
               + context
               + lineSeparator()
               + locator
               + lineSeparator();
    }

    public static ErrorMessage fromLines(
            String message,
            List<EnumeratedLine> lines,
            Locator locator
    ) {
        return new ErrorMessage(message, new ErrorContext(lines), locator);
    }
}

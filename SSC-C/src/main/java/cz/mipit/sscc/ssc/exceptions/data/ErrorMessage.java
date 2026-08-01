package cz.mipit.sscc.ssc.exceptions.data;

import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;
import cz.mipit.sscc.util.color.ConsoleColor;
import cz.mipit.sscc.util.color.ConsoleColorFactory;

import java.util.Objects;

import static cz.mipit.sscc.util.SSCCUtil.Text.INDENT;
import static java.lang.System.lineSeparator;

public final class ErrorMessage {
    private final String message;
    private final ErrorContext context;
    private final Locator locator;

    private final ConsoleColor messageColor;

    private static final ConsoleColor FILENAME_COLOR = ConsoleColorFactory.COLOR_DEFAULT;


    private ErrorMessage(
            @Nullable String message,
            @NotNull ErrorContext context,
            @Nullable Locator locator,
            @NotNull ConsoleColor messageColor
    ) {
        this.message = message;
        this.context = Objects.requireNonNull(context);
        this.locator = locator;
        this.messageColor = Objects.requireNonNull(messageColor);
    }

    @Override
    public String toString() {
        return messageColor
               + INDENT
               + "in file '" + FILENAME_COLOR + context.filename() + messageColor + "'"
               + lineSeparator()
               + INDENT
               + message
               + ConsoleColorFactory.COLOR_DEFAULT
               + lineSeparator()
               + context
               + lineSeparator()
               + locator
               + lineSeparator();
    }

    public static ErrorMessage fromErrorContext(
            String message,
            ErrorContext errorContext,
            Locator locator,
            ConsoleColor messageColor
    ) {
        return new ErrorMessage(message, errorContext, locator, messageColor);
    }
}

package cz.mipit.sscc.ssc.exceptions.data;

import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;
import cz.mipit.sscc.util.color.ConsoleColorFactory;

import java.util.Objects;

import static cz.mipit.sscc.util.SSCCUtil.Text.INDENT;
import static java.lang.System.lineSeparator;

public final class ErrorMessage {
    private final String message;
    private final ErrorContext context;
    private final Locator locator;


    private ErrorMessage(
            @Nullable String message,
            @NotNull ErrorContext context,
            @Nullable Locator locator
    ) {
        this.message = message;
        this.context = Objects.requireNonNull(context);
        this.locator = locator;
    }

    @Override
    public String toString() {
        return INDENT
               + "in file '" + context.filename() + "'"
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
            Locator locator
    ) {
        return new ErrorMessage(message, errorContext, locator);
    }
}

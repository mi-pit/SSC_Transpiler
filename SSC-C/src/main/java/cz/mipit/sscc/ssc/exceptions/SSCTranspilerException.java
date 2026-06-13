package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.ssc.exceptions.data.ErrorContext;
import cz.mipit.sscc.ssc.exceptions.data.ErrorMessage;
import cz.mipit.sscc.ssc.exceptions.data.Locator;
import cz.mipit.sscc.util.collection.Enumerated;
import cz.mipit.sscc.util.collection.Enumerator;
import cz.mipit.sscc.util.color.ConsoleColor;
import cz.mipit.sscc.util.color.ConsoleColorFactory;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

import static cz.mipit.sscc.ssc.exceptions.data.ErrorContext.getApparentContext;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

public class SSCTranspilerException extends RuntimeException {
    public static final int LINES_BEFORE = 4;
    public static final int LINES_AFTER = 0;

    protected static final ConsoleColor COLOR_FATAL = ConsoleColorFactory.create(Ground.FORE, Color.RED);
    protected static final ConsoleColor COLOR_ANTLR = COLOR_FATAL;

    protected static final ConsoleColor COLOR_WARNING = ConsoleColorFactory.create(Ground.FORE, Color.YELLOW);


    public static final String LINENO_SEPARATOR = " | ";

    private static final int LINES_BEFORE_PREV = 0;
    private static final int LINES_AFTER_PREV = 0;

    private final Type type;
    private final List<ErrorMessage> errorMessages;

    /* Base constructor */
    private SSCTranspilerException(
            Type type,
            List<ErrorMessage> errorMessages
    ) {
        this.type = requireNonNull(type);

        this.errorMessages = requireNonNull(errorMessages);
    }

    private SSCTranspilerException(
            Type type,
            ErrorMessage errorMessage
    ) {
        this(type, List.of(errorMessage));
    }

    private SSCTranspilerException(
            final Type type,
            final String message,
            final ErrorContext errorContext,
            final ParseTree offendingCtx,
            final CommonTokenStream tokens
    ) {
        this(
                type,
                ErrorMessage.fromErrorContext(
                        message,
                        errorContext,
                        new Locator(offendingCtx, tokens, errorContext.lastLineNumber())
                )
        );
    }

    protected SSCTranspilerException(
            Type type,
            String message,
            ParseTree offendingCtx,
            CommonTokenStream tokens
    ) {

        this(
                type,
                message,
                getApparentContext(
                        requireNonNull(offendingCtx, "Context"),
                        requireNonNull(tokens, "Token stream"),
                        LINES_BEFORE,
                        LINES_AFTER
                ),
                offendingCtx,
                tokens
        );
    }

    public SSCTranspilerException(
            Type type,
            String message,
            ErrorContext apparentContext,
            Token offendingToken
    ) {
        this(
                type,
                ErrorMessage.fromErrorContext(
                        message,
                        apparentContext,
                        new Locator(offendingToken, apparentContext.lastLineNumber())
                )
        );
    }


    protected SSCTranspilerException(
            Type type,
            String message,
            Token offendingToken,
            CommonTokenStream tokens
    ) {
        this(
                type,
                message,
                getApparentContext(
                        offendingToken,
                        tokens,
                        LINES_BEFORE,
                        LINES_AFTER
                ),
                offendingToken
        );
    }

    protected SSCTranspilerException(
            Type type,
            String message, List<ParseTree> offenders,
            CommonTokenStream tokens
    ) {
        this(
                type,
                getErrorMessages(message, offenders, tokens)
        );
    }

    public static List<ErrorMessage> getErrorMessages(
            final String message,
            List<ParseTree> offenders,
            CommonTokenStream tokens
    ) {
        final List<ErrorMessage> list = new ArrayList<>();
        for (final Enumerated<ParseTree> offender : Enumerator.of(offenders)) {
            final int linesBefore = offender.index() == 0 ? LINES_BEFORE : LINES_BEFORE_PREV;
            final int linesAfter = offender.index() == 0 ? LINES_AFTER : LINES_AFTER_PREV;
            final String actualMessage = offender.index() == 0 ? message : "Previous definition here:";

            final ErrorContext ctx = getApparentContext(
                    offender.item(),
                    tokens,
                    linesBefore,
                    linesAfter
            );

            final ErrorMessage errorMessage = ErrorMessage.fromErrorContext(
                    actualMessage,
                    ctx,
                    new Locator(offender.item(), tokens, ctx.lastLineNumber())
            );
            list.add(errorMessage);
        }
        return list;
    }


    @Override
    public String getMessage() {
        return createMessage(type, errorMessages);
    }


    public static String createMessage(
            final Type type,
            final List<ErrorMessage> errorMessages
    ) {
        final ConsoleColor color = type.toColor();

        final StringBuilder sBuilder = new StringBuilder(color.toString());
        sBuilder
                .append(type.humanReadableName())
                .append(color)
                .append(":")
                .append(lineSeparator());

        for (final ErrorMessage errorMessage : errorMessages) {
            sBuilder.append(errorMessage);
        }

        sBuilder.append(COLOR_DEFAULT);

        return sBuilder.toString();
    }


    public enum Type {
        Language,
        Antlr_parser,
        Warning,
        ;

        public final ConsoleColor toColor() {
            return switch (this) {
                case Language -> COLOR_FATAL;
                case Antlr_parser -> COLOR_ANTLR;
                case Warning -> COLOR_WARNING;
            };
        }

        public final String humanReadableName() {
            final String replaced = name().replace('_', ' ');
            if (this == Warning) {
                return replaced;
            }

            return replaced + " exception";
        }
    }
}

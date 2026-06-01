package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.File;
import cz.mipit.sscc.ssc.exceptions.data.EnumeratedLine;
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

import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

public class SSCTranspilerException extends RuntimeException {
    public static final int LINES_BEFORE = 4;
    public static final int LINES_AFTER = 0;

    protected static final ConsoleColor COLOR_FATAL = ConsoleColorFactory.create(Ground.FORE, Color.RED);
    protected static final ConsoleColor COLOR_ANTLR = ConsoleColorFactory.create(Ground.FORE, Color.RED);

    public static final String LINENO_SEPARATOR = " | ";


    private final File currentFile;
    private final Type type;
    private final List<ErrorMessage> errorMessages;

    /* Base constructor */
    private SSCTranspilerException(
            Type type,
            List<ErrorMessage> errorMessages,
            File currentFile
    ) {
        this.type = requireNonNull(type);

        this.errorMessages = errorMessages;

        this.currentFile = requireNonNull(currentFile);
    }

    private SSCTranspilerException(
            Type type,
            ErrorMessage errorMessage,
            File currentFile
    ) {
        this(type, List.of(errorMessage), currentFile);
    }

    protected SSCTranspilerException(
            Type type,
            String message, ParseTree offendingCtx,
            CommonTokenStream tokens, File currentFile
    ) {
        this(
                type,
                ErrorMessage.fromLines(
                        message,
                        EnumeratedLine.getLines(
                                requireNonNull(offendingCtx, "Context"),
                                requireNonNull(tokens, "Token stream"),
                                LINES_BEFORE, LINES_AFTER
                        ),
                        new Locator(offendingCtx)
                ),
                currentFile
        );
    }

    protected SSCTranspilerException(
            Type type,
            String message, Token offendingToken,
            CommonTokenStream tokens, File currentFile
    ) {
        this(
                type,
                ErrorMessage.fromLines(
                        message,
                        EnumeratedLine.getLines(
                                offendingToken,
                                tokens,
                                LINES_BEFORE,
                                LINES_AFTER
                        ),
                        new Locator(offendingToken)
                ),
                currentFile
        );
    }

    private record Rec(ParseTree node, String message) {
    }

    protected SSCTranspilerException(
            Type type,
            String message, List<ParseTree> offenders,
            CommonTokenStream tokens, File currentFile
    ) {
        this(
                type,
                getErrorMessages(message, offenders, tokens),
                currentFile
        );
    }

    private static List<ErrorMessage> getErrorMessages(
            final String message,
            List<ParseTree> offenders, CommonTokenStream tokens
    ) {
        final List<ErrorMessage> list = new ArrayList<>();
        for (final Enumerated<ParseTree> offender : new Enumerator<>(offenders)) {
            final int linesBefore = offender.index() == 0 ? LINES_BEFORE : 0;
            final int linesAfter = offender.index() == 0 ? LINES_AFTER : 0;
            final String actualMessage = offender.index() == 0 ? message : "Previous definition here:";

            final ErrorMessage errorMessage = ErrorMessage.fromLines(
                    actualMessage,
                    EnumeratedLine.getLines(offender.item(), tokens, linesBefore, linesAfter),
                    new Locator(offender.item())
            );
            list.add(errorMessage);
        }
        return list;
    }


    @Override
    public String getMessage() {
        final ConsoleColor color = type.toColor();

        final StringBuilder sBuilder = new StringBuilder(color.toString());
        sBuilder
                .append(Main.SSCC_NAME)
                .append(": ")
                .append(type.humanReadableName())
                .append(" exception while processing file '")
                .append(COLOR_DEFAULT)
                .append(currentFile.fullName())
                .append(color)
                .append("':")
                .append(lineSeparator());

        for (final ErrorMessage errorMessage : errorMessages) {
            sBuilder.append(errorMessage);
        }

        sBuilder.append(COLOR_DEFAULT);

        return sBuilder.toString();
    }


    protected enum Type {
        Language,
        Antlr_parser,
        ;

        public final ConsoleColor toColor() {
            return switch (this) {
                case Language -> COLOR_FATAL;
                case Antlr_parser -> COLOR_ANTLR;
            };
        }

        public final String humanReadableName() {
            return name().replace('_', ' ');
        }
    }
}

package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.util.EnumeratedLine;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;
import cz.mipit.sscc.util.color.ConsoleColor;
import cz.mipit.sscc.util.color.ConsoleColorFactory;
import cz.mipit.sscc.util.color.UnixTerminalColor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;

import static cz.mipit.sscc.util.SSCCUtil.Maths.digitsOf;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

public class SSCTranspilerException extends RuntimeException {
    protected static final int LINES_BEFORE = 4;
    protected static final int LINES_AFTER = 0;

    protected static final ConsoleColor COLOR_FATAL = ConsoleColorFactory.create(Ground.FORE, Color.RED);
    protected static final ConsoleColor COLOR_ANTLR = ConsoleColorFactory.create(Ground.FORE, Color.RED);

    protected static final ConsoleColor COLOR_CODE = ConsoleColorFactory.create(Ground.FORE, Color.WHITE);
    protected static final ConsoleColor COLOR_LOCATOR = ConsoleColorFactory.create(Ground.FORE, Color.CYAN);

    protected static final ConsoleColor COLOR_CODE_BOLD;

    static {
        if (ConsoleColorFactory.FROM_OS == ConsoleColorFactory.UNIX) {
            COLOR_CODE_BOLD = new UnixTerminalColor("\u001B[1m" + COLOR_CODE);
        } else {
            COLOR_CODE_BOLD = ConsoleColorFactory.FROM_OS.defaultColor();
        }
    }

    public static final String LINENO_SEPARATOR = " | ";


    private final InputFile currentFile;
    private final Type type;
    private final @Nullable String message;
    private final String context;
    private final @Nullable String locator;

    /* Base constructor */
    private SSCTranspilerException(Type type, String message,
                                   String context, String locator,
                                   InputFile currentFile) {
        this.type = requireNonNull(type);
        this.message = message;
        this.context = requireNonNull(context);
        this.locator = locator;
        this.currentFile = requireNonNull(currentFile);
    }

    protected SSCTranspilerException(Type type, String message,
                                     List<EnumeratedLine> lines,
                                     String locator, InputFile currentFile) {
        this(type, message, formatLines(lines), locator, currentFile);
    }

    protected SSCTranspilerException(Type type, String message,
                                     ParserRuleContext offendingCtx, CommonTokenStream tokens,
                                     InputFile currentFile) {
        this(type, message, getLinesFromCtx(
                        requireNonNull(offendingCtx, "Context"),
                        requireNonNull(tokens, "Token stream")),
                getLocator(offendingCtx),
                currentFile);
    }

    protected SSCTranspilerException(Type type, String message, Token offendingToken,
                                     CommonTokenStream tokens, InputFile currentFile) {
        this(type,
                message,
                getLinesFromToken(offendingToken, tokens),
                getLocator(offendingToken),
                currentFile);
    }

    protected static String formatLines(final List<EnumeratedLine> lines) {
        final StringBuilder sBuilder = new StringBuilder(256)
                .append(COLOR_CODE_BOLD);

        final int fst = lines.getFirst().lineNumber();
        final int last = lines.getLast().lineNumber();
        final String fmtstr = "%" + getLineNumberLength(fst, last) + "d";

        for (int i = 0; i < lines.size(); i++) {
            final EnumeratedLine line = lines.get(i);

            final String formatted = String.format(fmtstr, line.lineNumber());
            sBuilder.append(formatted)
                    .append(LINENO_SEPARATOR)
                    .append(line.line());

            if (i < lines.size() - 1) {
                sBuilder.append(lineSeparator());
            }
        }

        return "" + sBuilder + COLOR_DEFAULT;
    }

    protected static List<EnumeratedLine> getLinesFromToken(Token token, CommonTokenStream tokens) {
        return SSCCUtil.Text.getLinesAroundToken(token, tokens, LINES_BEFORE, LINES_AFTER);
    }

    protected static List<EnumeratedLine> getLinesFromCtx(ParserRuleContext ctx, CommonTokenStream tokens) {
        return getLinesFromToken(ctx.getStart(), tokens);
    }

    /// Creates a locator highlighting a single token
    protected static String getLocator(Token token) {
        final int offset = getLineNumberOffset(token.getLine());
        final int posInLine = token.getCharPositionInLine();
        final int len = token.getStopIndex() - token.getStartIndex() + 1;

        return " ".repeat(offset + posInLine) + "^".repeat(len) + " here";
    }

    /// Creates a locator highlighting a context
    protected static String getLocator(ParserRuleContext ctx) {
        final int startLine = ctx.getStart().getLine();
        final int endLine = ctx.getStop().getLine();

        final int offset = getLineNumberOffset(startLine);

        final int start = ctx.getStart().getCharPositionInLine();
        final int stop = ctx.getStop().getCharPositionInLine();

        final int nSpaces = offset + start;
        final int nCarets = Math.max(
                1,
                endLine == startLine
                        ? stop - start
                        : 1
        );

        final String spaces = " ".repeat(nSpaces);
        final String carets = "^".repeat(nCarets);

        return spaces + carets + " here";
    }

    private static int getLineNumberOffset(final int lineNumber) {
        return getLineNumberLength(lineNumber,
                lineNumber - (LINES_BEFORE + LINES_AFTER)) + LINENO_SEPARATOR.length();
    }

    private static int getLineNumberLength(final int min, final int max) {
        return Math.max(1, Math.max(digitsOf(min), digitsOf(max)));
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

        if (message != null) {
            sBuilder
                    .append("    ")
                    .append(message)
                    .append(COLOR_DEFAULT)
                    .append(lineSeparator());
        }
        sBuilder.append(context);

        if (locator != null) {
            sBuilder.append(lineSeparator())
                    .append(COLOR_LOCATOR)
                    .append(locator);
        }

        sBuilder.append(COLOR_DEFAULT);

        return sBuilder.toString();
    }


    protected enum Type {
        Syntax,
        Antlr_parser,
        ;

        public final ConsoleColor toColor() {
            return switch (this) {
                case Syntax -> COLOR_FATAL;
                case Antlr_parser -> COLOR_ANTLR;
            };
        }

        public final String humanReadableName() {
            return name().replace('_', ' ');
        }
    }
}

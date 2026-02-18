package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.util.EnumeratedLine;
import cz.mipit.sscc.util.Range;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;
import cz.mipit.sscc.util.color.ConsoleColor;
import cz.mipit.sscc.util.color.ConsoleColorFactory;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import static cz.mipit.sscc.util.SSCCUtil.Maths.digitsOf;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

public abstract class SSCTranspilerException extends RuntimeException {
    private static final int LINES_BEFORE = 4;
    private static final int LINES_AFTER = 0;

    protected static final ConsoleColor COLOR_ERR_MESSAGE = ConsoleColorFactory.create(Ground.FORE, Color.RED);
    protected static final ConsoleColor COLOR_WARNING = ConsoleColorFactory.create(Ground.FORE, Color.YELLOW);
    protected static final ConsoleColor COLOR_OTHER = ConsoleColorFactory.create(Ground.BACK, Color.YELLOW);

    protected static final ConsoleColor COLOR_CODE = ConsoleColorFactory.create(Ground.FORE, Color.WHITE);
    protected static final ConsoleColor COLOR_LOCATOR = ConsoleColorFactory.create(Ground.FORE, Color.CYAN);

    private static final ConsoleColor COLOR_CODE_BOLD = new ConsoleColor() {
        private static final String r = "\u001B[1m" + COLOR_CODE;

        @Override
        public void setConsoleColor(PrintStream stream) {
            stream.print(r);
        }

        @Override
        public String toString() {
            return r;
        }
    };

    public static final String LINENO_SEPARATOR = " | ";


    private final InputFile currentFile;
    private final Type type;
    private final @Nullable String message;
    private final String context;
    private final @Nullable String locator;


    private String formattedMessage() {
        final ConsoleColor color = type.toColor();

        final StringBuilder sBuilder = new StringBuilder(color.toString());
        sBuilder
                .append(Main.SSCC_NAME)
                .append(": ")
                .append(type.humanReadableName())
                .append(" exception while processing file '")
                .append(COLOR_DEFAULT)
                .append(currentFile.getFullName())
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

    protected SSCTranspilerException(
            final Type type,
            final SSCTranspilerException e,
            final InputFile currentFile
    ) {
        this(type, null, e.getMessage(), null, currentFile);
    }

    protected SSCTranspilerException(Type type, String message,
                                     ParserRuleContext ctx, CommonTokenStream tokens,
                                     InputFile currentFile) {
        this(type, message, getLinesFromCtx(
                        requireNonNull(ctx, "Context"),
                        requireNonNull(tokens, "Token stream")),
                getLocator(ctx),
                currentFile);
    }

    protected SSCTranspilerException(Type type, String message, Token token,
                                     CommonTokenStream tokens, InputFile currentFile) {
        this(type, message, getLinesFromToken(token, tokens), getLocator(token), currentFile);
    }

    protected static String formatLines(final List<EnumeratedLine> lines) {
        final StringBuilder sBuilder = new StringBuilder(256)
                .append(COLOR_CODE_BOLD);

        final int fst = lines.get(0).lineNumber();
        final int last = lines.get(lines.size() - 1).lineNumber();
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

    /// Error nodes must be sorted.
    protected static String getLocator(EnumeratedLine enumeratedLine, Set<Integer> errorNodes) {
        final int offset = getLineNumberOffset(enumeratedLine.lineNumber());

        final StringBuilder sb = new StringBuilder(" ".repeat(offset));

        for (int i = 0; i < enumeratedLine.line().length(); i++) {
            if (errorNodes.contains(i)) {
                sb.append("^");
            } else {
                sb.append(" ");
            }
        }

        return sb + " here";
    }

    /// Creates a locator for the whole line
    protected static String getLocator(EnumeratedLine line) {
        return getLocator(line, new Range(0, line.line().length()));
    }

    /// Creates a locator for a given range of columns
    protected static String getLocator(final EnumeratedLine line,
                                       final Range range) {
        return getLocator(line, Set.copyOf(range));
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
        final int line = ctx.getStart().getLine();
        final int offset = getLineNumberOffset(line);

        final int start = ctx.getStart().getCharPositionInLine();
        final int stop = ctx.getStop().getCharPositionInLine();

        final String spaces = " ".repeat(offset + start);
        final String carets = ctx.getStop().getLine() == line
                ? "^".repeat(Math.max(stop - start, 1))
                : "^".repeat(ctx.getSourceInterval().length());

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
        return formattedMessage();
    }


    protected enum Type {
        Syntax, Antlr_parser, Preprocessor, Other;

        public final ConsoleColor toColor() {
            return switch (this) {
                case Syntax, Preprocessor -> COLOR_ERR_MESSAGE;

                case Antlr_parser -> COLOR_WARNING;

                case Other -> COLOR_OTHER;
            };
        }

        public final String humanReadableName() {
            return name().replace('_', ' ');
        }

        @Override
        public String toString() {
            return "enum Type{ " + name() + " }";
        }
    }
}

package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.ssc.preprocessor.EnumeratedLine;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.color.ConsoleColor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.Arrays;
import java.util.List;

import static cz.mipit.sscc.util.SSCCUtil.Maths.digitsOf;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.create;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

public abstract class SSCTranspilerException extends RuntimeException {
    private static final int LINES_BEFORE = 4;
    private static final int LINES_AFTER = 0;

    protected static final ConsoleColor COLOR_ERR_MESSAGE = create(Ground.FORE, Color.RED);
    protected static final ConsoleColor COLOR_WARNING = create(Ground.FORE, Color.YELLOW);
    protected static final ConsoleColor COLOR_OTHER = create(Ground.BACK, Color.YELLOW);

    protected static final ConsoleColor COLOR_CODE = create(Ground.FORE, Color.WHITE);
    protected static final ConsoleColor COLOR_LOCATOR = create(Ground.FORE, Color.CYAN);

    private static final ConsoleColor COLOR_CODE_BOLD = new ConsoleColor() {
        @Override
        public void setConsoleColor() {
            System.out.print(this);
        }

        @Override
        public String toString() {
            return "\u001B[1m" + COLOR_CODE;
        }
    };


    public static final String LINENO_SEPARATOR = " | ";

    private SSCTranspilerException(Type type, String message,
                                   String context, String locator) {
        super(requireNonNull(type).toColor() +
                "SSC Transpiler: " + type.humanReadableName() + " exception: " +
                requireNonNull(message, "Message") + COLOR_DEFAULT + lineSeparator() +
                context +
                (locator != null ? (lineSeparator() + COLOR_LOCATOR + locator + COLOR_DEFAULT) : "")
        );
    }

    protected SSCTranspilerException(Type type, String message,
                                     List<EnumeratedLine> lines, String locator) {
        this(type, message, formatLines(lines), locator);
    }

    protected SSCTranspilerException(
            final Type type,
            final String message,
            final SSCTranspilerException e
    ) {
        this(type, message, e.getMessage(), null);
    }

    protected SSCTranspilerException(Type type, String message,
                                     ParserRuleContext ctx, CommonTokenStream tokens) {
        this(type, message, getLinesFromCtx(
                requireNonNull(ctx, "Context"),
                requireNonNull(tokens, "Token stream")
        ), getLocator(ctx));
    }

    protected SSCTranspilerException(Type type, Token tok, CommonTokenStream tokens) {
        this(type, "Could not parse token '" + tok.getText() + "'", getLinesFromToken(
                requireNonNull(tok, "Token"),
                requireNonNull(tokens, "Token stream")
        ), getLocator(tok));
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
    protected static String getLocator(EnumeratedLine enumeratedLine, int[] errorNodes) {
        final int offset = getLineNumberOffset(enumeratedLine.lineNumber());

        final StringBuilder sb = new StringBuilder(" ".repeat(offset));

        for (int i = 0; i < enumeratedLine.line().length(); i++) {
            if (Arrays.binarySearch(errorNodes, i) >= 0) {
                sb.append("^");
            } else {
                sb.append(" ");
            }
        }

        return sb + " here";
    }

    /// Creates a locator for the whole line
    protected static String getLocator(EnumeratedLine line) {
        return getLocator(line, 0, line.line().length());
    }

    /// Creates a locator for a given range of columns
    protected static String getLocator(EnumeratedLine line, int from, int to) {
        return getLocator(line, SSCCUtil.Maths.getRange(from, to));
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
        return getLocator(ctx.getStart());
    }

    private static int getLineNumberOffset(final int lineNumber) {
        return getLineNumberLength(lineNumber, lineNumber - (LINES_BEFORE + LINES_AFTER))
                + LINENO_SEPARATOR.length();
    }

    private static int getLineNumberLength(final int min, final int max) {
        return Math.max(digitsOf(min), digitsOf(max));
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

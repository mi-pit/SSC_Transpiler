package cz.mipit.sscc.ssc.exceptions.data;

import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.color.ConsoleColor;
import cz.mipit.sscc.util.color.ConsoleColorFactory.Color;
import cz.mipit.sscc.util.color.ConsoleColorFactory.Ground;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Objects;

import static cz.mipit.sscc.ssc.exceptions.SSCTranspilerException.LINENO_SEPARATOR;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.create;

public class Locator {
    private static final ConsoleColor COLOR_LOCATOR = create(Ground.FORE, Color.CYAN);

    private final String data;

    private Locator(String data) {
        this.data = Objects.requireNonNull(data);
    }

    public Locator(
            final ParseTree tree,
            final CommonTokenStream tokens,
            final int lineNumber
    ) {
        this(getLocator(tree, tokens, lineNumber));
    }

    public Locator(
            final Token token,
            final int lineNumber
    ) {
        this(getLocator(token, lineNumber));
    }

    /// Creates a locator highlighting a single token
    private static String getLocator(
            final Token token,
            final int lastLineNumber
    ) {
        final int offset = getLineNumberOffset(lastLineNumber);
        final int posInLine = token.getCharPositionInLine();
        final int len = token.getStopIndex() - token.getStartIndex() + 1;

        final int nSpaces = offset + posInLine;
        return getLocator(nSpaces, len);
    }

    /// Creates a locator highlighting a context
    private static String getLocator(
            final ParserRuleContext ctx,
            final CommonTokenStream tokens,
            final int lastLineNumber
    ) {
        final int startLine = ctx.getStart().getLine();
        final int endLine = ctx.getStop().getLine();

        final int offset = getLineNumberOffset(lastLineNumber);

        final int start = ctx.getStart().getCharPositionInLine();

        final String literal = SSCCUtil.getLiteral(ctx, tokens);

        final int nSpaces = offset + start;
        final int nCarets = Math.max(
                1,
                endLine == startLine
                        ? literal.length()
                        : 1
        );

        return getLocator(nSpaces, nCarets);
    }

    private static String getLocator(
            ParseTree node,
            CommonTokenStream tokens,
            final int lastLineNumber
    ) {
        if (node instanceof TerminalNode t) {
            return getLocator(t.getSymbol(), lastLineNumber);
        }
        if (node instanceof ParserRuleContext p) {
            return getLocator(p, tokens, lastLineNumber);
        }

        throw new IllegalArgumentException("Unrecognized ParseTree type: " + node.getClass().getName());
    }


    private static String getLocator(int nSpaces, int nCarets) {
        final String spaces = " ".repeat(nSpaces);
        final String carets = "^".repeat(nCarets);

        return spaces + carets + " here";
    }


    private static int getLineNumberOffset(final int lineNumber) {
        return SSCCUtil.Text.getLineNumberLength(lineNumber,
                lineNumber - (SSCTranspilerException.LINES_BEFORE + SSCTranspilerException.LINES_AFTER)) + LINENO_SEPARATOR.length();
    }


    @Override
    public String toString() {
        return COLOR_LOCATOR + data + COLOR_DEFAULT;
    }
}

package cz.muni.fi.sscc.util;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Util {
    private Util() {
    }

    /**
     * Returns the exact text corresponding to a ParserRuleContext.
     * Works for any context.
     */
    public static String getContextText(ParserRuleContext ctx, CommonTokenStream tokens) {
        final int start = ctx.getStart().getTokenIndex();
        final int stop = ctx.getStop().getTokenIndex();
        return tokens.getText(new Interval(start, stop));
    }

    public static String getContextAroundToken(final Token token,
                                               final CommonTokenStream tokens,
                                               final int back,
                                               final int forward) {
        final int idx = token.getTokenIndex();
        final List<Token> tokenList = tokens.getTokens();

        final int from = Math.max(0, idx - back);
        final int to = Math.min(tokenList.size() - 1, idx + forward);

        final StringBuilder sb = new StringBuilder();

        for (int i = from; i <= to; ++i) {
            final Token t = tokenList.get(i);
            sb.append(t.getText());
        }

        return sb.toString();
    }

    public static String getLinesAroundToken(Token token, CommonTokenStream tokens, int before, int after) {
        String fullText = tokens.getTokenSource().getInputStream().toString();
        String[] lines = fullText.split("\n", -1);

        int lineIndex = token.getLine() - 1;
        int start = Math.max(0, lineIndex - before);
        int end = Math.min(lines.length - 1, lineIndex + after);

        return String.join("\n", new ArrayList<>(Arrays.asList(lines).subList(start, end + 1)));
    }
}

package cz.muni.fi.sscc.util;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import java.util.List;

public class Util {

    /**
     * Returns the exact text corresponding to a ParserRuleContext.
     * Works for any context.
     */
    public static String getContextText(ParserRuleContext ctx, CommonTokenStream tokens) {
        if (ctx == null || tokens == null)
            return "";
        int start = ctx.getStart().getTokenIndex();
        int stop = ctx.getStop().getTokenIndex();
        return tokens.getText(new Interval(start, stop)).replace("\n", " ");
    }

    public static String getContextAroundToken(Token token, CommonTokenStream tokens, int radius) {
        if (token == null || tokens == null) return "";

        final int idx = token.getTokenIndex();
        final List<Token> list = tokens.getTokens();

        int from = Math.max(0, idx - radius);
        int to = Math.min(list.size() - 1, idx + radius);

        StringBuilder sb = new StringBuilder();
        for (int i = from; i <= to; i++) {
            sb.append(list.get(i).getText());
        }
        return sb.toString();
    }

    public static String getLineFromTokens(CommonTokenStream tokens, int lineNumber) {
        StringBuilder sb = new StringBuilder();

        for (Token t : tokens.getTokens()) {
            if (t.getLine() == lineNumber && t.getType() != Token.EOF) {
                sb.append(t.getText());
            }
        }
        return sb.toString().trim();
    }
}

package cz.muni.fi.sscc.util;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;

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
        return tokens.getText(new Interval(start, stop));
    }
}

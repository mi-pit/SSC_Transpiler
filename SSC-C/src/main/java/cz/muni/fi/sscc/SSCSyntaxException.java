package cz.muni.fi.sscc;

import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

public class SSCSyntaxException extends RuntimeException {
    public SSCSyntaxException(String message,
                              ParserRuleContext ctx,
                              CommonTokenStream tokens) {
        super(getFormatted(message, ctx, tokens));
    }

    public SSCSyntaxException(String message,
                              String context) {
        super(getFormatted(message, context));
    }

    private static String getFormatted(String message, ParserRuleContext ctx, CommonTokenStream tokens) {
        return String.format("SSC Syntax error: \"%s\"\n", message)
                + (ctx != null && tokens != null
                ? String.format("""
                    found at `%s`
                """, Util.getContextText(ctx, tokens))
                : "");
    }

    private static String getFormatted(String message, String ctx) {
        return String.format("""
                SSC Syntax error: "%s"
                    found at `%s`
                """, message, ctx);
    }
}

package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import static cz.muni.fi.sscc.util.Util.getContextAroundToken;

public abstract class SSCTranspilerException extends RuntimeException /* todo? make not runtime */ {
    protected static final String COLOR_RESET = "\u001B[0m";
    protected static final String COLOR_CYAN = "\u001B[36m";
    protected static final String COLOR_WHITE = "\u001B[37m";
    protected static final String COLOR_WHITE_BOLD = "\u001B[1m\u001B[37m";
    protected static final String COLOR_RED = "\u001B[31m";

    private static final int LINES_BEFORE = 4;
    private static final int LINES_AFTER = 0;

    private SSCTranspilerException(Type type, String message, String context) {
        super(COLOR_RED + "SSC Transpiler: " + type + " exception: " + message + COLOR_RESET + "\n" + context);
    }

    protected SSCTranspilerException(Type type, String message,
                                     ParserRuleContext ctx, CommonTokenStream tokens) {
        this(type, message, getFormattedMessage(ctx, tokens));
    }

    protected SSCTranspilerException(Type type, Token tok, CommonTokenStream tokens) {
        this(type, "Could not parse token", getFormattedMessage(tok, tokens));
    }


    public static String getFormattedMessage(Token token, CommonTokenStream tokens) {
        return COLOR_RED +
                "    in the middle of: `" +
                COLOR_WHITE +
                getContextAroundToken(token, tokens, 2, 2)
                        .replaceAll("\\s+", " ") +
                COLOR_RED +
                "`\n" +
                "    in:\n" +
                COLOR_WHITE_BOLD +
                Util.getLinesAroundToken(token, tokens, LINES_BEFORE, LINES_AFTER) +
                COLOR_RESET +
                "\n" +
                getLocalizationMessage(token);
    }

    public static String getFormattedMessage(ParserRuleContext ctx, CommonTokenStream tokens) {
        return getFormattedMessage(ctx.getStart(), tokens);
    }

    public static String getLocalizationMessage(Token token) {
        final int offset = token.getCharPositionInLine();
        final int len = token.getStopIndex() - token.getStartIndex() + 1;
        return " ".repeat(offset) + COLOR_CYAN + "^".repeat(len) + " here" + COLOR_RESET;
    }


    protected enum Type {
        Syntax, Antlr_parser;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }
}

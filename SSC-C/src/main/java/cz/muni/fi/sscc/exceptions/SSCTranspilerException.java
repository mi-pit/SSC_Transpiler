package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.Colors;
import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import static cz.muni.fi.sscc.util.Colors.COLOR_RESET;

public abstract class SSCTranspilerException extends RuntimeException /* todo? make not runtime */ {
    private static final int LINES_BEFORE = 4;
    private static final int LINES_AFTER = 0;

    protected static final String COLOR_MESSAGE = Colors.create(Colors.Ground.FORE, Colors.Color.RED);
    protected static final String COLOR_CODE = Colors.create(Colors.Ground.FORE, Colors.Color.WHITE);
    private static final String BOLD = "\u001B[1m";
    protected static final String COLOR_CODE_BOLD = BOLD + Colors.create(Colors.Ground.FORE, Colors.Color.WHITE);
    protected static final String COLOR_LOCATOR = Colors.create(Colors.Ground.FORE, Colors.Color.CYAN);

    private SSCTranspilerException(Type type, String message, String context) {
        super(COLOR_MESSAGE + "SSC Transpiler: " + type + " exception: " + message + COLOR_RESET + "\n"
                + context);
    }

    protected SSCTranspilerException(Type type, String message,
                                     ParserRuleContext ctx, CommonTokenStream tokens) {
        this(type, message, getFormattedMessage(ctx, tokens));
    }

    protected SSCTranspilerException(Type type, Token tok, CommonTokenStream tokens) {
        this(type, "Could not parse token", getFormattedMessage(tok, tokens));
    }


    public static String getFormattedMessage(Token token, CommonTokenStream tokens) {
        return COLOR_MESSAGE +
                "    in the middle of: `" +
                COLOR_CODE +
                Util.getContextAroundToken(token, tokens, 2, 2)
                        .replaceAll("\\s+", " ") +
                COLOR_MESSAGE +
                "`\n" +
                "    in:\n" +
                COLOR_CODE_BOLD +
                Util.getLinesAroundToken(token, tokens, LINES_BEFORE, LINES_AFTER) +
                COLOR_RESET +
                "\n" +
                Util.getLocalizationMessage(token, COLOR_LOCATOR);
    }

    public static String getFormattedMessage(ParserRuleContext ctx, CommonTokenStream tokens) {
        return getFormattedMessage(ctx.getStart(), tokens);
    }


    protected enum Type {
        Syntax, Antlr_parser;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }
}

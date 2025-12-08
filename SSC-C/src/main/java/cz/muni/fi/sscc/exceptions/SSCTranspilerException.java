package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.Colors;
import cz.muni.fi.sscc.util.Strings;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static cz.muni.fi.sscc.util.Colors.COLOR_RESET;

public class SSCTranspilerException extends RuntimeException /* todo? make not runtime */ {
    private static final int LINES_BEFORE = 4;
    private static final int LINES_AFTER = 0;

    public static final String COLOR_MESSAGE = Colors.create(Colors.Ground.FORE, Colors.Color.RED);
    public static final String COLOR_CODE = Colors.create(Colors.Ground.FORE, Colors.Color.WHITE);
    private static final String BOLD = "\u001B[1m";
    public static final String COLOR_CODE_BOLD = BOLD + Colors.create(Colors.Ground.FORE, Colors.Color.WHITE);
    public static final String COLOR_LOCATOR = Colors.create(Colors.Ground.FORE, Colors.Color.CYAN);

    private SSCTranspilerException(Type type, String message, String context) {
        super(COLOR_MESSAGE + "SSC Transpiler: " + type + " exception: " + message + COLOR_RESET + "\n"
                + context);
    }

    public SSCTranspilerException(Type type, String message,
                                  ParserRuleContext ctx, CommonTokenStream tokens) {
        this(
                Objects.requireNonNull(type, "Exception type"),
                Objects.requireNonNull(message, "Message"),
                getFormattedMessage(
                        Objects.requireNonNull(ctx, "Context"),
                        Objects.requireNonNull(tokens, "Tokens")
                )
        );
    }

    public SSCTranspilerException(Type type, Token tok, CommonTokenStream tokens) {
        this(type, "Could not parse token", getFormattedMessage(tok, tokens));
    }


    public static String getFormattedMessage(Token token, CommonTokenStream tokens) {
        return COLOR_MESSAGE +
                "    in the middle of: `" +
                COLOR_CODE +
                Strings.getContextAroundToken(token, tokens, 2, 2)
                        .replaceAll("\\s+", " ") +
                COLOR_MESSAGE +
                "`\n" +
                "    in:\n" +
                COLOR_CODE_BOLD +
                Strings.getLinesAroundToken(token, tokens, LINES_BEFORE, LINES_AFTER) +
                COLOR_RESET +
                "\n" +
                Strings.getLocalizationMessage(token, COLOR_LOCATOR);
    }

    public static String getFormattedMessage(ParserRuleContext ctx, CommonTokenStream tokens) {
        return getFormattedMessage(ctx.getStart(), tokens);
    }


    public enum Type {
        Syntax, Antlr_parser, Other;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }
}

package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.Strings;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

import static cz.muni.fi.sscc.util.UnixTerminalColors.*;
import static cz.muni.fi.sscc.util.UnixTerminalColors.COLOR_RESET;

public abstract class SSCTranspilerException extends RuntimeException {
    private static final int LINES_BEFORE = 4;
    private static final int LINES_AFTER = 0;

    private static final String BOLD = "\u001B[1m";

    public static final String COLOR_MESSAGE = create(Ground.FORE, Color.RED);
    public static final String COLOR_CODE = create(Ground.FORE, Color.WHITE);
    public static final String COLOR_CODE_BOLD = BOLD + create(Ground.FORE, Color.WHITE);
    public static final String COLOR_LOCATOR = create(Ground.FORE, Color.CYAN);

    private SSCTranspilerException(Type type, String message, String context) {
        super(COLOR_MESSAGE + "SSC Transpiler: " +
                Objects.requireNonNull(type, "Exception type") +
                " exception: " +
                Objects.requireNonNull(message, "Message") + COLOR_RESET + "\n" +
                Objects.requireNonNull(context, "Internal, how is this possible?"));
    }

    protected SSCTranspilerException(Type type, String message,
                                     ParserRuleContext ctx, CommonTokenStream tokens) {
        this(type, message, getFormattedMessage(
                Objects.requireNonNull(ctx, "Context"),
                Objects.requireNonNull(tokens, "Token stream")
        ));
    }

    protected SSCTranspilerException(Type type, Token tok, CommonTokenStream tokens) {
        this(type, "Could not parse token", getFormattedMessage(
                Objects.requireNonNull(tok, "Token"),
                Objects.requireNonNull(tokens, "Token stream")
        ));
    }


    public static String getFormattedMessage(Token token, CommonTokenStream tokens) {
        return COLOR_MESSAGE +
                "    at line: " + token.getLine() + "\n" +
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


    protected enum Type {
        Syntax, Antlr_parser, Other;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }
}

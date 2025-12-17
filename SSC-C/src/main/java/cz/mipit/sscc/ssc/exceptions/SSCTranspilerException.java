package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.util.ContextText;
import cz.mipit.sscc.util.UnixTerminalColors;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.Objects;

public abstract class SSCTranspilerException extends RuntimeException {
    private static final int LINES_BEFORE = 4;
    private static final int LINES_AFTER = 0;

    private static final String BOLD = "\u001B[1m";

    public static final String COLOR_MESSAGE =
            UnixTerminalColors.create(UnixTerminalColors.Ground.FORE, UnixTerminalColors.Color.RED);
    public static final String COLOR_CODE =
            UnixTerminalColors.create(UnixTerminalColors.Ground.FORE, UnixTerminalColors.Color.WHITE);
    public static final String COLOR_CODE_BOLD =
            BOLD + UnixTerminalColors.create(UnixTerminalColors.Ground.FORE, UnixTerminalColors.Color.WHITE);
    public static final String COLOR_LOCATOR =
            UnixTerminalColors.create(UnixTerminalColors.Ground.FORE, UnixTerminalColors.Color.CYAN);

    protected SSCTranspilerException(Type type, String message, String context) {
        super(COLOR_MESSAGE + "SSC Transpiler: " +
                Objects.requireNonNull(type, "Exception type") +
                " exception: " +
                Objects.requireNonNull(message, "Message") + UnixTerminalColors.COLOR_RESET + "\n" +
                Objects.requireNonNull(context, "Context string"));
    }

    protected SSCTranspilerException(Type type, String message,
                                     ParserRuleContext ctx, CommonTokenStream tokens) {
        this(type, message, getFormattedMessage(
                Objects.requireNonNull(ctx, "Context"),
                Objects.requireNonNull(tokens, "Token stream")
        ));
    }

    protected SSCTranspilerException(Type type, Token tok, CommonTokenStream tokens) {
        this(
                type,
                "Could not parse token '" + tok.getText() + "'",
                getFormattedMessage(
                        Objects.requireNonNull(tok, "Token"),
                        Objects.requireNonNull(tokens, "Token stream")
                )
        );
    }


    public static String getFormattedMessage(Token token, CommonTokenStream tokens) {
        return COLOR_MESSAGE +
                "    at line: " + token.getLine() + "\n" +
                "    in the middle of: `" +
                COLOR_CODE +
                ContextText.getContextAroundToken(token, tokens, 2, 2)
                        .replaceAll("\\s+", " ") +
                COLOR_MESSAGE +
                "`\n" +
                "    in:\n" +
                COLOR_CODE_BOLD +
                ContextText.getLinesAroundToken(token, tokens, LINES_BEFORE, LINES_AFTER) +
                UnixTerminalColors.COLOR_RESET +
                "\n" +
                ContextText.getLocalizationMessage(token, COLOR_LOCATOR);
    }

    public static String getFormattedMessage(ParserRuleContext ctx, CommonTokenStream tokens) {
        return getFormattedMessage(ctx.getStart(), tokens);
    }


    protected enum Type {
        Syntax, Antlr_parser, Preprocessor, Other;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }
}

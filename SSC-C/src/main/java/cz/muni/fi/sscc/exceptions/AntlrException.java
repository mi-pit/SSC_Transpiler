package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Pair;

import java.util.Objects;

public class AntlrException extends RuntimeException {
    public AntlrException(Token token, CommonTokenStream tokens) {
        super(getFormattedMessage(
                Objects.requireNonNull(token),
                Objects.requireNonNull(tokens)
        ));
    }

    public AntlrException(Pair<String, Object> message) {
        super(String.format("""
                        Antlr parser exception: '%s'
                            in the middle of: `%s`
                            in:
                        """,
                message.a,
                message.b));
    }

    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String WHITE = "\u001B[37m";

    public static String getFormattedMessage(Token token, CommonTokenStream tokens) {
        return String.format("""
                        Antlr parser exception: "Could not parse token '%s'"
                            in the middle of: `%s`
                            in:
                        """,
                token.getText(),
                Util.getContextAroundToken(token, tokens, 2, 2).replaceAll("\\s+", " "))
                + WHITE
                + Util.getLinesAroundToken(token, tokens, 3, 0) + "\n"
                + getLocalizationMessage(token) + "\n";
    }

    public static String getLocalizationMessage(Token token) {
        final int offset = token.getCharPositionInLine();
        return " ".repeat(offset) + CYAN + "^ here" + RESET;
    }
}

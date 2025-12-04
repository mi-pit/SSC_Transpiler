package cz.muni.fi.sscc;

import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

public class AntlrException extends RuntimeException {
    public AntlrException(Token token, CommonTokenStream tokens) {
        super(getFormattedMessage(token, tokens));
    }

    public static String getFormattedMessage(Token token, CommonTokenStream tokens) {
        return String.format("""
                        Antlr parser exception: "Could not parse token '%s'"
                            in the middle of: `%s`
                            at line %d:
                        ```
                        %s
                        ```
                        """,
                token.getText(),
                Util.getContextAroundToken(token, tokens, 2).replace("\n", " "),
                token.getLine(),
                Util.getLineFromTokens(tokens, token.getLine()));
    }
}

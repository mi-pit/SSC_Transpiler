package cz.muni.fi.sscc.exceptions;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

public class SSCSyntaxException extends SSCTranspilerException {
    public SSCSyntaxException(String message,
                              ParserRuleContext ctx,
                              CommonTokenStream tokens) {
        super(Type.Syntax, message, ctx, tokens);
    }
}

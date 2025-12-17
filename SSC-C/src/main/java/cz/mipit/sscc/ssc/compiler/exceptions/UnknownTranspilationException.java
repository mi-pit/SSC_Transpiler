package cz.mipit.sscc.ssc.compiler.exceptions;

import cz.mipit.sscc.util.ContextText;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

public class UnknownTranspilationException extends RuntimeException {
    public UnknownTranspilationException(String message, ParserRuleContext ctx, CommonTokenStream tokens) {
        super(message + " | " + ContextText.getLiteral(ctx, tokens));
    }
}

package cz.muni.fi.sscc.exceptions;

import cz.muni.fi.sscc.util.Strings;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

public class UnknownTranspilationException extends RuntimeException {
    public UnknownTranspilationException(String message, ParserRuleContext ctx, CommonTokenStream tokens) {
        super(message + " | " + Strings.getContextText(ctx, tokens));
    }
}

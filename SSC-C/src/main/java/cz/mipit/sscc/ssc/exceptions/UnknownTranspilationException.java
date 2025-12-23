package cz.mipit.sscc.ssc.exceptions;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

public class UnknownTranspilationException extends SSCTranspilerException {
    public UnknownTranspilationException(String message, ParserRuleContext ctx, CommonTokenStream tokens) {
        super(Type.Other, message, ctx, tokens);
    }
}

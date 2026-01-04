package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

public class UnknownTranspilationException extends SSCTranspilerException {
    public UnknownTranspilationException(String message, ParserRuleContext ctx,
                                         CommonTokenStream tokens, InputFile currentFile) {
        super(Type.Other, message, ctx, tokens, currentFile);
    }
}

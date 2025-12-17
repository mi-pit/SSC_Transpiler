package cz.mipit.sscc.ssc.compiler.exceptions;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

public class AntlrException extends SSCTranspilerException {
    public AntlrException(Token token, CommonTokenStream tokens) {
        super(Type.Antlr_parser, token, tokens);
    }
}

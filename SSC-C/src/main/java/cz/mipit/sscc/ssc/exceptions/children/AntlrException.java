package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.File;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

public class AntlrException extends SSCTranspilerException {
    public AntlrException(String message, Token token, CommonTokenStream tokens, File file) {
        super(Type.Antlr_parser, message, token, tokens, file);
    }
}

package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

public class AntlrException extends SSCTranspilerException {
    public AntlrException(Token token, CommonTokenStream tokens, InputFile currentFile) {
        super(Type.Antlr_parser, token, tokens, currentFile);
    }
}

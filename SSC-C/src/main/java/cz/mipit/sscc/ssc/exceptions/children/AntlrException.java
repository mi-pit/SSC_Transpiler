package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

public class AntlrException extends SSCTranspilerException {

    public AntlrException(String message, Token token, CommonTokenStream tokens, InputFile inputFile) {
        super(Type.Antlr_parser, message, token, tokens, inputFile);
    }

    public AntlrException(Token tok, CommonTokenStream tokens, InputFile inputFile) {
        this("Could not parse token '" + tok.getText() + "'", tok, tokens, inputFile);
    }
}

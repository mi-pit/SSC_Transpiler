package cz.mipit.sscc.ssc.compiler;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.exceptions.children.AntlrException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class SSCErrorListener extends BaseErrorListener {
    private final InputFile inputFile;

    public SSCErrorListener(InputFile inputFile) {
        super();
        this.inputFile = inputFile;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        Token token = (Token) offendingSymbol;

        final String symbolicName = SSCParser.VOCABULARY.getSymbolicName(token.getType());

        print(new AntlrException(
                msg + System.lineSeparator() + "    got " + symbolicName,
                token,
                (CommonTokenStream) recognizer.getInputStream(),
                inputFile
        ));
    }

    private static void print(final SSCTranspilerException e) {
        System.err.println(e.getMessage());
    }
}

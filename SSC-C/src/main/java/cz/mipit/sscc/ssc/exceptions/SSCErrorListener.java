package cz.mipit.sscc.ssc.exceptions;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.exceptions.children.AntlrException;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.SequencedCollection;

public class SSCErrorListener extends BaseErrorListener {
    private final SequencedCollection<AntlrException> errors;

    public SSCErrorListener(SequencedCollection<AntlrException> errors) {
        super();
        this.errors = errors;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        final Token token = (Token) offendingSymbol;
        final CommonTokenStream tokenStream = (CommonTokenStream) recognizer.getInputStream();

        final String symbolicNameMsg = msg.contains("expected")
                ? System.lineSeparator() + "    got " + SSCParser.VOCABULARY.getSymbolicName(token.getType())
                : "";

        errors.add(
                new AntlrException(
                        msg + symbolicNameMsg,
                        token,
                        tokenStream
                )
        );
    }
}

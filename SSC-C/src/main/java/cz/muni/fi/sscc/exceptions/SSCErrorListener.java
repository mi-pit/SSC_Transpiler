package cz.muni.fi.sscc.exceptions;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.misc.Pair;

import java.util.Optional;

public class SSCErrorListener extends BaseErrorListener {
    private String lastErrorMessage = null;
    private Object offendingSymbol = null;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        lastErrorMessage = msg;
        this.offendingSymbol = offendingSymbol;
    }

    public Optional<Pair<String, Object>> getLastErrorMessage() {
        assert (lastErrorMessage == null) == (offendingSymbol == null);
        if (lastErrorMessage == null) {
            return Optional.empty();
        }
        return Optional.of(new Pair<>(lastErrorMessage, offendingSymbol));
    }
}

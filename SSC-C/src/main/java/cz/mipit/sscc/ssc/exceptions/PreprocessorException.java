package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.ssc.preprocessor.Preprocessor;
import org.antlr.v4.runtime.misc.Pair;

import static cz.mipit.sscc.util.UnixTerminalColors.COLOR_RESET;

public class PreprocessorException extends SSCTranspilerException {
    public PreprocessorException(String message) {
        super(Type.Preprocessor, message, getLastThreeLinesFormatted());
    }

    private static String getLastThreeLinesFormatted() {
        assert !Preprocessor.getCurrentLineQueue().isEmpty();

        final StringBuilder sb = new StringBuilder();
        String lastLine = null;
        for (Pair<Integer, String> pair : Preprocessor.getCurrentLineQueue()) {
            lastLine = pair.b;
            sb
                    .append(String.format("%d: %s", pair.a, lastLine))
                    .append("\n");
        }
        sb
                .append("   ")
                .append(COLOR_LOCATOR)
                .append("^".repeat(lastLine.length()))
                .append(" here")
                .append(COLOR_RESET);

        return sb.toString();
    }
}

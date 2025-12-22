package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.ssc.preprocessor.Preprocessor;

import static cz.mipit.sscc.util.UnixTerminalColors.COLOR_RESET;

public class PreprocessorException extends SSCTranspilerException {
    public PreprocessorException(String message,
                                 String context) {
        super(Type.Preprocessor, message, context);
    }

    public PreprocessorException(String message) {
        this(message, format());
    }

    private static String format() {
        final StringBuilder sb = new StringBuilder();

        String lastLine = null;
        for (final String line : Preprocessor.getLast3Lines()) {
            sb
                    .append(line)
                    .append("\n");

            lastLine = line;
        }
        assert lastLine != null;
        sb
                .append(COLOR_LOCATOR)
                .append("^".repeat(lastLine.length()))
                .append(" here")
                .append(COLOR_RESET);

        return sb.toString();
    }
}

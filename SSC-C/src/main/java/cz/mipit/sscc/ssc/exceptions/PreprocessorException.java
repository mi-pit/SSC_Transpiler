package cz.mipit.sscc.ssc.exceptions;

import cz.mipit.sscc.ssc.preprocessor.Preprocessor;

public class PreprocessorException extends SSCTranspilerException {
    public PreprocessorException(String message,
                                 String context) {
        super(Type.Preprocessor, message, context);
    }

    public PreprocessorException(String message) {
        this(message, Preprocessor.getCurrentLine());
    }
}

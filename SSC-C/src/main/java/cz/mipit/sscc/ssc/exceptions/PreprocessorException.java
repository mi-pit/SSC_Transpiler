package cz.mipit.sscc.ssc.exceptions;

public class PreprocessorException extends SSCTranspilerException {
    public PreprocessorException(String message,
                                 String context) {
        super(Type.Preprocessor, message, context);
    }
}

package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.preprocessor.EnumeratedLine;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.List;

public class PreprocessorException extends SSCTranspilerException {
    public PreprocessorException(final String message, final SSCTranspilerException e) {
        super(Type.Preprocessor, message, e);
    }

    private PreprocessorException(String message, List<EnumeratedLine> lines, String locator) {
        super(Type.Preprocessor, message, lines, locator);
    }

    public PreprocessorException(String message,
                                 List<EnumeratedLine> lines) {
        this(message, lines, getLocator(getLast(lines)));
    }

    public PreprocessorException(String message,
                                 List<EnumeratedLine> lines,
                                 int[] errorNodes) {
        this(message, lines, getLocator(getLast(lines), errorNodes));
    }

    public PreprocessorException(String message, List<EnumeratedLine> enumeratedLines, int start, int end) {
        this(message, enumeratedLines, SSCCUtil.Maths.getRange(start, end));
    }


    private static EnumeratedLine getLast(List<EnumeratedLine> enumeratedLines) {
        return enumeratedLines.get(enumeratedLines.size() - 1);
    }
}

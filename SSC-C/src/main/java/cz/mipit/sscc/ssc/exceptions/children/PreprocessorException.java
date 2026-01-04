package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.preprocessor.EnumeratedLine;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.List;

public class PreprocessorException extends SSCTranspilerException {
    public PreprocessorException(final String message, final SSCTranspilerException e, InputFile currentFile) {
        super(Type.Preprocessor, message, e, currentFile);
    }

    private PreprocessorException(String message, List<EnumeratedLine> lines,
                                  String locator, InputFile currentFile) {
        super(Type.Preprocessor, message, lines, locator, currentFile);
    }

    public PreprocessorException(String message, List<EnumeratedLine> lines,
                                 InputFile currentFile) {
        this(message, lines, getLocator(getLast(lines)), currentFile);
    }

    public PreprocessorException(String message,
                                 List<EnumeratedLine> lines,
                                 int[] errorNodes, InputFile currentFile) {
        this(message, lines, getLocator(getLast(lines), errorNodes), currentFile);
    }

    public PreprocessorException(String message, List<EnumeratedLine> enumeratedLines,
                                 int start, int end, InputFile currentFile) {
        this(message, enumeratedLines, SSCCUtil.Maths.getRange(start, end), currentFile);
    }


    private static <T> T getLast(List<T> ls) {
        return ls.get(ls.size() - 1);
    }
}

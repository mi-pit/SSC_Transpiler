package cz.mipit.sscc.ssc.exceptions.children;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.preprocessor.EnumeratedLine;
import cz.mipit.sscc.util.Range;

import java.util.List;
import java.util.Set;

public class PreprocessorException extends SSCTranspilerException {
    public PreprocessorException(final SSCTranspilerException e, InputFile currentFile) {
        super(Type.Preprocessor, e, currentFile);
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
                                 Set<Integer> errorNodes,
                                 InputFile currentFile) {
        this(message, lines, getLocator(getLast(lines), errorNodes), currentFile);
    }

    public PreprocessorException(String message, List<EnumeratedLine> enumeratedLines,
                                 Range range, InputFile currentFile) {
        this(message, enumeratedLines, Set.copyOf(range), currentFile);
    }


    private static <T> T getLast(List<T> ls) {
        return ls.get(ls.size() - 1);
    }
}

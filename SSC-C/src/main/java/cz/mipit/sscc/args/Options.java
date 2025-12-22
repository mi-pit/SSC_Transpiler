package cz.mipit.sscc.args;

import cz.mipit.sscc.file.InputFile;

import java.util.List;

public record Options(boolean verbose, boolean debug, boolean stopOnError,
                      String compileTargetFilename, List<InputFile> filesToProcess) {
    public static final Options DEFAULT =
            new Options(false, false, true, null, List.of());
}

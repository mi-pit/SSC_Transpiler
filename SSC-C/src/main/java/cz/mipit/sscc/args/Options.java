package cz.mipit.sscc.args;

import cz.mipit.sscc.file.InputFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Options {
    public static final Options DEFAULT =
            new Options(false, false, true, null, Collections.emptyList());

    private final boolean verbose;
    private final boolean debug;
    private final boolean stopOnError;
    private final String compileTargetFilename;
    private final List<InputFile> filesToProcess;

    public Options(boolean verbose, boolean debug, boolean stopOnError,
                   String compileTargetFilename, List<InputFile> filesToProcess) {
        this.verbose = verbose;
        this.debug = debug;
        this.stopOnError = stopOnError;
        this.compileTargetFilename = compileTargetFilename;
        this.filesToProcess = filesToProcess;
    }

    public boolean verbose() {
        return verbose;
    }

    public boolean debug() {
        return debug;
    }

    public boolean stopOnError() {
        return stopOnError;
    }

    public Optional<String> compileTargetFilename() {
        return Optional.ofNullable(compileTargetFilename);
    }

    public List<InputFile> filesToProcess() {
        return Collections.unmodifiableList(filesToProcess);
    }
}

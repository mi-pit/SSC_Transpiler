package cz.mipit.sscc.args;

import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.cc.CCStandard;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Options {
    public static final boolean DEF_VERBOSE = false;
    public static final boolean DEF_DEBUG = false;
    public static final boolean DEF_STOP_ON_ERROR = true;
    public static final String DEF_COMPILER_TARGET = null;
    public static final List<InputFile> DEF_FILES_TO_PROCESS = Collections.emptyList();
    public static final CCStandard DEF_C_STANDARD = CCStandard.C23;

    public static final Options DEFAULT =
            new Options(DEF_VERBOSE, DEF_DEBUG,
                    DEF_STOP_ON_ERROR, DEF_COMPILER_TARGET,
                    DEF_FILES_TO_PROCESS, DEF_C_STANDARD);

    private final boolean verbose;
    private final boolean debug;
    private final boolean stopOnError;
    private final String compileTargetFilename;
    private final List<InputFile> filesToProcess;
    private final CCStandard standard;

    Options(boolean verbose, boolean debug, boolean stopOnError,
            String compileTargetFilename, List<InputFile> filesToProcess,
            CCStandard standard) {
        this.verbose = verbose;
        this.debug = debug;
        this.stopOnError = stopOnError;
        this.compileTargetFilename = compileTargetFilename;
        this.filesToProcess = filesToProcess;
        this.standard = standard;
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

    public CCStandard cStandard() {
        return standard;
    }
}

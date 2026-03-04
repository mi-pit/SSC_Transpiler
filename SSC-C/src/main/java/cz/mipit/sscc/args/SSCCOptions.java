package cz.mipit.sscc.args;

import cz.mipit.sscc.file.InputFile;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class SSCCOptions implements Iterable<Option<?>> {
    public static final String OPTSTR_HELP_SHORT = "-h";
    public static final String OPTSTR_HELP_LONG = "--help";
    static final Option<Void> OPTION_HELP = new Option<>(
            new OptionString(OPTSTR_HELP_SHORT, OPTSTR_HELP_LONG),
            "Help", "Display this message",
            null, Void.class, null, null
    );

    public static final String OPTSTR_VERBOSE_SHORT = "-v";
    public static final String OPTSTR_VERBOSE_LONG = "--verbose";
    private final Option<Boolean> OPTION_VERBOSE = new Option<>(
            new OptionString(OPTSTR_VERBOSE_SHORT, OPTSTR_VERBOSE_LONG),
            "Verbose", "Print information about current stage",
            null, Boolean.class, false, NextOperation.None
    );

    public static final String OPTSTR_STOP_ON_ERROR_SHORT = "-s";
    public static final String OPTSTR_STOP_ON_ERROR_LONG = "--no-stop-on-error";
    private final Option<Boolean> OPTION_STOP_ON_ERROR = new Option<>(
            new OptionString(OPTSTR_STOP_ON_ERROR_SHORT, OPTSTR_STOP_ON_ERROR_LONG),
            "Don't stop on error", "By default, sscc stops processing after encountering an error (this powers through)",
            null, Boolean.class, true, NextOperation.None
    );

    public static final String OPTSTR_NORMAL_DEBUG = "--debug";
    private final Option<Boolean> OPTION_DEBUG = new Option<>(
            new OptionString(OPTSTR_NORMAL_DEBUG),
            "Debug mode", "Print debug information (unstable)",
            null, Boolean.class, false, NextOperation.None
    );

    public static final String OPTSTR_ANTLR_DEBUG = "--debug!";
    private final Option<Boolean> OPTION_ANTLR_DEBUG = new Option<>(
            new OptionString(OPTSTR_ANTLR_DEBUG),
            "Token debug mode", "Print antlr debug information (unstable)",
            null, Boolean.class, false, NextOperation.None
    );

    public static final String OPTSTR_COMPILE_SHORT = "-c";
    public static final String OPTSTR_COMPILE_LONG = "--compile";
    private final Option<String> OPTION_COMPILE = new Option<>(
            new OptionString(OPTSTR_COMPILE_SHORT, OPTSTR_COMPILE_LONG),
            "Compile", "Compiles the resulting C code into a binary",
            "name of the resulting binary", String.class, null, NextOperation.CompileTarget
    );

    public static final String OPTSTR_LIB_SHORT = "-d";
    public static final String OPTSTR_LIB_LONG = "--dir";
    private final Option<Path> OPTION_LIB = new Option<>(
            new OptionString(OPTSTR_LIB_SHORT, OPTSTR_LIB_LONG),
            "Directory", "Process all `.c` & `.ssc` files in a directory",
            "path to root of a directory to be processed", Path.class, null, NextOperation.LibPath
    );

    public static final String OPTSTR_STOP_OPTS_LONG = "--";
    private final Option<Boolean> OPTION_STOP_OPTS = new Option<>(
            new OptionString(OPTSTR_STOP_OPTS_LONG),
            "Terminate options parsing", "Treats all following strings as file names",
            null, Boolean.class, false, NextOperation.FilesOnly
    );

    private final List<Option<?>> OPTIONS = List.of(
            OPTION_HELP,
            OPTION_VERBOSE,
            OPTION_STOP_ON_ERROR,
            OPTION_DEBUG,
            OPTION_ANTLR_DEBUG,
            OPTION_COMPILE,
            OPTION_LIB,
            OPTION_STOP_OPTS
    );

    private final Set<InputFile> filesToProcess = new HashSet<>();

    private SSCCOptions() {
    }

    public static SSCCOptions newWithDefaults() {
        return new SSCCOptions();
    }

    public boolean verbose() {
        return OPTION_VERBOSE.value();
    }

    public boolean debug() {
        return OPTION_DEBUG.value();
    }

    public boolean stopOnError() {
        return OPTION_STOP_ON_ERROR.value();
    }

    public Optional<String> compileTarget() {
        return Optional.ofNullable(OPTION_COMPILE.value());
    }

    public void setCompileTarget(String filename) {
        OPTION_COMPILE.setValue(Objects.requireNonNull(filename));
    }

    public void addFile(InputFile file) {
        filesToProcess.add(Objects.requireNonNull(file));
    }

    public void addFiles(Set<InputFile> inputFiles) {
        filesToProcess.addAll(inputFiles);
    }

    public Set<InputFile> filesToProcess() {
        return Collections.unmodifiableSet(filesToProcess);
    }

    @Override
    public Iterator<Option<?>> iterator() {
        return OPTIONS.iterator();
    }
}

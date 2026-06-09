package cz.mipit.sscc.args;

import cz.mipit.sscc.file.File;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

//TODO
public final class SSCCOptions implements Iterable<Option<?>> {
    public static final String OPTSTR_HELP_SHORT = "-h";
    public static final String OPTSTR_HELP_LONG = "--help";
    static final Option<Void> OPTION_HELP = new Option<>(
            new OptionString(OPTSTR_HELP_SHORT, OPTSTR_HELP_LONG),
            "Help", "Display this message",
            List.of(), Void.class, null, null
    );

    public static final String OPTSTR_VERBOSE_SHORT = "-v";
    public static final String OPTSTR_VERBOSE_LONG = "--verbose";
    private final Option<Boolean> OPTION_VERBOSE = new Option<>(
            new OptionString(OPTSTR_VERBOSE_SHORT, OPTSTR_VERBOSE_LONG),
            "Verbose", "Print information about current stage",
            List.of(), Boolean.class, false, NextOperation.None
    );

    public static final String OPTSTR_NORMAL_DEBUG = "--debug";
    private final Option<Boolean> OPTION_DEBUG = new Option<>(
            new OptionString(OPTSTR_NORMAL_DEBUG),
            "Debug mode", "Print debug information (unstable)",
            List.of(), Boolean.class, false, NextOperation.None
    );

    public static final String OPTSTR_ANTLR_DEBUG = "--debug!";
    private final Option<Boolean> OPTION_ANTLR_DEBUG = new Option<>(
            new OptionString(OPTSTR_ANTLR_DEBUG),
            "Token debug mode", "Print antlr debug information (unstable)",
            List.of(), Boolean.class, false, NextOperation.None
    );

    public static final String OPTSTR_COMPILE_SHORT = "-c";
    public static final String OPTSTR_COMPILE_LONG = "--compile";
    private final Option<String> OPTION_COMPILE = new Option<>(
            new OptionString(OPTSTR_COMPILE_SHORT, OPTSTR_COMPILE_LONG),
            "Compile", "Compiles the resulting C code into a binary",
            List.of("name of the resulting binary"), String.class, null, NextOperation.CompileTarget
    );

    public static final String OPTSTR_DIR_SHORT = "-d";
    public static final String OPTSTR_DIR_LONG = "--dir";
    private final Option<Path> OPTION_DIR = new Option<>(
            new OptionString(OPTSTR_DIR_SHORT, OPTSTR_DIR_LONG),
            "Directory", "Process all `.c` & `.ssc` files in a directory",
            List.of("path to root of a directory to be processed"), Path.class, null, NextOperation.LibPath
    );

    public static final String OPTSTR_FILETYPE_SHORT = "-x";
    public static final String OPTSTR_FILETYPE_LONG = "--filetype";
    private final Option<Path> OPTION_FILETYPE = new Option<>(
            new OptionString(OPTSTR_FILETYPE_SHORT, OPTSTR_FILETYPE_LONG),
            "Filetype",
            "Treat the following file as if it had the extension given as the first argument to this option (`-x c file.ssc` treats it as a c file)",
            List.of("('c' | 'ssc')", "filename"), Path.class, null, NextOperation.FileType
    );

    public static final String OPTSTR_STOP_OPTS = "--";
    private final Option<Boolean> OPTION_STOP_OPTS = new Option<>(
            new OptionString(OPTSTR_STOP_OPTS),
            "Terminate options parsing", "Treats all following strings as file names",
            List.of(), Boolean.class, false, NextOperation.FilesOnly
    );

    public static final String OPTSTR_FORMAT_ONLY_SHORT = "-f";
    public static final String OPTSTR_FORMAT_ONLY_LONG = "--format";
    private final Option<File> OPTION_FORMAT = new Option<>(
            new OptionString(OPTSTR_FORMAT_ONLY_SHORT, OPTSTR_FORMAT_ONLY_LONG),
            "Format the code", "Preprocesses and formats the code and outputs to the output file",
            List.of("output-file"), File.class, null, NextOperation.OutputFile
    );

    private final List<Option<?>> OPTIONS = List.of(
            OPTION_HELP,
            OPTION_VERBOSE,
            OPTION_DEBUG,
            OPTION_ANTLR_DEBUG,
            OPTION_COMPILE,
            OPTION_DIR,
            OPTION_FILETYPE,
            OPTION_STOP_OPTS,
            OPTION_FORMAT
    );

    private final Set<File> filesToProcess = new HashSet<>();

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

    public boolean formatOnly() {
        return OPTION_FORMAT.value() != null;
    }

    public Optional<String> compileTarget() {
        return Optional.ofNullable(OPTION_COMPILE.value());
    }

    public void setCompileTarget(String filename) {
        OPTION_COMPILE.setValue(Objects.requireNonNull(filename));
    }

    public void addFile(File file) {
        filesToProcess.add(Objects.requireNonNull(file));
    }

    public void addFiles(Set<File> files) {
        filesToProcess.addAll(files);
    }

    public void setFormatOutputFile(String filename) {
        OPTION_FORMAT.setValue(
                File.fromPath(Path.of(filename))
        );
    }

    public File formatOutputFile() {
        return OPTION_FORMAT.value();
    }

    public Set<File> filesToProcess() {
        return Collections.unmodifiableSet(filesToProcess);
    }

    @Override
    public Iterator<Option<?>> iterator() {
        return OPTIONS.iterator();
    }
}

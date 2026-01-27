package cz.mipit.sscc.args;

import cz.mipit.sscc.file.DirectoryTreeParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.cc.CCStandard;
import cz.mipit.sscc.util.ExitValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cz.mipit.sscc.Logger.err;
import static cz.mipit.sscc.Logger.warn;

public final class ArgumentParser {
    public static final String OPTSTR_STD_C = "--std=";
    public static final String OPTSTR_VERBOSE = "-v";
    public static final String OPTSTR_STOP_ON_ERROR = "-s";
    public static final String OPTSTR_DEBUG = "--debug";
    public static final String OPTSTR_HELP = "--help";
    public static final String OPTSTR_COMPILE = "--compile";
    public static final String OPTSTR_LIB = "--lib";

    private static final String HELP_STRING = """
            Usage: sscc [options|files]
            Options:
            """;

    private static final List<Option> OPTIONS = List.of(
            Option.of(OPTSTR_HELP, "Help", "Display this message", null),
            Option.of(OPTSTR_VERBOSE, "Verbose", "Print information about current stage", null),
            Option.of(OPTSTR_DEBUG, "Debug mode", "Print debug information (unstable)", null),
            Option.of(OPTSTR_STOP_ON_ERROR, "Stop on error", "Stops processing after encountering an error", null),
            Option.of(OPTSTR_STD_C, "Set C standard", "Sets the C standard for compilation/preprocessing", "c standard string (same as in cc)"),
            Option.of(OPTSTR_COMPILE + " ", "Compile", "Compiles the resulting C code into a binary", "name of the resulting binary"),
            Option.of(OPTSTR_LIB + " ", "Library", "Process all `.c` & `.ssc` files in a directory", "library path")
    );

    private static void printHelpAndExit() {
        System.out.print(HELP_STRING);
        for (Option option : OPTIONS) {
            option.print();
        }
        System.exit(0);
    }

    private enum NextOperation {None, CompileTarget, LibPath}

    public static SSCCOptions parse(String[] args) throws IOException {
        if (args.length == 0) {
            printHelpAndExit();
        }

        final Set<InputFile> filesToProcess = new HashSet<>();
        String compileTarget = null;
        boolean verbose = false;
        boolean printDebug = false;
        boolean stopOnError = true;
        CCStandard standard = SSCCOptions.DEF_C_STANDARD;

        NextOperation nextOperation = NextOperation.None;
        for (String arg : args) {
            nextOperation = switch (nextOperation) {
                case CompileTarget -> {
                    final Path asPath = Path.of(arg);
                    if (asPath.toFile().exists()) {
                        warn("File chosen as output already exists");
                    }
                    if (asPath.toFile().isDirectory()) {
                        err(ExitValue.INVALID_ARGUMENTS, "'" + arg + "' is a directory");
                    }
                    if (asPath.toString().split("\\.").length != 1) {
                        err(ExitValue.INVALID_ARGUMENTS, "Output file has a strange suffix");
                    }
                    compileTarget = arg;
                    yield NextOperation.None;
                }

                case LibPath -> {
                    DirectoryTreeParser.getPathsInDirectory(Path.of(arg), filesToProcess);
                    yield NextOperation.None;
                }

                case None -> {
                    if (!arg.startsWith("-")) {
                        final Path path = Path.of(arg);
                        final InputFile inputFile = verifyInputFilePath(path);

                        filesToProcess.add(inputFile);
                        yield NextOperation.None;
                    }

                    yield switch (arg) {
                        case OPTSTR_HELP -> {
                            printHelpAndExit();
                            throw new RuntimeException("Unreachable");
                        }
                        case OPTSTR_VERBOSE -> {
                            verbose = true;
                            yield NextOperation.None;
                        }
                        case OPTSTR_STOP_ON_ERROR -> {
                            stopOnError = false;
                            yield NextOperation.None;
                        }
                        case OPTSTR_DEBUG -> {
                            printDebug = true;
                            yield NextOperation.None;
                        }

                        case OPTSTR_COMPILE -> {
                            if (compileTarget != null) {
                                err(ExitValue.INVALID_ARGUMENTS, "Compile target already specified");
                            }
                            yield NextOperation.CompileTarget;
                        }

                        case "--lib" -> NextOperation.LibPath;

                        default -> {
                            if (arg.startsWith(OPTSTR_STD_C)) {
                                standard = CCStandard.fromString(arg.substring(OPTSTR_STD_C.length()));
                                yield NextOperation.None;
                            }

                            err(ExitValue.INVALID_ARGUMENTS, "Unknown option: " + arg);
                            throw new RuntimeException("Unreachable");
                        }
                    };
                }
            };
        }

        if (nextOperation != NextOperation.None) {
            err(ExitValue.INVALID_ARGUMENTS, "Missing argument for option '" + nextOperation + "'");
        }

        return new SSCCOptions(verbose, printDebug, stopOnError, compileTarget, filesToProcess, standard);
    }

    private static InputFile verifyInputFilePath(final Path path) {
        if (!Files.exists(path)) {
            err(ExitValue.INVALID_ARGUMENTS, "File '" + path + "' does not exist");
        }
        if (!Files.isRegularFile(path)) {
            err(ExitValue.INVALID_ARGUMENTS, "File '" + path + "' is not a regular file");
        }

        final InputFile inputFile = InputFile.fromAbsolutePath(path.toAbsolutePath());

        if (inputFile.suffix() == null) {
            err(ExitValue.INVALID_ARGUMENTS, "Could not verify type of file '" + inputFile.absolutePathString() + "'");
        }

        return inputFile;
    }


    private ArgumentParser() {
    }
}

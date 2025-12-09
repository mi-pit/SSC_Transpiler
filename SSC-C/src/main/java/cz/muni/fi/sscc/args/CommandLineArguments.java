package cz.muni.fi.sscc.args;

import cz.muni.fi.sscc.exceptions.ExitValue;
import cz.muni.fi.sscc.file.DirectoryTreeParser;
import cz.muni.fi.sscc.file.InputFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static cz.muni.fi.sscc.exceptions.ExitValue.err;
import static cz.muni.fi.sscc.exceptions.ExitValue.warn;

public class CommandLineArguments {
    private String compileTarget = null;
    private boolean verbose = false;
    private boolean printDebug = false;
    private final List<InputFile> filesToProcess = new ArrayList<>();
    private boolean stopOnError = false;
    private final Collection<InputFile> libraryPaths = new HashSet<>();

    public CommandLineArguments(String[] args) throws IOException {
        enum NextOperation {None, CompileTarget, LibPath}

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
                    final DirectoryTreeParser parser = new DirectoryTreeParser(Path.of(arg));
                    filesToProcess.addAll(parser.getFiles());
                    yield NextOperation.None;
                }

                case None -> {
                    if (arg.startsWith("-")) {
                        switch (arg) {
                            case "-v":
                                verbose = true;
                                break;

                            case "-s":
                                stopOnError = true;
                                break;

                            case "--debug":
                                printDebug = true;
                                break;

                            case "--compile":
                                if (compileTarget != null) {
                                    err(ExitValue.INVALID_ARGUMENTS, "Compile target already specified");
                                }
                                yield NextOperation.CompileTarget;

                            case "--lib":
                                yield NextOperation.LibPath;

                            default:
                                err(ExitValue.INVALID_ARGUMENTS, "Unknown option: " + arg);
                        }
                    } else {
                        filesToProcess.add(InputFile.fromAbsolutePath(Path.of(arg)));
                    }

                    yield NextOperation.None;
                }
            };
        }

        if (nextOperation != NextOperation.None) {
            err(ExitValue.INVALID_ARGUMENTS, "Missing option `" + nextOperation.name() + "` argument");
        }
    }

    public Optional<String> getCompileTarget() {
        return compileTarget == null ? Optional.empty() : Optional.of(compileTarget);
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isPrintDebug() {
        return printDebug;
    }

    public List<InputFile> getFilesToProcess() {
        return filesToProcess;
    }

    public boolean isStopOnError() {
        return stopOnError;
    }
}

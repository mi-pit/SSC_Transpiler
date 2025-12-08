package cz.muni.fi.sscc.args;

import cz.muni.fi.sscc.exceptions.ExitValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cz.muni.fi.sscc.exceptions.ExitValue.err;
import static cz.muni.fi.sscc.exceptions.ExitValue.warn;

public class CommandLineArguments {
    private String compileTarget;
    private boolean verbose;
    private boolean printDebug;
    private final List<InputFile> filesToProcess;
    private boolean stopOnError;

    public CommandLineArguments(String[] args) {
        filesToProcess = new ArrayList<>();
        verbose = false;
        printDebug = false;
        compileTarget = null;
        stopOnError = false;

        enum NextOperation {None, CompileTarget}

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

        if (nextOperation == NextOperation.CompileTarget) {
            err(ExitValue.INVALID_ARGUMENTS, "Missing compile target");
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

package cz.muni.fi.sscc.args;

import cz.muni.fi.sscc.exceptions.ExitValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static cz.muni.fi.sscc.exceptions.ExitValue.err;

public class CommandLineArguments {
    private String compileTarget;
    private boolean verbose;
    private boolean printDebug;
    private final List<InputFile> filesToProcess;

    public CommandLineArguments(String[] args) {
        filesToProcess = new ArrayList<>();
        verbose = false;
        printDebug = false;
        compileTarget = null;

        enum NextOperation {None, CompileTarget}

        NextOperation nextOperation = NextOperation.None;
        for (String arg : args) {
            nextOperation = switch (nextOperation) {
                case CompileTarget -> {
                    compileTarget = arg;
                    yield NextOperation.None;
                }

                case None -> {
                    if (arg.startsWith("-")) {
                        switch (arg) {
                            case "-v":
                                verbose = true;
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
}

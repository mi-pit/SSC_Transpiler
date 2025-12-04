package cz.muni.fi.sscc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommandLineArguments {
    private String compileTarget;
    private boolean verbose;
    private boolean printDebug;
    private final List<Path> filesToProcess;

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
                                    err(1, "Compile target already specified");
                                }
                                yield NextOperation.CompileTarget;

                            default:
                                err(1, "Unknown option: " + arg);
                        }
                    } else {
                        filesToProcess.add(Paths.get(arg));
                    }

                    yield NextOperation.None;
                }
            };
        }

        if (nextOperation == NextOperation.CompileTarget) {
            err(1, "Missing compile target");
        }
    }

    private static void err(int exitCode, String message) {
        System.err.println("SSC Transpiler: " + message);
        System.exit(exitCode);
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

    public List<Path> getFilesToProcess() {
        return filesToProcess;
    }
}

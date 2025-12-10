package cz.muni.fi.sscc.args;

import cz.muni.fi.sscc.exceptions.ExitValue;
import cz.muni.fi.sscc.file.DirectoryTreeParser;
import cz.muni.fi.sscc.file.InputFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cz.muni.fi.sscc.exceptions.ExitValue.err;
import static cz.muni.fi.sscc.exceptions.ExitValue.warn;

public class CommandLineArguments {
    private String compileTarget = null;
    private boolean verbose = false;
    private boolean printDebug = false;
    private final List<InputFile> filesToProcess = new ArrayList<>();
    private boolean stopOnError = true;

    private enum NextOperation {None, CompileTarget, LibPath}

    public CommandLineArguments(final String[] args) throws IOException {
        NextOperation nextOperation = NextOperation.None;
        for (String arg : args) {
            nextOperation = switch (nextOperation) {
                case CompileTarget -> getCompileTarget(arg);
                case LibPath -> getLibraryFiles(arg);
                case None -> getNextOperationFromNone(arg);
            };
        }

        if (nextOperation != NextOperation.None) {
            err(ExitValue.INVALID_ARGUMENTS, "Missing argument for option '" + nextOperation + "'");
        }
    }

    private NextOperation getLibraryFiles(final String arg) throws IOException {
        final DirectoryTreeParser parser = new DirectoryTreeParser(Path.of(arg));
        filesToProcess.addAll(parser.getFiles());
        return NextOperation.None;
    }

    private NextOperation getCompileTarget(String arg) {
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
        return NextOperation.None;
    }

    private NextOperation getNextOperationFromNone(final String arg) {
        if (!arg.startsWith("-")) {
            final Path path = Path.of(arg);
            if (!Files.exists(path)) {
                err(ExitValue.INVALID_ARGUMENTS, "File '" + arg + "' does not exist");
            }

            final InputFile inputFile = InputFile.fromAbsolutePath(path.toAbsolutePath());
            if (inputFile.suffix() == null) {
                err(ExitValue.INVALID_ARGUMENTS, "Could not verify type of file '" + arg + "'");
            }
            filesToProcess.add(inputFile);
            return NextOperation.None;
        }

        return processOption(arg);
    }

    private NextOperation processOption(final String arg) {
        return switch (arg) {
            case "-v" -> {
                verbose = true;
                yield NextOperation.None;
            }
            case "-s" -> {
                stopOnError = false;
                yield NextOperation.None;
            }
            case "--debug" -> {
                printDebug = true;
                yield NextOperation.None;
            }

            case "--compile" -> {
                if (compileTarget != null) {
                    err(ExitValue.INVALID_ARGUMENTS, "Compile target already specified");
                }
                yield NextOperation.CompileTarget;
            }

            case "--lib" -> NextOperation.LibPath;

            default -> {
                err(ExitValue.INVALID_ARGUMENTS, "Unknown option: " + arg);
                throw new RuntimeException("Unreachable");
            }
        };
    }

    public Optional<String> getCompileTarget() {
        return Optional.ofNullable(compileTarget);
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

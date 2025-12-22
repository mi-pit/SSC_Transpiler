package cz.mipit.sscc.args;

import cz.mipit.sscc.ExitValue;
import cz.mipit.sscc.file.DirectoryTreeParser;
import cz.mipit.sscc.file.InputFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static cz.mipit.sscc.ExitValue.err;
import static cz.mipit.sscc.ExitValue.warn;

public final class ArgumentParser {
    private ArgumentParser() {
    }

    private enum NextOperation {None, CompileTarget, LibPath}

    public static Options parse(String[] args) throws IOException {
        final List<InputFile> filesToProcess = new ArrayList<>();

        String compileTarget = null;
        boolean verbose = false;
        boolean printDebug = false;
        boolean stopOnError = true;

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
                    if (!arg.startsWith("-")) {
                        final Path path = Path.of(arg);
                        final InputFile inputFile = verifyInputFilePath(path);

                        filesToProcess.add(inputFile);
                        yield NextOperation.None;
                    }

                    yield switch (arg) {
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
            };
        }

        if (nextOperation != NextOperation.None) {
            err(ExitValue.INVALID_ARGUMENTS, "Missing argument for option '" + nextOperation + "'");
        }

        return new Options(verbose, printDebug, stopOnError, compileTarget, filesToProcess);
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
}

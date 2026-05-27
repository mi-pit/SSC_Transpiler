package cz.mipit.sscc.args;

import cz.mipit.sscc.Logger;
import cz.mipit.sscc.file.DirectoryTreeParser;
import cz.mipit.sscc.file.FileType;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.UnreachableCodeException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static cz.mipit.sscc.Logger.warn;
import static cz.mipit.sscc.args.SSCCOptions.OPTION_HELP;

public final class ArgumentParser {
    private final SSCCOptions options;

    private static final String HELP_STRING = """
            Usage: sscc [options|files]
            Options:    // output of options marked as "unstable" is subject to change
            """;

    private static void printHelpAndExit() {
        System.out.print(HELP_STRING);
        for (final Option<?> option : SSCCOptions.newWithDefaults()) {
            option.print();
        }
        System.exit(0);
    }

    public static SSCCOptions parse(String[] args) {
        if (args.length == 0) {
            printHelpAndExit();
        }
        final ArgumentParser argumentParser = new ArgumentParser();
        final SSCCOptions opts = argumentParser.parse_(args);

        opts.compileTarget().map(Path::of).ifPresent(path -> {
            if (Files.exists(path) && (opts.verbose() || opts.debug())) {
                warn("File chosen as output already exists");
            }
            if (Files.isDirectory(path)) {
                Logger.errExit(ExitValue.INVALID_ARGUMENTS, "'" + opts.compileTarget() + "' is a directory");
            }
        });

        return opts;
    }

    @SuppressWarnings("DuplicateExpressions") /* Path.of(arg) could throw if unverified */
    private SSCCOptions parse_(String[] args) {
        NextOperation nextOperation = NextOperation.None;
        FileType nextFileType = null;
        for (final String arg : args) {
            nextOperation = switch (nextOperation) {
                case FilesOnly -> {
                    options.addFile(verifyInputFilePath(Path.of(arg)));
                    yield NextOperation.FilesOnly;
                }

                case CompileTarget -> {
                    if (options.compileTarget().isPresent()) {
                        Logger.errExit(ExitValue.INVALID_ARGUMENTS, "Compile target is already set");
                    }
                    options.setCompileTarget(arg);
                    yield NextOperation.None;
                }

                case LibPath -> {
                    final Set<InputFile> inputFiles;
                    try {
                        inputFiles = DirectoryTreeParser.getFilesInDirectory(Path.of(arg), Set.of("ssc", "c"));
                    } catch (final IOException io) {
                        Logger.errExit(ExitValue.IO_EXCEPTION, io.getMessage());
                        throw new UnreachableCodeException();
                    }
                    options.addFiles(inputFiles);
                    yield NextOperation.None;
                }

                case FileType -> {
                    nextFileType = FileType.fromString(arg);
                    yield NextOperation.InputFile;
                }

                case InputFile -> {
                    final Path path = Path.of(arg);
                    final InputFile in = nextFileType == null
                            ? InputFile.fromPath(path)
                            : InputFile.fromPath(nextFileType, path);
                    nextFileType = null;

                    options.addFile(in);
                    yield NextOperation.None;
                }

                case None -> {
                    if (!arg.startsWith("-")) {
                        final Path path = Path.of(arg);
                        final InputFile inputFile = verifyInputFilePath(path);

                        options.addFile(inputFile);
                        yield NextOperation.None;
                    }
                    if (OPTION_HELP.matches(arg)) {
                        printHelpAndExit();
                    }
                    for (final Option<?> opt : options) {
                        if (opt.matches(arg)) {
                            if (opt.defaultValue() instanceof Boolean def) {
                                opt.setValue(!def);
                            }

                            yield opt.nextOperation;
                        }
                    }
                    Logger.errExit(ExitValue.INVALID_ARGUMENTS, "Unknown option: " + arg);
                    throw new AssertionError("Unreachable");
                }

                case OutputFile -> {
                    options.setFormatOutputFile(arg);
                    yield NextOperation.None;
                }
            };
        }

        if (nextOperation.requiresArgument()) {
            Logger.errExit(ExitValue.INVALID_ARGUMENTS, "Missing argument for option '" + nextOperation + "'");
        }

        return options;
    }

    private static InputFile verifyInputFilePath(final Path path) {
        if (!Files.exists(path)) {
            Logger.errExit(ExitValue.INVALID_ARGUMENTS, "File '" + path + "' does not exist");
        }
        if (!Files.isRegularFile(path)) {
            Logger.errExit(ExitValue.INVALID_ARGUMENTS, "File '" + path + "' is not a regular file");
        }

        final InputFile inputFile = InputFile.fromPath(path.toAbsolutePath());

        if (inputFile.suffix() == null) {
            Logger.errExit(ExitValue.INVALID_ARGUMENTS, "Could not verify type of file '" + inputFile.absolutePathString() + "'");
        }

        return inputFile;
    }

    private ArgumentParser() {
        options = SSCCOptions.newWithDefaults();
    }
}

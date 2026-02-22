package cz.mipit.sscc.args;

import cz.mipit.sscc.Logger;
import cz.mipit.sscc.file.DirectoryTreeParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.util.ExitValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static cz.mipit.sscc.Logger.warn;
import static cz.mipit.sscc.args.SSCCOptions.OPTION_HELP;

public final class ArgumentParser {
    private final SSCCOptions options;

    private static final String PROGNAME =
            Objects.requireNonNullElse(System.getProperty("sun.java.command"), "sscc");

    private static final String HELP_STRING =
            "Usage: " + PROGNAME + " [options|files]" + System.lineSeparator()
                    + "Options:";

    private static void printHelpAndExit() {
        System.out.print(HELP_STRING);
        for (final Option<?> option : SSCCOptions.withDefaults()) {
            option.print();
        }
        System.exit(0);
    }

    public static SSCCOptions parse(String[] args) throws IOException {
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
    private SSCCOptions parse_(String[] args) throws IOException {
        NextOperation nextOperation = NextOperation.None;
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
                    options.addFiles(DirectoryTreeParser.getPathsInDirectory(Path.of(arg)));
                    yield NextOperation.None;
                }

                case None -> {
                    if (!arg.startsWith("-")) {
                        final Path path = Path.of(arg);
                        final InputFile inputFile = verifyInputFilePath(path);

                        options.addFile(inputFile);
                        yield NextOperation.None;
                    }
                    if (OPTION_HELP.strings.matches(arg)) {
                        printHelpAndExit();
                    }
                    for (final Option<?> opt : options) {
                        final OptionString optstr = opt.strings;
                        if (optstr.matches(arg)) {
                            if (opt.defaultValue instanceof Boolean def) {
                                opt.setValue(!def);
                            }

                            yield opt.nextOperation;
                        }
                    }
                    Logger.errExit(ExitValue.INVALID_ARGUMENTS, "Unknown option: " + arg);
                    throw new AssertionError("Unreachable");
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

        final InputFile inputFile = InputFile.fromAbsolutePath(path.toAbsolutePath());

        if (inputFile.suffix() == null) {
            Logger.errExit(ExitValue.INVALID_ARGUMENTS, "Could not verify type of file '" + inputFile.absolutePathString() + "'");
        }

        return inputFile;
    }

    private ArgumentParser() {
        options = SSCCOptions.withDefaults();
    }
}

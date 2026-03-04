package cz.mipit.sscc.ssc.compiler;

import cz.mipit.sscc.Logger;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.Compiler;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.SuperstructConvertorVisitor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.VisitorData;
import cz.mipit.sscc.util.collection.builder.ListBuilder;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static cz.mipit.sscc.Logger.errReturn;
import static cz.mipit.sscc.Logger.warn;
import static cz.mipit.sscc.Main.logger;

public final class SSCCompiler implements Compiler {
    public static final Path SSCLIB_HOME;

    static {
        final String ssclibHomeEnv = System.getenv("SSCLIB_HOME");
        if (ssclibHomeEnv == null) {
            Logger.errExit(ExitValue.LIBRARY_NOT_FOUND, "SSCLIB_HOME not set");
            throw new AssertionError("unreachable");
        }

        final Path asPath = Path.of(ssclibHomeEnv);

        if (!Files.exists(asPath)) {
            Logger.errExit(ExitValue.LIBRARY_NOT_FOUND, "file doesn't exist: " + ssclibHomeEnv);
        }

        if (!Files.isDirectory(asPath)) {
            Logger.errExit(ExitValue.LIBRARY_NOT_FOUND, "not a directory: " + ssclibHomeEnv);
        }

        SSCLIB_HOME = asPath;
    }

    private final SSCCOptions options;

    private static final List<String> CC_OPTIONS = List.of(
            "-Wall",
            "-Wextra",

            "-Wno-extra-semi",      /* sscc creates extra semicolons */
            "-Wno-unused-function", /* preprocessor includes unused functions */

            "-Werror"
    );

    private final List<String> ccProcessArgBase;

    private InputFile currentFile = null;

    public SSCCompiler(final SSCCOptions options) {
        this.options = options;

        final ListBuilder<String> cc = ListBuilder
                .from("cc")
                .plus("-I" + SSCLIB_HOME + "/include/")
                .plusMany(CC_OPTIONS)
                .plus("--std=c2x");

        ccProcessArgBase = cc.build();
    }

    public ExitValue run() throws IOException, InterruptedException, SSCTranspilerException {
        if (options.filesToProcess().isEmpty()) {
            return errReturn(ExitValue.INVALID_ARGUMENTS, "No files given to process");
        }

        final Set<Path> outputtedFiles = new HashSet<>();
        final Set<Path> filesToCompile = new HashSet<>();

        final int totalFailed = goThroughAllFiles(filesToCompile, outputtedFiles);
        if (totalFailed != 0) {
            return errReturn(ExitValue.TRANSPILATION_FAIL, "Could not process " + totalFailed + " file(s)");
        }

        if (options.compileTarget().isPresent()) {
            logger.printVerbose("Compiling...");
            if (!compileCBatch(options.compileTarget().get(), filesToCompile)) {
                return ExitValue.C_COMPILATION_FAIL;
            }

            for (final Path path : outputtedFiles) {
                logger.printVerboseFilename("Deleting output file", path.toString());
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    warn("Could not delete file '" + path + "'");
                }
            }
        }
        logger.printVerbose("Successfully processed.");
        return ExitValue.SUCCESS;
    }

    private int goThroughAllFiles(final Set<Path> filesToCompile,
                                  final Set<Path> outputtedFiles)
            throws IOException, InterruptedException {
        int totalFailed = 0;
        final Set<InputFile> filesToProcess = options.filesToProcess();
        for (final InputFile fileArg : filesToProcess) {
            if (!"ssc".equals(fileArg.suffix())) {
                handleNonSSCFiles(fileArg, filesToCompile);
                continue;
            }

            try {
                currentFile = fileArg;
                final Optional<Path> processed = transpileFile(fileArg);
                if (processed.isEmpty()) {
                    totalFailed++;
                    if (options.stopOnError()) {
                        logger.printVerbose("Stopping.");
                        break;
                    }
                } else {
                    final Path file = processed.get();
                    outputtedFiles.add(file);
                    filesToCompile.add(file);
                }
            } catch (RuntimeException e) {
                handleKnownExceptionsOrRethrow(e);

                totalFailed++;
                if (options.stopOnError()) {
                    logger.printVerbose("Stopping.");
                    break;
                }
            } finally {
                logger.printVerboseFilename("Processed", fileArg.absolutePathString());
            }
        }
        return totalFailed;
    }

    private void handleNonSSCFiles(final InputFile fileArg,
                                   final Set<Path> filesToCompile) {
        logger.printDebug(() -> "Skipping transpilation of file '"
                + fileArg.absolutePathString()
                + "' (not an ssc file)");
        filesToCompile.add(fileArg.toAbsolutePath());
    }

    private void handleKnownExceptionsOrRethrow(final RuntimeException exception) throws RuntimeException {
        if (exception instanceof SSCTranspilerException e) {
            System.err.println(e.getMessage());
        } else {
            throw exception; /* doesn't get caught again */
        }
    }

    private Optional<Path> transpileFile(final InputFile inputFile)
            throws IOException, InterruptedException {
        logger.printVerboseFilename("Processing file", inputFile.absolutePathString());

        final InputFile workingFile = inputFile.getChangedSuffix("c");
        final Path workingFileAbsolutePath = workingFile.toAbsolutePath();

        logger.printVerbose("Preprocessing file...");
        if (!preprocessSSCCode(inputFile, workingFileAbsolutePath)) {
            logger.printVerbose("Preprocessing failed.");
            return Optional.empty();
        }

        logger.printVerbose("Parsing preprocessed code...");
        final VisitorData data = VisitorData.fromFile(workingFile);

        logger.printVerbose("Extracting superstructs...");
        if (!extractSuperstructMembers(data.tokens(), data.tree(), workingFileAbsolutePath)) {
            logger.printVerbose("Failed to extract superstructs.");
            return Optional.empty();
        }

        if (options.compileTarget().isPresent()) {
            /* don't verify if you're going to compile the files anyway */
            return Optional.of(workingFileAbsolutePath);
        }

        logger.printVerbose("Verifying...");
        final int exitCode = verifyCCode(workingFileAbsolutePath);
        if (exitCode != 0) {
            logger.printVerbose("Verification failed with exit code: " + exitCode);
            return Optional.empty();
        }

        return Optional.of(workingFileAbsolutePath);
    }

    private boolean extractSuperstructMembers(final CommonTokenStream tokens,
                                              final ParseTree tree,
                                              final Path outputFile)
            throws IOException {
        final SuperstructConvertorVisitor visitor = new SuperstructConvertorVisitor(tokens, currentFile);
        final String result = visitor.visit(tree);
        if (options.debug()) {
            for (var entry : visitor.functionVariables.entrySet()) {
                final String funcName = entry.getKey();
                final Set<SuperstructVariable> variables = entry.getValue();
                if (variables.isEmpty()) {
                    continue;
                }

                Main.logger.printDebug("For scope " + (funcName == null ? "global" : "'" + funcName + "'"));
                for (final SuperstructVariable variable : variables) {
                    Main.logger.printDebug("        " + variable);
                }
            }
        }

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);

        return visitor.hasNoErrors();
    }

    private static int doProcess(final List<String> args)
            throws IOException, InterruptedException {
        logger.printDebug(() -> String.join(" ", args));
        return new ProcessBuilder(args).inheritIO().start().waitFor();
    }

    private static final String SSC_DEF_MACRO_STRING_NAME = "__SSC_SOURCE__";

    private boolean preprocessSSCCode(final InputFile inFile,
                                      final Path outputFile)
            throws IOException, InterruptedException {
        return 0 == doProcess(ListBuilder
                .from(ccProcessArgBase)
                .plusMany(
                        "-E",
                        "-D" + SSC_DEF_MACRO_STRING_NAME,
                        "-Davailability(...)=", // TODO? remove
                        "-x", "c", inFile.absolutePathString(),
                        "-o", outputFile.toString()
                )
                .build()
        );
    }

    private int verifyCCode(final Path file) throws IOException, InterruptedException {
        return doProcess(ListBuilder
                .from(ccProcessArgBase)
                .plus("-fsyntax-only")
                .plus(file.toString())
                .build()
        );
    }

    private boolean compileCBatch(String binaryName, Set<Path> files)
            throws IOException, InterruptedException {
        final ListBuilder<String> argsBuilder = ListBuilder
                .from(ccProcessArgBase)
                .plusMapped(files, Path::toString)

                .plus("-o")
                .plus(binaryName)

                .plus("-L" + SSCLIB_HOME + "/dylib/")
                .plus("-lssclib")
                .plus("-Wl,-rpath," + SSCLIB_HOME + "/dylib/");
        if (options.debug()) {
            argsBuilder.plus("-v");
        }
        //                .add("-fsanitize=address")
        //                .add("-fsanitize=undefined")
        //                .add("-fsanitize=integer")

        final List<String> args = argsBuilder.build();

        logger.printDebug("Compiling using `%s`", String.join(" ", args));

        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        final int exitCode = doProcess(args);
        if (exitCode != 0) {
            errReturn(ExitValue.C_COMPILATION_FAIL, "Compilation failed with exit code: " + exitCode);
            return false;
        }
        return true;
    }
}

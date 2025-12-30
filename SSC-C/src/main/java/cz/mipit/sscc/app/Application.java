package cz.mipit.sscc.app;

import cz.mipit.sscc.args.ArgumentParser;
import cz.mipit.sscc.args.Options;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.SuperStruct;
import cz.mipit.sscc.ssc.compiler.visitors.PostfixExpressionConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.SSCConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.SuperstructConvertorVisitor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.preprocessor.Preprocessor;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.ListBuilder;
import cz.mipit.sscc.util.VisitorData;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static cz.mipit.sscc.Main.logger;
import static cz.mipit.sscc.util.ExitValue.err;
import static cz.mipit.sscc.util.ExitValue.warn;

public final class Application {
    private final Set<SuperStruct> sss = new HashSet<>();
    private final Options options;

    public Options getOptions() {
        return options;
    }

    private static final List<String> CC_OPTIONS = List.of(
            "-Werror",
            "-Wall",
            "-Wextra",
            "-Wno-extra-semi", /* transpiler creates extra semicolons */

            "--std=c2x" // todo: add option
    );

    public Application(final String[] args) throws IOException {
        options = ArgumentParser.parse(args);
    }

    public void run() throws IOException, InterruptedException {
        processFiles(options.filesToProcess());
    }

    private void processFiles(final List<InputFile> files)
            throws IOException, InterruptedException {
        if (files.isEmpty()) {
            err(ExitValue.INVALID_ARGUMENTS, "No files given to process");
        }

        final Set<Path> outputtedFiles = new HashSet<>();
        final Set<Path> filesToCompile = new HashSet<>();
        final int totalFailed = goThroughAllFiles(files, filesToCompile, outputtedFiles);

        if (totalFailed != 0) {
            err(ExitValue.TRANSPILATION_FAIL, "Could not process " + totalFailed + " file(s)");
            return;
        }

        if (options.compileTargetFilename().isPresent()) {
            logger.printVerbose("Compiling...");
            compileCBatch(options.compileTargetFilename().get(), filesToCompile);

            outputtedFiles.forEach(path -> {
                logger.printVerbose("Trying to delete output file '" + path + "'...");
                try {
                    Files.delete(path);
                    logger.printVerbose("    success");
                } catch (IOException e) {
                    warn("Could not delete file '" + path + "'");
                }
            });
        }
        logger.printVerbose("Successfully processed.");
    }

    private int goThroughAllFiles(List<InputFile> files,
                                  Set<Path> filesToCompile,
                                  Set<Path> outputtedFiles)
            throws IOException, InterruptedException {
        int totalFailed = 0;
        for (final InputFile fileArg : files) {
            if (!"ssc".equals(fileArg.suffix())) {
                handleNonSSCFiles(fileArg, filesToCompile);
                continue;
            }

            try {
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
                logger.printVerbose("Processed '" + fileArg.absolutePathString() + "'");
                sss.clear();
            }
        }
        return totalFailed;
    }

    private void handleNonSSCFiles(final InputFile fileArg,
                                   final Set<Path> filesToCompile) {
        logger.printDebug("Skipping transpilation of file '"
                + fileArg.absolutePathString()
                + "' (not an ssc file)");
        filesToCompile.add(fileArg.toAbsolutePath());
    }

    private void handleKnownExceptionsOrRethrow(final RuntimeException exception) throws RuntimeException {
        if (exception instanceof SSCTranspilerException) {
            System.err.println(exception.getMessage());
        } else {
            throw exception; /* doesn't get caught again */
        }
    }

    private Optional<Path> transpileFile(final InputFile inputFile)
            throws IOException, InterruptedException {
        logger.printVerbose("Processing file: ", inputFile.absolutePathString());

        final Path workingFileAbsolutePath = inputFile.getChangedSuffix("c").toAbsolutePath();

        logger.printVerbose("Preprocessing file...");
        if (!Preprocessor.preprocessSSC(inputFile, workingFileAbsolutePath)) {
            logger.printVerbose("Preprocessing failed.");
            return Optional.empty();
        }

        {
            final VisitorData data = VisitorData.fromFile(workingFileAbsolutePath);

            logger.printVerbose("Extracting superstructs...");
            if (!extractSuperstructMembers(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to extract superstructs.");
                return Optional.empty(); // Error nodes encountered
            }
        }
        {
            final VisitorData data = VisitorData.fromFile(workingFileAbsolutePath);

            logger.printVerbose("Replacing superstruct references...");
            if (!replaceSuperstructCalls(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to replace superstruct references.");
                return Optional.empty();
            }
        }

        if (options.compileTargetFilename().isPresent()) {
            /* don't format if you're going to delete the files anyway;
             * don't verify if you're going to compile the files anyway */
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
        final SuperstructConvertorVisitor visitor = new SuperstructConvertorVisitor(tokens);
        final String result = visitor.visit(tree);
        sss.addAll(visitor.getSuperStructs());

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);

        return visitor.hasNoErrors();
    }

    private boolean replaceSuperstructCalls(final CommonTokenStream tokens,
                                            final ParseTree tree,
                                            final Path outputFile)
            throws IOException {
        final SSCConvertorVisitor visitor = new PostfixExpressionConvertorVisitor(tokens, sss);
        final String result = visitor.visit(tree) + "\n";

        Files.writeString(outputFile, result,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        return visitor.hasNoErrors();
    }

    private static int doProcess(final List<String> args)
            throws IOException, InterruptedException {
        return new ProcessBuilder(args).inheritIO().start().waitFor();
    }

    private static int verifyCCode(final Path file) throws IOException, InterruptedException {
        return doProcess(ListBuilder
                .from("cc")
                .addAll(CC_OPTIONS)
                .add("-fsyntax-only")
                .add(file.toString())
                .build()
        );
    }

    private static void compileCBatch(String binaryName, Collection<Path> files)
            throws IOException, InterruptedException {
        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */

        final int exitCode = doProcess(ListBuilder
                .from("cc")
                .addAll(CC_OPTIONS)
                .add("-o")
                .add(binaryName)
                .addMapped(files, Path::toString)
                .build()
        );
        if (exitCode != 0) {
            err(ExitValue.C_COMPILATION_FAIL, "Compilation failed with exit code: " + exitCode);
        }
    }
}

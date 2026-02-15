package cz.mipit.sscc.ssc.compiler;

import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.DirectoryTreeParser;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.Processor;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.visitors.ExpressionConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.SSCConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.SuperstructConvertorVisitor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.ListBuilder;
import cz.mipit.sscc.util.VisitorData;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static cz.mipit.sscc.Logger.errNoExit;
import static cz.mipit.sscc.Main.logger;
import static cz.mipit.sscc.Logger.warn;

public final class Compiler implements Processor {
    public static final Path SSCLIB_HOME = DirectoryTreeParser.getLibraryRoot();

    private final Set<SuperStruct> sss = new HashSet<>();

    private final SSCCOptions options;

    private static final List<String> CC_OPTIONS = List.of(
            "-Wall",
            "-Wextra",

            "-Wno-extra-semi",  /* transpiler creates extra semicolons */
            "-Wno-unused",      /* Fixme: remove after implementing preprocessor */

            "-Werror"
    );

    private final List<String> ccProcessArgBase;

    private InputFile currentFile = null;

    public Compiler(final SSCCOptions options) {
        this.options = options;

        final ListBuilder<String> cc = ListBuilder
                .from("cc")
                .add("-I" + SSCLIB_HOME + "/include/")
                .addAll(CC_OPTIONS)
                .add(options.cStandard().ccOptionString());

        ccProcessArgBase = cc.build();
    }

    public ExitValue run() throws IOException, InterruptedException, SSCTranspilerException {
        if (options.filesToProcess().isEmpty()) {
            return errNoExit(ExitValue.INVALID_ARGUMENTS, "No files given to process");
        }

        final Set<Path> outputtedFiles = new HashSet<>();
        final Set<Path> filesToCompile = new HashSet<>();

        final int totalFailed = goThroughAllFiles(filesToCompile, outputtedFiles);
        if (totalFailed != 0) {
            return errNoExit(ExitValue.TRANSPILATION_FAIL, "Could not process " + totalFailed + " file(s)");
        }

        if (options.compileTargetFilename().isPresent()) {
            logger.printVerbose("Compiling...");
            if (!compileCBatch(options.compileTargetFilename().get(), filesToCompile)) {
                return ExitValue.C_COMPILATION_FAIL;
            }

            for (final Path path : outputtedFiles) {
                logger.printVerbose("Trying to delete output file '" + path + "'...");
                try {
                    Files.delete(path);
                    logger.printVerbose("    success");
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
        if (exception instanceof SSCTranspilerException e) {
            System.err.println(e.getMessage());
        } else {
            throw exception; /* doesn't get caught again */
        }
    }

    private Optional<Path> transpileFile(final InputFile inputFile)
            throws IOException, InterruptedException {
        logger.printVerboseFilename("Processing file: ", inputFile.absolutePathString());

        final InputFile workingFile = inputFile.getChangedSuffix("c");
        final Path workingFileAbsolutePath = workingFile.toAbsolutePath();

        logger.printVerbose("Preprocessing file...");
        if (!preprocessSSCCode(inputFile, workingFileAbsolutePath)) {
            logger.printVerbose("Preprocessing failed.");
            return Optional.empty();
        }

        {
            final VisitorData data = VisitorData.fromFile(workingFile);

            logger.printVerbose("Extracting superstructs...");
            if (!extractSuperstructMembers(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to extract superstructs.");
                return Optional.empty();
            }
        }
        {
            final VisitorData data = VisitorData.fromFile(workingFile);

            logger.printVerbose("Replacing superstruct references...");
            if (!replaceSuperstructCalls(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to replace superstruct references.");
                return Optional.empty();
            }
        }

        if (options.debug()) {
            for (var ss : sss) {
                logger.printDebug(ss.toString());
            }
        }

        if (options.compileTargetFilename().isPresent()) {
            /* don't verify if you're going to compile the files anyway */
            return Optional.of(workingFileAbsolutePath);
        }

        if (options.debug()) {
            doProcess(List.of(
                    "/opt/homebrew/bin/clang-format",
                    "-i",
                    workingFileAbsolutePath.toString())
            );
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
        sss.addAll(visitor.getSuperStructs());

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);

        return visitor.hasNoErrors();
    }

    private boolean replaceSuperstructCalls(final CommonTokenStream tokens,
                                            final ParseTree tree,
                                            final Path outputFile)
            throws IOException {
        final SSCConvertorVisitor visitor = new ExpressionConvertorVisitor(tokens, sss, currentFile);
        final String result = visitor.visit(tree) + "\n";

        try (final var bw = Files.newBufferedWriter(outputFile, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write(result);
            bw.flush();
        }

        return visitor.hasNoErrors();
    }

    private static int doProcess(final List<String> args)
            throws IOException, InterruptedException {
        logger.printDebug(args.toString());
        return new ProcessBuilder(args).inheritIO().start().waitFor();
    }

    private static final String SSC_DEF_MACRO_STRING_NAME = "__SSC_SOURCE__";

    private boolean preprocessSSCCode(final InputFile inFile,
                                      final Path outFileAbsolute)
            throws IOException, InterruptedException {
//        final Processor preprocessor = new Preprocessor(inFile, outFileAbsolute);
//        if (preprocessor.run().isFailure()) {
//            return false;
//        }

        final Path tempOut = Files.createTempFile(inFile.dir(), inFile.getFullName(), ".i");

        final int exitCode = doProcess(ListBuilder
                .from(ccProcessArgBase)
                .addAll(
                        "-E",
                        "-P",
                        "-D" + SSC_DEF_MACRO_STRING_NAME,
                        "-x", "c",
                        inFile.absolutePathString(),
                        "-o", tempOut.toString()
                )
                .build()
        );

        if (exitCode != 0) {
            Files.deleteIfExists(tempOut);
            return false;
        }

        Files.move(tempOut, outFileAbsolute, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private int verifyCCode(final Path file) throws IOException, InterruptedException {
        return doProcess(ListBuilder
                .from(ccProcessArgBase)
                .add("-fsyntax-only")
                .add(file.toString())
                .build()
        );
    }

    private boolean compileCBatch(String binaryName, Set<Path> files)
            throws IOException, InterruptedException {
        final ListBuilder<String> argsBuilder = ListBuilder
                .from(ccProcessArgBase)
                .addMapped(files, Path::toString)

                .add("-o")
                .add(binaryName)

                .add("-fsanitize=address")
                .add("-fsanitize=undefined")
                .add("-fsanitize=integer")

                .add("-L" + SSCLIB_HOME + "/dylib/")
                .add("-lssclib")
                .add("-Wl,-rpath," + SSCLIB_HOME + "/dylib/");
        if (options.debug()) {
            argsBuilder.add("-v");
        }
        final List<String> args = argsBuilder.build();

        logger.printVerbose("Compiling using `%s`", String.join(" ", args));

        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        final int exitCode = doProcess(args);
        if (exitCode != 0) {
            errNoExit(ExitValue.C_COMPILATION_FAIL, "Compilation failed with exit code: " + exitCode);
            return false;
        }
        return true;
    }
}

package cz.mipit.sscc.ssc.compiler;

import cz.mipit.sscc.Logger;
import cz.mipit.sscc.args.SSCCOptions;
import cz.mipit.sscc.file.File;
import cz.mipit.sscc.file.FileType;
import cz.mipit.sscc.ssc.Compiler;
import cz.mipit.sscc.ssc.compiler.visitors.BaseConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.ssc.compiler.visitors.fmt.FormattingConvertor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.exceptions.children.AntlrException;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.VisitorInput;
import cz.mipit.sscc.util.collection.builder.ListBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static cz.mipit.sscc.Logger.errReturn;
import static cz.mipit.sscc.Main.logger;

public final class SSCCompiler implements Compiler {
    // TODO? define this to be the date value of the last commit
    private static final String SSC_DEF_MACRO_STRING_NAME = "__SSC_SOURCE__";

    public static final Path SSCLIB_HOME;

    static {
        final String sscLibHomeEnv = System.getenv("SSCLIB_HOME");
        if (sscLibHomeEnv == null) {
            Logger.errExit(ExitValue.LIBRARY_NOT_FOUND, "SSCLIB_HOME not set");
            throw new AssertionError("unreachable");
        }

        final Path asPath = Path.of(sscLibHomeEnv);

        if (!Files.exists(asPath)) {
            Logger.errExit(ExitValue.LIBRARY_NOT_FOUND, "file doesn't exist: " + sscLibHomeEnv);
        }

        if (!Files.isDirectory(asPath)) {
            Logger.errExit(ExitValue.LIBRARY_NOT_FOUND, "not a directory: " + sscLibHomeEnv);
        }

        assert asPath != null;
        SSCLIB_HOME = asPath;
    }

    private final SSCCOptions options;

    private static final List<String> CC_OPTIONS = List.of(
            "-Wall",
            "-Wextra",
            "-pedantic",

            /* stdlib contains platform specific code */
            "-Wno-nullability-extension",
            /* and unused functions */
            "-Wno-unused-function",

            /* Things like `object Template<void *> var = {};`
             * turns into
             * struct (Template__void*); // <- semicolon inserted; needed
             * struct (Template__void*) var = {};
             *
             * while `object NotATemplate { ... };`
             * turns into
             * struct NotATemplate { ... };; // <- semicolon inserted; extraneous
             */
            "-Wno-extra-semi",

            "-Werror"
    );

    private final List<String> ccProcessArgBase;

    public SSCCompiler(final SSCCOptions options) {
        this.options = options;

        final ListBuilder<String> cc = ListBuilder
                .with("cc")
                .plus("-I" + SSCLIB_HOME + "/include/")
                .plusMany(CC_OPTIONS)
                .plus("--std=c2x");

        ccProcessArgBase = cc.build();
    }

    private static Stream<File> getSimpleFileStream(Set<File> files) {
        return files.stream();
    }

    private static Stream<File> getParallelFileStream(Set<File> files) {
        return files.parallelStream();
    }

    public ExitValue run() throws IOException, InterruptedException, SSCTranspilerException {
        if (options.filesToProcess().isEmpty()) {
            return errReturn(ExitValue.INVALID_ARGUMENTS, "No files given to process");
        }

        final Set<Path> filesToCompile = ConcurrentHashMap.newKeySet();
        // files to be deleted if binary is produced
        final Set<Path> outputtedFiles = ConcurrentHashMap.newKeySet();

        final int totalFailed = goThroughAllFiles(filesToCompile, outputtedFiles);
        if (totalFailed != 0) {
            return errReturn(ExitValue.TRANSPILATION_FAIL, "Could not process " + totalFailed + " file(s)");
        }

        if (options.compileTarget().isPresent()) {
            logger.printVerbose("Compiling...");
            final int exitCode = compileCBatch(options.compileTarget().get(), filesToCompile);
            if (exitCode != 0) {
                return errReturn(ExitValue.C_COMPILATION_FAIL, "Compilation failed with exit code: " + exitCode);
            }

            for (final Path path : outputtedFiles) {
                logger.printVerboseFilename("Deleting output file", path.toString());
                Files.delete(path);
            }
        }
        logger.printVerbose("Successfully processed.");
        return ExitValue.SUCCESS;
    }

    private static Stream<File> getFileStream(SSCCOptions options) {
        if (options.debug())
            return getSimpleFileStream(options.filesToProcess());

        return getParallelFileStream(options.filesToProcess());
    }

    /**
     * Input sets must support concurrency
     *
     * @return number of files where processing failed
     */
    private int goThroughAllFiles(final Set<Path> filesToCompile,
                                  final Set<Path> outputtedFiles) {
        final AtomicInteger totalFailed = new AtomicInteger();

        getFileStream(options).forEach(fileArg -> {
            if (fileArg.getFileType() != FileType.SSC) {
                logger.printVerboseFilename("Skipping processing of file", fileArg.fullName());
                filesToCompile.add(fileArg.toAbsolutePath());
                return;
            }

            final File outputFile = options.formatOnly()
                    ? options.formatOutputFile()
                    : fileArg.getChangedSuffix("c");

            try {
                final Optional<Path> processed = transpileFile(fileArg, outputFile);
                if (processed.isPresent()) {
                    final Path file = processed.get();
                    outputtedFiles.add(file);
                    filesToCompile.add(file);
                } else {
                    totalFailed.getAndIncrement();
                }
            } catch (RuntimeException | IOException | InterruptedException e) {
                totalFailed.getAndIncrement();

                final boolean shouldPrintStackTrace = options.verbose() || options.debug();
                Logger.errReturn(
                        ExitValue.INTERNAL_ERROR,
                        "Caught exception while processing file '%s'%s",
                        fileArg.fullName(),
                        (shouldPrintStackTrace ? "" : " (run with verbose or debug option to see stack trace)")
                );

                if (shouldPrintStackTrace) {
                    e.printStackTrace(System.err);
                }
            } finally {
                logger.printVerboseFilename("Processed", fileArg.absolutePathString());
            }
        });
        return totalFailed.get();
    }

    private Optional<Path> transpileFile(final File file,
                                         final File workingFile)
            throws IOException, InterruptedException {
        logger.printVerboseFilename("Processing file", file.absolutePathString());

        final Path workingFileAbsolutePath = workingFile.toAbsolutePath();

        logger.printVerbose("Preprocessing file...");
        if (!preprocessSSCCode(file, workingFileAbsolutePath)) {
            logger.printVerbose("Preprocessing failed.");
            return Optional.empty();
        }

        logger.printVerbose("Parsing preprocessed code...");
        final SequencedCollection<AntlrException> exceptions = new LinkedList<>();
        final VisitorInput data = VisitorInput.fromFile(workingFile, exceptions);
        if (!exceptions.isEmpty()) {
            exceptions.forEach(logger::printException);
            logger.printVerbose("Could not parse code.");
            return Optional.empty();
        }

        logger.printVerbose("Processing parsed SSC code...");
        if (!processSSCCode(data, workingFileAbsolutePath)) {
            logger.printVerbose("Failed to process SSC code.");
            return Optional.empty();
        }

        if (options.compileTarget().isPresent()) {
            /* don't verify if you're going to compile the files anyway */
            return Optional.of(workingFileAbsolutePath);
        }

        if (options.formatOnly()) {
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

    private boolean processSSCCode(final VisitorInput data,
                                   final Path outputFile)
            throws IOException {
        final BaseConvertorVisitor visitor = options.formatOnly()
                ? new FormattingConvertor(data.tokens(), data.file())
                : new VisitorDispatcher(data);

        String result = visitor.visit(data.tree()) + "\n";
        if (options.formatOnly()) {
            result = result
                    .lines()
                    .map(line -> line.stripTrailing())
                    .collect(Collectors.joining("\n"));
        }

        if (options.debug()) {
            visitor.debugPrintDump();
        }

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);

        return visitor.hasNoErrors();
    }

    private boolean preprocessSSCCode(final File inFile,
                                      final Path outputFile)
            throws IOException, InterruptedException {
        return 0 == doProcess(ListBuilder
                .from(ccProcessArgBase)
                .plusMany(
                        "-E",
                        "-D" + SSC_DEF_MACRO_STRING_NAME,
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

    private int compileCBatch(String binaryName, Set<Path> files)
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

        final List<String> args = argsBuilder.build();

        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        return doProcess(args);
    }

    public synchronized static int doProcess(final List<String> args)
            throws IOException, InterruptedException {
        logger.printDebug(() -> "Creating a new process: " + String.join(" ", args));
        return new ProcessBuilder(args)
                .inheritIO()
                .start()
                .waitFor();
    }
}

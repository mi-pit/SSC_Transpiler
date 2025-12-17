package cz.mipit.sscc;

import antlr.SSCLexer;
import antlr.SSCParser;
import cz.mipit.sscc.args.CommandLineArguments;
import cz.mipit.sscc.ssc.compiler.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.compiler.exceptions.UnknownTranspilationException;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.SuperStruct;
import cz.mipit.sscc.util.ListBuilder;
import cz.mipit.sscc.ssc.compiler.visitors.ConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.PostfixExpressionConvertorVisitor;
import cz.mipit.sscc.ssc.compiler.visitors.SuperstructConvertorVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static cz.mipit.sscc.ExitValue.err;
import static cz.mipit.sscc.ExitValue.warn;

public final class Main {
    private Main() {
    }

    private static final String SSC_DEF_MACRO_STRING = "__SSC_LANGUAGE__";

    private static Collection<SuperStruct> sss;
    private static CommandLineArguments parsedArgs;
    public static Logger logger;

    private static final List<String> CC_OPTIONS = List.of(
            "--std=c2x", // todo: add option
            "-Werror",
            "-Wall",
            "-Wextra",
            "-Wno-extra-semi",      /* transpiler creates extra semicolons */
            "-Wno-unused-function"  /* preprocessor extracts "static" functions */
    );

    public static void main(String[] args)
            throws IOException, InterruptedException {
        parsedArgs = new CommandLineArguments(args);
        logger = new Logger(parsedArgs);

        processFiles(parsedArgs.getFilesToProcess());
    }

    private static void processFiles(final List<InputFile> files)
            throws IOException, InterruptedException {
        if (files.isEmpty()) {
            err(ExitValue.INVALID_ARGUMENTS, "No files given to process");
        }

        int totalFailed = 0;
        final Set<Path> outputtedFiles = new HashSet<>();
        final Set<Path> filesToCompile = new HashSet<>();
        for (final InputFile fileArg : files) {
            if (!"ssc".equals(fileArg.suffix())) {
                logger.printDebug("Skipping transpilation of file '"
                        + fileArg.absolutePathString()
                        + "' (not an ssc file)");
                filesToCompile.add(fileArg.toAbsolutePath());
                continue;
            }

            try {
                final Optional<Path> processed = processFile(fileArg);
                if (processed.isEmpty()) {
                    totalFailed++;
                    if (parsedArgs.isStopOnError()) {
                        logger.printVerbose("Stopping.");
                        break;
                    }
                } else {
                    final Path file = processed.get();
                    outputtedFiles.add(file);
                    filesToCompile.add(file);
                }
            } catch (RuntimeException e) {
                handleKnownExceptionsOrRethrow(fileArg, e);

                totalFailed++;
                if (parsedArgs.isStopOnError()) {
                    logger.printVerbose("Stopping.");
                    break;
                }
            } finally {
                logger.printVerbose("Processed '" + fileArg.absolutePathString() + "'");
            }
        }

        if (totalFailed != 0) {
            err(ExitValue.TRANSPILATION_FAIL, "Could not process " + totalFailed + " file(s)");
            return;
        }

        if (parsedArgs.getCompileTarget().isPresent()) {
            logger.printVerbose("Compiling...");
            compileCBatch(parsedArgs.getCompileTarget().get(), filesToCompile);

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

    private static void handleKnownExceptionsOrRethrow(final InputFile fileArg,
                                                       final RuntimeException exception) throws RuntimeException {
        if (exception instanceof UnknownTranspilationException) {
            System.err.println("Caught unknown exception while processing '" + fileArg.absolutePathString() + "'");
            //noinspection CallToPrintStackTrace
            exception.printStackTrace();
        } else if (exception instanceof SSCTranspilerException) {
            System.err.println(exception.getMessage());
        } else {
            throw exception; /* doesn't get caught again */
        }
    }

    private static Optional<Path> processFile(final InputFile inputFile)
            throws IOException, InterruptedException {
        logger.printVerbose("Processing file: '" + inputFile.absolutePathString() + "'");

        final Path workingFileAbsolutePath = inputFile.getChangedSuffix("c").toAbsolutePath();

        logger.printVerbose("Preprocessing file...");
        if (!preprocessSSCCode(inputFile, workingFileAbsolutePath)) {
            return Optional.empty();
        }

        {
            final VisitorData data = getVisitorData(workingFileAbsolutePath);

            logger.printVerbose("Extracting superstructs...");
            if (!extractSuperstructMembers(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to extract superstructs.");
                return Optional.empty(); // Error nodes encountered
            }
        }
        {
            final VisitorData data = getVisitorData(workingFileAbsolutePath);

            logger.printVerbose("Replacing superstruct references...");
            if (!replaceSuperstructCalls(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to replace superstruct references.");
                return Optional.empty();
            }
        }

        if (parsedArgs.getCompileTarget().isPresent()) {
            /* don't format if you're going to delete the files anyway;
             * don't verify if you're going to compile the files anyway
             */
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

    private static VisitorData getVisitorData(final Path file) throws IOException {
        final SSCLexer lexer = new SSCLexer(CharStreams.fromString(Files.readString(file)));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final SSCParser parser = new SSCParser(tokens);

        parser.removeErrorListeners();

        final ParseTree tree = parser.compilationUnit();
        return new VisitorData(tokens, tree);
    }

    private static boolean extractSuperstructMembers(final CommonTokenStream tokens,
                                                     final ParseTree tree,
                                                     final Path outputFile)
            throws IOException {
        final SuperstructConvertorVisitor visitor = new SuperstructConvertorVisitor(tokens);
        final String result = visitor.visit(tree);
        sss = visitor.getSuperStructs();

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);

        return visitor.hasNoErrors();
    }

    private static boolean replaceSuperstructCalls(final CommonTokenStream tokens,
                                                   final ParseTree tree,
                                                   final Path outputFile)
            throws IOException {
        final ConvertorVisitor visitor = new PostfixExpressionConvertorVisitor(tokens, sss);
        final String result = visitor.visit(tree) + "\n";

        Files.writeString(outputFile, result,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        return visitor.hasNoErrors();
    }

    private static int doProcess(String... args)
            throws IOException, InterruptedException {
        return doProcess(Arrays.asList(args));
    }

    private static int doProcess(final List<String> args)
            throws IOException, InterruptedException {
        return new ProcessBuilder(args).inheritIO().start().waitFor();
    }

    private static boolean preprocessSSCCode(final InputFile inFile,
                                             final Path outFile)
            throws IOException, InterruptedException {
        final Path tempOut = Files.createTempFile(inFile.dir(), inFile.getFullName(), ".i");

        /* cc -E -P -x c ‹file.ssc› -o tmp.i */
        final int exitCode = doProcess(
                "cc",
                "-E",                               // Preprocess
                "-P",                               // Do not output `#line` directives
                "-D" + SSC_DEF_MACRO_STRING + "=1", // Define a way for C/SSC code to know which it is
                "-x", "c",                          // Treat the .ssc file as C
                inFile.toAbsolutePath().toString(), // Input file
                "-o", tempOut.toString()            // Output file
        );

        if (exitCode != 0) {
            tempOut.toFile().deleteOnExit();
            return false;
        }

        Files.move(tempOut, outFile, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static int verifyCCode(final Path file) throws IOException, InterruptedException {
        final List<String> cmd = ListBuilder.from("cc")
                .addAll(CC_OPTIONS)
                .add("-fsyntax-only")
                .add(file.toString())
                .build();

        return doProcess(cmd);
    }

    private static void compileCBatch(String binaryName, Collection<Path> files)
            throws IOException, InterruptedException {
        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        final List<String> cmd = ListBuilder.from("cc")
                .addAll(CC_OPTIONS)
                .add("-o")
                .add(binaryName)
                .addMapped(files, Path::toString)
                .build();

        final int exitCode = doProcess(cmd);
        if (exitCode != 0) {
            err(ExitValue.C_COMPILATION_FAIL, "Compilation failed with exit code: " + exitCode);
        }
    }
}

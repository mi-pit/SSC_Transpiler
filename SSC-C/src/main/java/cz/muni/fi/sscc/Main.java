package cz.muni.fi.sscc;

import antlr.SSCLexer;
import antlr.SSCParser;
import cz.muni.fi.sscc.args.CommandLineArguments;
import cz.muni.fi.sscc.args.InputFile;
import cz.muni.fi.sscc.data.SuperStructRepre;
import cz.muni.fi.sscc.exceptions.*;
import cz.muni.fi.sscc.visitors.ConvertorVisitor;
import cz.muni.fi.sscc.visitors.PostfixExpressionConvertorVisitor;
import cz.muni.fi.sscc.visitors.SSVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static cz.muni.fi.sscc.exceptions.ExitValue.err;
import static cz.muni.fi.sscc.exceptions.ExitValue.warn;

public final class Main {
    private Main() {
    }

    private static final String SSC_DEF_MACRO_STRING = "__SSC_LANGUAGE__";

    private static Collection<SuperStructRepre> sss;
    private static CommandLineArguments parsedArgs;
    public static Logger logger;

    private static final List<String> CC_OPTIONS = List.of(
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
        for (InputFile fileArg : files) {
            if (!fileArg.suffix().equals("ssc")) {
                logger.printVerbose("Skipping transpilation of file '"
                        + fileArg.absolutePathString()
                        + "' (not an ssc file)");
                outputtedFiles.add(fileArg.toAbsolutePath());
                logger.printDebug("Added to compilation files set");
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
                    outputtedFiles.add(processed.get());
                }
            } catch (SSCTranspilerException e) {
                System.err.println(e.getMessage());
                totalFailed++;
                if (parsedArgs.isStopOnError()) {
                    logger.printVerbose("Stopping.");
                    break;
                }
            }
        }

        if (totalFailed != 0) {
            err(ExitValue.TRANSPILATION_FAIL, "Could not process " + totalFailed + " file(s)");
            return;
        }

        if (parsedArgs.getCompileTarget().isPresent()) {
            logger.printVerbose("Compiling...");
            compileCCode(parsedArgs.getCompileTarget().get(), outputtedFiles);
        }
    }

    /**
     * TODO:
     *  add options
     *      transpile to C only
     *      transpile AND compile (remove `.c` file)
     */
    private static Optional<Path> processFile(final InputFile inputFile) throws IOException, InterruptedException {
        Optional<Path> pathVerified = getPathVerified(inputFile.toAbsolutePath());
        if (pathVerified.isEmpty())
            return Optional.empty();

        logger.printVerbose("Parsing file: '" + inputFile.absolutePathString() + "'");

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

        logger.printVerbose("Formatting...");
        if (!formatCCode(workingFileAbsolutePath)) {
            logger.printVerbose("Failed to format C code.");
            return Optional.empty();
        }

        logger.printVerbose("Verifying...");
        final int exitCode = verifyCCode(workingFileAbsolutePath);
        if (exitCode != 0) {
            logger.printVerbose("Verification failed with exit code: " + exitCode);
            return Optional.empty();
        }

        return Optional.of(workingFileAbsolutePath);
    }

    private static Optional<Path> getPathVerified(final Path fileAbsolutePath) {
        if (!Files.exists(fileAbsolutePath)) {
            warn("File " + fileAbsolutePath + " not found");
            return Optional.empty();
        }

        if (!Files.isRegularFile(fileAbsolutePath)) {
            warn("File " + fileAbsolutePath + " is not a regular file");
            return Optional.empty();
        }

        return Optional.of(fileAbsolutePath);
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
        final SSVisitor visitor = new SSVisitor(tokens);
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

    private static boolean preprocessSSCCode(final InputFile inFile,
                                             final Path outFile)
            throws IOException, InterruptedException {
        final Path tempOut = Files.createTempFile(inFile.dir(), inFile.getFullName(), ".i");

        /* cc -E -x c test_file.ssc -P */
        final ProcessBuilder pb = new ProcessBuilder(
                "cc",
                "-E",       // Preprocess
                "-P",       // Do not output # line directives
                "-D" + SSC_DEF_MACRO_STRING + "=1",
                "-x", "c",  // treat the .ssc file as C
                inFile.toAbsolutePath().toString(),
                "-o", tempOut.toString() // output file
        );

        pb.inheritIO(); // shows warnings/errors in terminal

        final Process process = pb.start();

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            tempOut.toFile().deleteOnExit();
            return false;
        }

        Files.move(tempOut, outFile, StandardCopyOption.REPLACE_EXISTING);
        return true;
    }

    private static boolean formatCCode(final Path file) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder(
                "/opt/homebrew/bin/clang-format", // fixme
                "-i" /* in place */,
                file.toString()
        );
        pb.inheritIO();
        final Process process = pb.start();
        final int exitCode = process.waitFor();
        return exitCode == 0;
    }

    private static int verifyCCode(Path file) throws IOException, InterruptedException {
        final List<String> cmd = new ArrayList<>();
        cmd.add("cc");
        cmd.addAll(CC_OPTIONS);
        cmd.add("-fsyntax-only");
        cmd.add(file.toString());

        final ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        final Process process = pb.start();
        return process.waitFor();
    }

    private static void compileCCode(String binaryName, Collection<Path> files)
            throws IOException, InterruptedException {
        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        final List<String> cmd = new ArrayList<>();
        cmd.add("cc");
        cmd.addAll(CC_OPTIONS);
        cmd.add("-o");
        cmd.add(binaryName);

        // add all source files
        for (final Path file : files) {
            cmd.add(file.toString());
        }
        final ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();

        final Process process = pb.start();
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            err(ExitValue.C_COMPILATION_FAIL, "Compilation failed with exit code: " + exitCode);
        }
    }

    private record VisitorData(CommonTokenStream tokens, ParseTree tree) {
    }
}

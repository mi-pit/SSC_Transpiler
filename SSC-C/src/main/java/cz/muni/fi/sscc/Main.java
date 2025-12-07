package cz.muni.fi.sscc;

import antlr.SSCLexer;
import antlr.SSCParser;
import cz.muni.fi.sscc.exceptions.AntlrException;
import cz.muni.fi.sscc.exceptions.SSCSyntaxException;
import cz.muni.fi.sscc.visitors.ConvertorVisitor;
import cz.muni.fi.sscc.visitors.PostfixExpressionConvertorVisitor;
import cz.muni.fi.sscc.visitors.SSVisitor;
import cz.muni.fi.sscc.data.SuperStructRepre;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cz.muni.fi.sscc.ExitValue.err;
import static cz.muni.fi.sscc.ExitValue.warn;

public class Main {
    private static final String SSC_DEF_MACRO_STRING = "__SSC_LANG__";

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

    public static final SSCErrorListener errorListener = new SSCErrorListener();


    public static void main(String[] args)
            throws IOException, InterruptedException {
        parsedArgs = new CommandLineArguments(args);
        logger = new Logger(parsedArgs);

        processFiles(parsedArgs.getFilesToProcess());
    }

    private static void processFiles(List<Path> files) throws IOException, InterruptedException {
        if (files.isEmpty()) {
            err(ExitValue.INVALID_ARGUMENTS, "No files given to process");
        }

        int aggregate = 0;
        for (Path fileArg : files) {
            try {
                aggregate += processFile(fileArg) ? 0 : 1;
            } catch (SSCSyntaxException | AntlrException e) {
                System.err.println(e.getMessage());
            }
        }

        if (aggregate != 0) {
            warn("Could not process " + aggregate + " file(s)");
        }
    }

    /**
     * TODO:
     *  add options
     *      transpile to C only
     *      transpile AND compile (remove `.c` file)
     */
    private static boolean processFile(final Path fileAbsolutePath) throws IOException, InterruptedException {
        if (!Files.exists(fileAbsolutePath)) {
            warn("File " + fileAbsolutePath + " not found");
            return false;
        }

        logger.printVerbose("Parsing file: `" + fileAbsolutePath + "`");
        final Path inputFileDir = getFileDir(fileAbsolutePath);

        final Path workingFileAbsolutePath =
                Path.of(fileAbsolutePath.toString().replaceFirst("\\.ssc$", ".c"));

        logger.printVerbose("Preprocessing file " + workingFileAbsolutePath + " ...");
        if (!preprocessSSCCode(inputFileDir, fileAbsolutePath, workingFileAbsolutePath)) {
            return false;
        }

        {
            final VisitorData data = getVisitorData(workingFileAbsolutePath);

            logger.printVerbose("Extracting superstructs...");
            if (!extractSuperstructMembers(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to extract superstructs.");
                return false; // Error nodes encountered
            }
        }
        {
            final VisitorData data = getVisitorData(workingFileAbsolutePath);

            logger.printVerbose("Replacing superstruct references...");
            if (!replaceSuperstructCalls(data.tokens(), data.tree(), workingFileAbsolutePath)) {
                logger.printVerbose("Failed to replace superstruct references.");
                return false;
            }
        }

        logger.printVerbose("Formatting...");
        if (!formatCCode(workingFileAbsolutePath)) {
            logger.printVerbose("Failed to format C code.");
            return false;
        }

        logger.printVerbose("Verifying...");
        final int exitCode = verifyCCode(workingFileAbsolutePath);
        if (exitCode != 0) {
            logger.printVerbose("Verification failed with exit code: " + exitCode);
            return false;
        }

        if (parsedArgs.getCompileTarget().isEmpty()) {
            logger.printVerbose("Not compiling anything.");
            return true;
        }

        logger.printVerbose("Compiling...");
        // TODO: compile all files at once
        compileCCode(parsedArgs.getCompileTarget().get(), workingFileAbsolutePath);
        workingFileAbsolutePath.toFile().deleteOnExit();
        return true;
    }

    private static Path getFileDir(final Path file) {
        final Path parent = file.getParent();
        return parent == null
                ? Path.of(".")
                : parent;
    }

    private static VisitorData getVisitorData(Path file) throws IOException {
        final SSCLexer lexer = new SSCLexer(CharStreams.fromString(Files.readString(file)));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final SSCParser parser = new SSCParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        final ParseTree tree = parser.compilationUnit();
        return new VisitorData(tokens, tree);
    }

    private static boolean extractSuperstructMembers(CommonTokenStream tokens, ParseTree tree, Path outputFile)
            throws IOException {
        SSVisitor visitor = new SSVisitor(tokens);
        String result = visitor.visit(tree);
        sss = visitor.getSuperStructs();

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);

        return visitor.hasNoErrors();
    }

    private static boolean replaceSuperstructCalls(CommonTokenStream tokens, ParseTree tree,
                                                   Path outputFile)
            throws IOException {
        final ConvertorVisitor visitor = new PostfixExpressionConvertorVisitor(tokens, sss);
        final String result = visitor.visit(tree) + "\n";

        Files.writeString(outputFile, result,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        return visitor.hasNoErrors();
    }

    private static boolean preprocessSSCCode(final Path dir,
                                             final Path inFile,
                                             final Path outFile)
            throws IOException, InterruptedException {
        final Path tempOut = Files.createTempFile(dir, inFile.getFileName().toString(), ".i");

        /* cc -E -x c test_file.ssc -P */
        final ProcessBuilder pb = new ProcessBuilder(
                "cc",
                "-E",       // Preprocess
                "-P",       // Do not output # line directives
                "-D" + SSC_DEF_MACRO_STRING,
                "-x", "c",  // treat the .ssc file as C
                inFile.toString(),
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

    private static boolean formatCCode(Path file) throws IOException, InterruptedException {
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

    private static void compileCCode(String binaryName, Path... files) throws IOException, InterruptedException {
        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        final List<String> cmd = new ArrayList<>();
        cmd.add("cc");
        cmd.addAll(CC_OPTIONS);
        cmd.add("-o");
        cmd.add(binaryName);

        // add all source files
        for (Path file : files) {
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

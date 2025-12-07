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


    public static void main(String[] args) {
        parsedArgs = new CommandLineArguments(args);
        logger = new Logger(parsedArgs);

        try {
            processFiles(parsedArgs.getFilesToProcess());
        } catch (IOException | InterruptedException e) {
            err(ExitValue.IO_EXCEPTION, e.getMessage());
        }
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
            err(ExitValue.TRANSPILATION_FAIL, "Could not process " + aggregate + " file(s)");
        }
    }

    /**
     * TODO:
     *  add options
     *      transpile to C only
     *      transpile AND compile (remove `.c` file)
     */
    private static boolean processFile(final Path fileAbsolutePath) throws IOException, InterruptedException {
        logger.printVerbose("Parsing file: `" + fileAbsolutePath + "`");
        final Path inputFileDir = fileAbsolutePath.getParent();

        final Path workingFile = Path.of(fileAbsolutePath.toString().replaceFirst("\\.ssc$", ".c"));

        logger.printVerbose("Preprocessing file " + workingFile + " ...");
        preprocessSSCCode(inputFileDir, fileAbsolutePath, workingFile);

        {
            final VisitorData data = getVisitorData(workingFile);

            logger.printVerbose("Extracting superstructs...");
            if (!extractSuperstructMembers(data.tokens(), data.tree(), workingFile)) {
                logger.printVerbose("Failed to extract superstructs.");
                return false; // Error nodes encountered
            }
        }
        {
            final VisitorData data = getVisitorData(workingFile);

            logger.printVerbose("Replacing superstruct references...");
            if (!replaceSuperstructCalls(data.tokens(), data.tree(), workingFile)) {
                logger.printVerbose("Failed to replace superstruct references.");
                return false;
            }
        }

        logger.printVerbose("Formatting...");
        formatCCode(workingFile);

        logger.printVerbose("Verifying...");
        verifyCCode(workingFile);

        if (parsedArgs.getCompileTarget().isEmpty()) {
            return true;
        }

        logger.printVerbose("Compiling...");
        // TODO: compile all files at once
        compileCCode(parsedArgs.getCompileTarget().get(), workingFile);
        workingFile.toFile().deleteOnExit();
        return true;
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

    private static void preprocessSSCCode(Path dir, Path inFile, Path outFile)
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
            err(ExitValue.C_PREPROCESSING_FAIL, "C preprocessor failed with exit code: " + exitCode);
        }

        Files.move(tempOut, outFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void formatCCode(Path file) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder(
                "/opt/homebrew/bin/clang-format", // fixme
                "-i" /* in place */,
                file.toString()
        );
        pb.inheritIO();
        final Process process = pb.start();
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            err(ExitValue.IO_EXCEPTION, "Formatting failed with exit code: " + exitCode);
        }
    }

    private static void verifyCCode(Path file) throws IOException, InterruptedException {
        final List<String> cmd = new ArrayList<>();
        cmd.add("cc");
        cmd.addAll(CC_OPTIONS);
        cmd.add("-fsyntax-only");
        cmd.add(file.toString());

        final ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        final Process process = pb.start();
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            err(ExitValue.C_VERIFICATION_FAIL, "Verification failed with exit code: " + exitCode);
        }
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

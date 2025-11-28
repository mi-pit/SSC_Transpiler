package cz.muni.fi.sscc;

import cz.muni.fi.sscc.antlr.SSCLexer;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.ConvertorVisitor;
import cz.muni.fi.sscc.mine.IncludeDirectiveVisitor;
import cz.muni.fi.sscc.mine.PostfixExpressionConvertorVisitor;
import cz.muni.fi.sscc.mine.SSVisitor;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static cz.muni.fi.sscc.util.Util.*;

public class Main {
    private static Collection<SuperStructRepre> sss;
    public static boolean doPrintDebug = false;
    public static boolean verbose = true;

    public static void main(String[] args) {
        try {
            processFiles(args);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void processFiles(String[] args) throws IOException, InterruptedException {
        if (args.length == 0) {
            System.err.println("Usage: Main <file> [<file>...]");
            System.exit(1);
        }

        for (String fileArg : args) {
            processFile(fileArg);
        }
    }

    private static void processFile(String fileArg) throws IOException, InterruptedException {
        try {
            printVerbose("Parsing file: `" + fileArg + "`");
            final Path fileAbsolutePath = Path.of(fileArg);

            final Path inputFileDir = fileAbsolutePath.getParent();
            final Path inputFileName = fileAbsolutePath.getFileName();

            /* TODO:
             *  clean up file manipulation
             *  add compilation
             *  add options
             *      transpile to C only
             *      transpile AND compile (remove `.c` file)
             */
            printVerbose("Preprocessing file...");
            final Path preprocessedSSCCodeAbsolutePath = preprocessSSCCode(inputFileDir, inputFileName);
            final Path preprocessedSSCCodeFileName = preprocessedSSCCodeAbsolutePath.getFileName();
            final Path finalOutputFile =
                    inputFileDir.resolve(inputFileName.toString().replaceFirst("\\.ssc$", ".c"));

            CommonTokenStream sscTokens = new CommonTokenStream(
                    new SSCLexer(CharStreams.fromString(Files.readString(preprocessedSSCCodeAbsolutePath)))
            );
            SSCParser parser = new SSCParser(sscTokens);
            parser.removeErrorListeners();
            ParseTree sscTree = parser.compilationUnit();

            Path extractedSuperstructsFile = Files.createTempFile(
                    inputFileDir,
                    preprocessedSSCCodeFileName.toString(),
                    ".tmp.ssc"
            );
            extractedSuperstructsFile.toFile().deleteOnExit();

            printVerbose("Extracting superstructs...");
            /* Print `struct`s to temp file */
            extractSuperstructMembers(sscTokens, sscTree, extractedSuperstructsFile);

            /* Read from temp file */
            SSCLexer tmpLexer = new SSCLexer(CharStreams.fromString(Files.readString(extractedSuperstructsFile)));
            CommonTokenStream tmpTokens = new CommonTokenStream(tmpLexer);
            SSCParser tmpParser = new SSCParser(tmpTokens);
            tmpParser.removeErrorListeners();
            ParseTree tmpTree = tmpParser.compilationUnit();

            printVerbose("Replacing superstruct references...");
            /* Convert temp */
            replaceSuperstructCalls(tmpTokens, tmpTree, finalOutputFile);

            printVerbose("Formatting...");
            formatCCode(finalOutputFile);

            printVerbose("Done");
            printVerbose("Output: " + finalOutputFile); // fixme

            printVerbose("Verifying...");
            verifyCCode(finalOutputFile);

            printVerbose("Compiling...");
            compileCCode(new Path[]{finalOutputFile},
                    "/Users/mipit/Programming/CProjects/MyC/SSC_code/normal_c_runnable");
        } catch (SSCSyntaxException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void extractSuperstructMembers(CommonTokenStream tokens, ParseTree tree, Path outputFile)
            throws IOException {
        SSVisitor visitor = new SSVisitor(tokens);
        String result = visitor.visit(tree);
        sss = visitor.getSuperStructs();

        writeToFile(outputFile, result);
    }

    private static void replaceSuperstructCalls(CommonTokenStream tokens, ParseTree tree, Path outputFile)
            throws IOException {
        ConvertorVisitor visitor = new PostfixExpressionConvertorVisitor(tokens, sss);
        String result = visitor.visit(tree);

        Files.writeString(outputFile, result + "\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static Path prepreprocessSSCCode(Path inputFileDir, Path inputFileName) throws IOException {
        // TODO
        return null;
    }

    public static Path preprocessSSCCode(Path dir, Path inputFile) throws IOException, InterruptedException {
        final Path tempOut = Files.createTempFile(dir, inputFile.toString(), ".i");
        tempOut.toFile().deleteOnExit();

        /* cc -E -dI -x c test_file.ssc -P */
        final ProcessBuilder pb = new ProcessBuilder(
                "cc",
                "-E",           // Preprocess
                "-dI",          // no includes
                "-P",           // Do not output # line directives
                "-x", "c",      // treat the .ssc file as C
                dir + "/" + inputFile,
                "-o", tempOut.toString() // output file
        );

        pb.inheritIO(); // shows warnings/errors in terminal

        final Process process = pb.start();

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Preprocessing failed with exit code: " + exitCode);
        }

        return tempOut;
    }

    public static void formatCCode(Path file) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder(
                "/opt/homebrew/bin/clang-format", // fixme
                "-i" /* in place */,
                file.toString()
        );
        pb.inheritIO();
        final Process process = pb.start();
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Formatting failed with exit code: " + exitCode);
        }
    }

    public static void verifyCCode(Path file) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder(
                /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
                "cc", "-Werror", "-fsyntax-only", file.toString()
        );
        pb.inheritIO();
        final Process process = pb.start();
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Verification failed with exit code: " + exitCode);
        }
    }

    public static void compileCCode(Path[] files, String binaryName) throws IOException, InterruptedException {
        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        List<String> cmd = new ArrayList<>();
        cmd.add("cc");
        cmd.add("-Werror");
        cmd.add("-Wall");
        cmd.add("-Wextra");
        cmd.add("-pedantic");
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
            System.err.println("Compilation failed with exit code: " + exitCode);
        }
    }
}

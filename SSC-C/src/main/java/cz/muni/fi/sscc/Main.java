package cz.muni.fi.sscc;

import cz.muni.fi.sscc.antlr.SSCLexer;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.*;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cz.muni.fi.sscc.util.Util.*;

public class Main {
    private static Collection<SuperStructRepre> sss;
    public static boolean doPrintDebug = false;
    public static boolean verbose = true;
    public static boolean doCompile = false;

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
            System.err.println("Usage: ‹progname› <file> [<file>...]");
            System.exit(1);
        }

        for (String fileArg : args) {
            try {
                processFile(fileArg);
            } catch (SSCSyntaxException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * TODO:
     *  clean up file manipulation
     *  add compilation
     *  add options
     *      transpile to C only
     *      transpile AND compile (remove `.c` file)
     */
    private static void processFile(String fileArg) throws IOException, InterruptedException {
        printVerbose("Parsing file: `" + fileArg + "`");
        final Path fileAbsolutePath = Path.of(fileArg);
        final Path inputFileDir = fileAbsolutePath.getParent();

        Path workingFile = Path.of(fileAbsolutePath.toString().replaceFirst("\\.ssc$", ".c"));
        Files.writeString(workingFile, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        final VisitorData origSSCVisitorData = getVisitorData(fileAbsolutePath);

        printVerbose("Extracting include directives...");
        final List<String> includes = extractDirectives(
                workingFile, origSSCVisitorData.tokens(), origSSCVisitorData.tree());

        printVerbose("Preprocessing file " + workingFile + " ...");
        preprocessSSCCode(inputFileDir, workingFile);

        {
            VisitorData data = getVisitorData(workingFile);

            printVerbose("Extracting superstructs...");
            extractSuperstructMembers(data.tokens(), data.tree(), workingFile);
        }
        {
            VisitorData data = getVisitorData(workingFile);

            printVerbose("Replacing superstruct references...");
            replaceSuperstructCalls(data.tokens(), data.tree(), workingFile, includes);
        }

        printVerbose("Formatting...");
        formatCCode(workingFile);

        printVerbose("Verifying...");
        if (!verifyCCode(workingFile) || !doCompile) {
            return;
        }

        printVerbose("Compiling...");
        // fixme
        compileCCode("/Users/mipit/Programming/CProjects/MyC/SSC_code/normal_c_runnable", workingFile);
    }

    private static VisitorData getVisitorData(Path file) throws IOException {
        final SSCLexer tmpLexer = new SSCLexer(CharStreams.fromString(Files.readString(file)));
        final CommonTokenStream tmpTokens = new CommonTokenStream(tmpLexer);
        final SSCParser tmpParser = new SSCParser(tmpTokens);
        final ParseTree tmpTree = tmpParser.compilationUnit();
        return new VisitorData(tmpTokens, tmpTree);
    }

    private record VisitorData(CommonTokenStream tokens, ParseTree tree) {
    }

    private static void extractSuperstructMembers(CommonTokenStream tokens, ParseTree tree, Path outputFile)
            throws IOException {
        SSVisitor visitor = new SSVisitor(tokens);
        String result = visitor.visit(tree);
        sss = visitor.getSuperStructs();

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void replaceSuperstructCalls(CommonTokenStream tokens, ParseTree tree,
                                                Path outputFile, List<String> includes)
            throws IOException {
        ConvertorVisitor visitor = new PostfixExpressionConvertorVisitor(tokens, sss);
        final String visited = visitor.visit(tree);
        final String includesString = String.join("\n", includes);
        final String result = includesString + "\n" + visited;

        Files.writeString(outputFile, result,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Rewrites {@code outFile} with the code without `#include`s and returns a list of include strings.
     */
    public static List<String> extractDirectives(Path outFile, CommonTokenStream sscTokens, ParseTree sscTree)
            throws IOException {
        final IncludeDirectiveRemoverVisitor visitor = new IncludeDirectiveRemoverVisitor(sscTokens);
        final IncludeDirectiveCollector collector = new IncludeDirectiveCollector();

        collector.visit(sscTree);
        final String withoutStdIncludes = visitor.visit(sscTree);
        final List<String> includesOnly = collector.getIncludes();

        Files.writeString(outFile, withoutStdIncludes, StandardOpenOption.TRUNCATE_EXISTING);

        return includesOnly;
    }

    public static void preprocessSSCCode(Path dir, Path workingFile)
            throws IOException, InterruptedException {
        final Path tempOut = Files.createTempFile(dir, workingFile.getFileName().toString(), ".i");

        /* cc -E -dI -x c test_file.ssc -P */
        final ProcessBuilder pb = new ProcessBuilder(
                "cc",
                "-E",           // Preprocess
                "-dI",          // no includes
                "-P",           // Do not output # line directives
                "-x", "c",      // treat the .ssc file as C
                workingFile.toString(),
                "-o", tempOut.toString() // output file
        );

        pb.inheritIO(); // shows warnings/errors in terminal

        final Process process = pb.start();

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Preprocessing failed with exit code: " + exitCode);
        }

        Files.move(tempOut, workingFile, StandardCopyOption.REPLACE_EXISTING);
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

    public static boolean verifyCCode(Path file) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder(
                /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
                "cc", "-Werror", "-fsyntax-only", file.toString()
        );
        pb.inheritIO();
        final Process process = pb.start();
        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.err.println("Verification failed with exit code: " + exitCode);
            return false;
        }
        return true;
    }

    public static void compileCCode(String binaryName, Path... files) throws IOException, InterruptedException {
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

package cz.muni.fi.sscc;

import cz.muni.fi.sscc.antlr.SSCLexer;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.ConvertorVisitor;
import cz.muni.fi.sscc.mine.PostfixExpressionConvertorVisitor;
import cz.muni.fi.sscc.mine.SSVisitor;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
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

public class Main {
    private static Collection<SuperStructRepre> sss;
    private static CommandLineArguments parsedArgs;
    public static Logger logger;

    public static void main(String[] args) {
        parsedArgs = new CommandLineArguments(args);
        logger = new Logger(parsedArgs);

        try {
            processFiles(parsedArgs.getFilesToProcess());
        } catch (IOException | InterruptedException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void processFiles(List<Path> files) throws IOException, InterruptedException {
        if (files.isEmpty()) {
            System.err.println("No files given to process");
            System.exit(1);
        }

        for (Path fileArg : files) {
            try {
                processFile(fileArg);
            } catch (SSCSyntaxException | AntlrException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * TODO:
     *  add options
     *      transpile to C only
     *      transpile AND compile (remove `.c` file)
     */
    private static void processFile(final Path fileAbsolutePath) throws IOException, InterruptedException {
        logger.printVerbose("Parsing file: `" + fileAbsolutePath + "`");
        final Path inputFileDir = fileAbsolutePath.getParent();

        final Path workingFile = Path.of(fileAbsolutePath.toString().replaceFirst("\\.ssc$", ".c"));

        logger.printVerbose("Preprocessing file " + workingFile + " ...");
        preprocessSSCCode(inputFileDir, fileAbsolutePath, workingFile);

        {
            final VisitorData data = getVisitorData(workingFile);

            logger.printVerbose("Extracting superstructs...");
            extractSuperstructMembers(data.tokens(), data.tree(), workingFile);
        }
        {
            final VisitorData data = getVisitorData(workingFile);

            logger.printVerbose("Replacing superstruct references...");
            replaceSuperstructCalls(data.tokens(), data.tree(), workingFile);
        }

        logger.printVerbose("Formatting...");
        formatCCode(workingFile);

        logger.printVerbose("Verifying...");
        if (!verifyCCode(workingFile) || parsedArgs.getCompileTarget().isEmpty()) {
            return;
        }

        logger.printVerbose("Compiling...");
        compileCCode(parsedArgs.getCompileTarget().get(), workingFile);
    }

    private static VisitorData getVisitorData(Path file) throws IOException {
        final SSCLexer lexer = new SSCLexer(CharStreams.fromString(Files.readString(file)));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final SSCParser parser = new SSCParser(tokens);
        parser.removeErrorListeners();
        final ParseTree tree = parser.compilationUnit();
        return new VisitorData(tokens, tree);
    }

    private static void extractSuperstructMembers(CommonTokenStream tokens, ParseTree tree, Path outputFile)
            throws IOException {
        SSVisitor visitor = new SSVisitor(tokens);
        String result = visitor.visit(tree);
        sss = visitor.getSuperStructs();

        Files.writeString(outputFile, result, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private static void replaceSuperstructCalls(CommonTokenStream tokens, ParseTree tree,
                                                Path outputFile)
            throws IOException {
        ConvertorVisitor visitor = new PostfixExpressionConvertorVisitor(tokens, sss);
        final String result = visitor.visit(tree);

        Files.writeString(outputFile, result,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private static void preprocessSSCCode(Path dir, Path inFile, Path outFile)
            throws IOException, InterruptedException {
        final Path tempOut = Files.createTempFile(dir, inFile.getFileName().toString(), ".i");

        /* cc -E -x c test_file.ssc -P */
        final ProcessBuilder pb = new ProcessBuilder(
                "cc",
                "-E",       // Preprocess
                "-P",       // Do not output # line directives
                "-D__MP_SSC__", // todo: define SSC
                "-x", "c",  // treat the .ssc file as C
                inFile.toString(),
                "-o", tempOut.toString() // output file
        );

        pb.inheritIO(); // shows warnings/errors in terminal

        final Process process = pb.start();

        final int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Preprocessing failed with exit code: " + exitCode);
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
            System.err.println("Formatting failed with exit code: " + exitCode);
        }
    }

    private static boolean verifyCCode(Path file) throws IOException, InterruptedException {
        final ProcessBuilder pb = new ProcessBuilder(
                /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
                "cc",
                "-Werror", "-Wall", "-Wextra",
                "-Wno-extra-semi", /* I wasn't able to get rid of the trailing semicolons */
                "-fsyntax-only", file.toString()
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

    private static void compileCCode(String binaryName, Path... files) throws IOException, InterruptedException {
        /* cc -Werror -Wall -Wextra -pedantic -fsyntax-only "$file" */
        final List<String> cmd = new ArrayList<>();
        cmd.add("cc");
        cmd.add("-Werror");
        cmd.add("-Wall");
        cmd.add("-Wextra");
        cmd.add("-pedantic");
        cmd.add("-Wno-extra-semi");
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


    private record VisitorData(CommonTokenStream tokens, ParseTree tree) {
    }
}

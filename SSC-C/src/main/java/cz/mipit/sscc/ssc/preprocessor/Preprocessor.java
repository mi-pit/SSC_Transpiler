package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.PreprocessorException;
import org.antlr.v4.runtime.misc.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;

public final class Preprocessor {
    private Preprocessor() {
    }

    public static final String INCLUDE_DIRECTIVE_NAME = "include";

    private static final Set<String> alreadyIncludedFiles = new HashSet<>();

    private static final Queue<Pair<Integer, String>> currentLineQueue = new ArrayDeque<>();

    public static boolean preprocessSSC(final InputFile inputFile,
                                        final Path outputFileAbsolutePath)
            throws IOException {
        if (!outputFileAbsolutePath.isAbsolute()) {
            throw new IllegalArgumentException("Output file path must be absolute");
        }

        final Path input = inputFile.toAbsolutePath();
        final List<String> lines = Files.readAllLines(input);
        final List<String> preprocessedLines = processFile(lines, inputFile.dir());

        if (!Files.exists(outputFileAbsolutePath)) {
            Files.createFile(outputFileAbsolutePath);
        }

        Files.write(outputFileAbsolutePath.toAbsolutePath(), preprocessedLines);

        alreadyIncludedFiles.clear();
        return true;
    }

    private static List<String> processFile(final List<String> lines,
                                            final Path dir) throws IOException {
        final List<String> outputLines = new ArrayList<>(lines.size());

        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);

            currentLineQueue.add(new Pair<>(++i, line));
            expandSSCHeaders(line, outputLines, dir);

            if (i > 3) {
                currentLineQueue.remove();
            }
        }

        return outputLines;
    }

    private static String removeComments(String line) {
        line = line
                .replaceAll("//.*", "")
                .replaceAll("/\\*.*?\\*/", "");

        Main.logger.printDebug("\tWithout comment: '" + line + "'");
        return line;
    }

    private static void expandSSCHeaders(final String line,
                                         final List<String> outputLines,
                                         final Path baseDir)
            throws IOException {
        final Optional<String> maybeFilePath = getFilePathString(removeComments(line));
        if (maybeFilePath.isEmpty()) {
            outputLines.add(line);
            return;
        }
        final String filePathString = maybeFilePath.get();

        if (filePathString.isBlank()) {
            throw new PreprocessorException("Empty file path string");
        }

        final Path resolvedNormalized = tryGetPathFromString(filePathString, baseDir)
                .toAbsolutePath()
                .normalize();

        final boolean isSscHeader = "ssch".equals(InputFile.fromAbsolutePath(resolvedNormalized).suffix());
        if (!isSscHeader) {
            final String newIncludeLine =
                    "#include \"" + resolvedNormalized + "\" /* resolved from " + filePathString + " */";
            outputLines.add(newIncludeLine);
            return;
        }

        if (alreadyIncludedFiles.contains(resolvedNormalized.getFileName().toString())) {
            return;
        }

        alreadyIncludedFiles.add(resolvedNormalized.getFileName().toString());

        final Path fileDir = resolvedNormalized.getParent();

        Main.logger.printDebug("\tFile path:       '" + resolvedNormalized + "'");

        if (!Files.exists(resolvedNormalized)) {
            throw new PreprocessorException("Included file '" + resolvedNormalized + "' does not exist");
        }

        /* Literal */
        final List<String> lines = Files.readAllLines(resolvedNormalized);

        /* Converted */
        final List<String> subfileOutputLines = processFile(lines, fileDir);

        outputLines.addAll(subfileOutputLines);
    }

    private static Optional<String> getFilePathString(final String withoutComments) {
        final String trimmed = withoutComments.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        if (!trimmed.startsWith("#")) {
            Main.logger.printDebug("\tNot a directive");
            return Optional.empty();
        }

        final String withoutHash = trimmed.substring(1).trim();
        Main.logger.printDebug("\tWithout hash:    '" + withoutHash + "'");

        if (!withoutHash.startsWith(INCLUDE_DIRECTIVE_NAME)) {
            Main.logger.printDebug("\tNot an include");
            return Optional.empty();
        }

        final String withoutInclude = withoutHash.substring(INCLUDE_DIRECTIVE_NAME.length()).trim();
        Main.logger.printDebug("\tWithout include: '" + withoutInclude + "'");

        if (withoutInclude.isEmpty()) {
            throw new PreprocessorException("Empty include directive");
        }

        if (withoutInclude.length() == 1) {
            throw new PreprocessorException("Include directive argument is missing a closing '>' or '\"'");
        }

        final char firstChar = withoutInclude.charAt(0);
        final char lastChar = withoutInclude.charAt(withoutInclude.length() - 1);
        if (firstChar == '<') {
            Main.logger.printDebug("\tNot quoted include");
            return Optional.empty();
        }

        if (firstChar != lastChar) {
            throw new PreprocessorException("Invalid include directive argument `" + withoutInclude + "`");
        }

        final String filePathString = withoutInclude
                .substring(1, withoutInclude.length() - 1)
                .trim();

        return Optional.of(filePathString);
    }

    private static Path tryGetPathFromString(final String filePathString,
                                             final Path baseDir) {
        try {
            return baseDir.resolve(filePathString);
        } catch (InvalidPathException e) {
            throw new PreprocessorException("Invalid file path: '" + filePathString + "'");
        }
    }

    public static Queue<Pair<Integer, String>> getCurrentLineQueue() {
        return currentLineQueue;
    }
}

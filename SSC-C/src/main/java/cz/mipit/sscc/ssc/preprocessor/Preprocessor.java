package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.PreprocessorException;

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

        for (String line : lines) {
            processLine(line, outputLines, dir);
        }

        return outputLines;
    }

    private static String removeComments(String line) {
        line = line
                .replaceAll("//.*", "")
                .replaceAll("/\\*.*?\\*/", "")
                .trim( /* todo: keep whitespace */);

        Main.logger.printDebug("\tWithout comment: '" + line + "'");
        return line;
    }

    private static void processLine(final String originalLine,
                                    final List<String> outputLines,
                                    final Path baseDir)
            throws IOException {
        final Optional<String> maybeFilePath = getFilePathString(
                removeComments(originalLine),
                outputLines
        );
        if (maybeFilePath.isEmpty()) {
            return;
        }
        final String filePath = maybeFilePath.get();

        final Path resolvedNormalized = tryGetPathFromString(originalLine, filePath, baseDir)
                .toAbsolutePath()
                .normalize();

        final boolean isSSCH = "ssch".equals(InputFile.fromAbsolutePath(resolvedNormalized).suffix());
        if (!isSSCH) {
            final String newIncludeLine =
                    "#include \"" + resolvedNormalized + "\" /* resolved from " + filePath + " */";
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
            throw new PreprocessorException("Included file '" + resolvedNormalized + "' does not exist", originalLine);
        }

        /* Literal */
        final List<String> lines = Files.readAllLines(resolvedNormalized);

        /* Converted */
        final List<String> subfileOutputLines = processFile(lines, fileDir);

        outputLines.addAll(subfileOutputLines);
    }

    private static Optional<String> getFilePathString(final String originalLine,
                                                      final List<String> outputLines) {
        if (!originalLine.startsWith("#")) {
            Main.logger.printDebug("\tNot a directive");
            outputLines.add(originalLine);
            return Optional.empty();
        }

        final String withoutHash = originalLine.substring(1).trim();
        Main.logger.printDebug("\tWithout hash:    '" + withoutHash + "'");

        if (!withoutHash.startsWith(INCLUDE_DIRECTIVE_NAME)) {
            Main.logger.printDebug("\tNot an include");
            // TODO: define, ...
            outputLines.add(originalLine);
            return Optional.empty();
        }

        final String withoutInclude = withoutHash
                .substring(INCLUDE_DIRECTIVE_NAME.length())
                .trim();
        Main.logger.printDebug("\tWithout include: '" + withoutInclude + "'");

        final char firstChar = withoutInclude.charAt(0);
        final char lastChar = withoutInclude.charAt(withoutInclude.length() - 1);
        if (firstChar == '<') {
            Main.logger.printDebug("\tNot quoted include");
            outputLines.add(originalLine);
            return Optional.empty();
        }

        if (firstChar != lastChar) {
            throw new PreprocessorException("Invalid include string", originalLine);
        }

        final String filePathString = withoutInclude
                .substring(1, withoutInclude.length() - 1)
                .trim();

        return Optional.of(filePathString);
    }

    private static Path tryGetPathFromString(final String errmsg,
                                             final String filePathString,
                                             final Path baseDir) {
        try {
            return baseDir.resolve(filePathString);
        } catch (InvalidPathException e) {
            throw new PreprocessorException("Invalid file path: '" + filePathString + "'", errmsg);
        }
    }

}

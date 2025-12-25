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
    public static final String INCLUDE_DIRECTIVE_NAME = "include";

    private final Set<Path> alreadyIncludedFiles;

    private int currentLineNumber;
    private String currentLine;

    private final LinkedList<EnumeratedLine> lastLines;
    private static final int nLines = 4;

    public Preprocessor() {
        currentLineNumber = 1;
        lastLines = new LinkedList<>();
        alreadyIncludedFiles = new HashSet<>();
    }

    public static boolean preprocessSSC(final InputFile inputFile,
                                        final Path outputFileAbsolutePath) throws IOException {
        final Preprocessor preprocessor = new Preprocessor();
        return preprocessor.preprocessSSC_(inputFile, outputFileAbsolutePath);
    }

    private boolean preprocessSSC_(final InputFile inputFile,
                                   final Path outputFileAbsolutePath)
            throws IOException {
        if (!outputFileAbsolutePath.isAbsolute()) {
            throw new IllegalArgumentException("Output file path must be absolute");
        }

        final List<String> preprocessedLines = processFile(
                Files.readAllLines(inputFile.toAbsolutePath()),
                inputFile.dir()
        );

        if (!Files.exists(outputFileAbsolutePath)) {
            Files.createFile(outputFileAbsolutePath);
        }

        Files.write(outputFileAbsolutePath.toAbsolutePath(), preprocessedLines);

        alreadyIncludedFiles.clear();
        return true;
    }

    private List<String> processFile(final List<String> lines,
                                     final Path dir) throws IOException {
        final List<String> outputLines = new ArrayList<>(lines.size());

        for (final String line : lines) {
            currentLine = line;

            lastLines.add(new EnumeratedLine(currentLineNumber++, line));
            if (lastLines.size() > nLines) {
                lastLines.remove();
            }

            processLine(line, outputLines, dir);
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

    private void processLine(final String currentLine,
                             final List<String> outputLines,
                             final Path baseDir)
            throws IOException {
        final Optional<String> maybeFilePath = getFilePathString(removeComments(currentLine));
        if (maybeFilePath.isEmpty()) {
            outputLines.add(currentLine);
            return;
        }
        final String filePathString = maybeFilePath.get();

        final Path resolvedNormalized = tryGetPathFromString(filePathString, baseDir)
                .toAbsolutePath()
                .normalize();

        final boolean isSscHeader = "ssch".equals(InputFile.fromAbsolutePath(resolvedNormalized).suffix());
        if (!isSscHeader) {
            final String newIncludeLine =
                    "#" + INCLUDE_DIRECTIVE_NAME + " \"" + resolvedNormalized + "\"" +
                            " /* resolved from " + filePathString + " */";
            outputLines.add(newIncludeLine);
            return;
        }

        if (alreadyIncludedFiles.contains(resolvedNormalized.getFileName())) {
            return;
        }

        alreadyIncludedFiles.add(resolvedNormalized.getFileName());

        final Path fileDir = resolvedNormalized.getParent();

        Main.logger.printDebug("\tFile path:       '" + resolvedNormalized + "'");

        if (!Files.exists(resolvedNormalized)) {
            throw new PreprocessorException(
                    "Included file '" + resolvedNormalized + "' does not exist",
                    lastLines
            );
        }

        /* Literal */
        final List<String> lines = Files.readAllLines(resolvedNormalized);

        /* Converted */
        final List<String> subfileOutputLines = processFile(lines, fileDir);

        outputLines.addAll(subfileOutputLines);
    }

    private Optional<String> getFilePathString(final String withoutComments) {
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
            throw new PreprocessorException(
                    "Empty include directive",
                    lastLines
            );
        }

        if (withoutInclude.length() == 1) {
            final int index = currentLine.lastIndexOf(withoutInclude);
            throw new PreprocessorException(
                    "Include directive argument is missing a closing '>' or '\"'",
                    lastLines,
                    new int[]{
                            index,
                            index + 1
                    }
            );
        }

        final char firstChar = withoutInclude.charAt(0);
        final char lastChar = withoutInclude.charAt(withoutInclude.length() - 1);

        if ((firstChar != '"' || lastChar != '"') && (firstChar != '<' || lastChar != '>')) {
            throw new PreprocessorException(
                    String.format("Include argument `%s` is not terminated properly", withoutInclude),
                    lastLines,
                    new int[]{
                            currentLine.lastIndexOf(firstChar),
                            currentLine.lastIndexOf(lastChar)
                    }
            );
        }

        if (firstChar == '<') {
            Main.logger.printDebug("\tNot quoted include");
            return Optional.empty();
        }

        final String filePathString = withoutInclude
                .substring(1, withoutInclude.length() - 1)
                .trim();

        if (filePathString.isBlank()) {
            throw new PreprocessorException(
                    "Empty file path string",
                    lastLines,
                    currentLine.lastIndexOf(withoutInclude) + 1,
                    withoutInclude.length() - 2
            );
        }

        return Optional.of(filePathString);
    }

    private Path tryGetPathFromString(final String filePathString,
                                      final Path baseDir) {
        try {
            return baseDir.resolve(filePathString);
        } catch (InvalidPathException e) {
            throw new PreprocessorException(
                    "Could not resolve path '" + baseDir + " + " + filePathString + "'",
                    lastLines
            );
        }
    }
}

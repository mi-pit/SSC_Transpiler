package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.exceptions.children.PreprocessorException;
import cz.mipit.sscc.util.SSCCUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Preprocessor {
    private static final String SSCH_FILE_SUFFIX = "ssch";
    private static final String INCLUDE_DIRECTIVE_NAME = "include";
    private static final int N_LINES = 4;

    private final InputFile inputFile;

    private final LinkedList<EnumeratedLine> lastLines;

    private int currentLineNumber;
    private String currentLine;
    private boolean comment;

    private Preprocessor(final InputFile inputFile) {
        currentLineNumber = 1;
        currentLine = null;
        comment = false;

        lastLines = new LinkedList<>();

        this.inputFile = inputFile;
    }

    public static boolean preprocessSSC(final InputFile inputFile,
                                        final Path outputFileAbsolutePath) throws IOException {
        if (!new Preprocessor(inputFile).writeToOutput(outputFileAbsolutePath))
            return false;

        Main.logger.printDebug("Preprocessing success");
        return true;
    }

    private static List<String> getLines(final Path path) throws IOException {
        final String read = Files.readString(path);
        return SSCCUtil.Text.splitLogicalLines(read);
    }

    private boolean writeToOutput(final Path outputFileAbsolutePath)
            throws IOException {
        if (!outputFileAbsolutePath.isAbsolute()) {
            throw new IllegalArgumentException("Output file path must be absolute");
        }

        final List<String> preprocessedLines = processFile(
                getLines(inputFile.toAbsolutePath()),
                inputFile.dir()
        );

        if (!Files.exists(outputFileAbsolutePath)) {
            Files.createFile(outputFileAbsolutePath);
        }

        Files.write(outputFileAbsolutePath.toAbsolutePath(), preprocessedLines);
        return true;
    }

    private List<String> processFile(final List<String> lines,
                                     final Path dir) throws IOException {
        final List<String> outputLines = new ArrayList<>(lines.size());

        for (final String line : lines) {
            currentLine = line;

            lastLines.add(new EnumeratedLine(currentLineNumber++, line));
            if (lastLines.size() > N_LINES) {
                lastLines.remove();
            }

            processLine(line, outputLines, dir);
        }

        return outputLines;
    }

    private static final Pattern BLOCK_COMMENT_START = Pattern.compile("/\\*.*");
    private static final Pattern BLOCK_COMMENT_END = Pattern.compile(".*?\\*/");

    private String removeComments(final String line) {
        final Matcher endMatcher = BLOCK_COMMENT_END.matcher(line);
        if (comment && !endMatcher.find()) {
            Main.logger.printDebug("\tOnly comment");
            return "";
        }

        String replaced = comment
                ? line.replaceAll(".*?\\*/", "")
                : line;
        comment = false;

        replaced = replaced
                .replaceAll("//.*", "")
                .replaceAll("/\\*.*?\\*/", "");

        final Matcher matcher = BLOCK_COMMENT_START.matcher(replaced);
        if (matcher.find()) {
            comment = true;
            replaced = matcher.replaceAll("");
        }

        Main.logger.printDebug("\tWithout comment: '" + replaced + "'");
        return replaced;
    }

    private void processLine(final String currentLine,
                             final List<String> outputLines,
                             final Path baseDir)
            throws IOException {
        final String commentsRemoved = removeComments(currentLine);
        final Optional<String> maybeFilePath = getFilePathString(commentsRemoved);
        if (maybeFilePath.isEmpty()) {
            outputLines.add(commentsRemoved);
            return;
        }
        final String filePathString = maybeFilePath.get();

        final Path resolvedNormalized = tryGetPathFromString(filePathString, baseDir)
                .toAbsolutePath()
                .normalize();

        final boolean isSscHeader = SSCH_FILE_SUFFIX.equals(InputFile.fromAbsolutePath(resolvedNormalized).suffix());
        if (!isSscHeader) {
            final String newIncludeLine =
                    "#" + INCLUDE_DIRECTIVE_NAME + " \"" + resolvedNormalized + "\"" +
                            " /* resolved from " + filePathString + " */";
            outputLines.add(newIncludeLine);
            return;
        }

        final Path fileDir = resolvedNormalized.getParent();

        Main.logger.printDebug("\tFile path:       '" + resolvedNormalized + "'");

        if (!Files.exists(resolvedNormalized)) {
            throw new PreprocessorException(
                    "Included file '" + resolvedNormalized + "' does not exist",
                    lastLines, inputFile
            );
        }

        final List<String> linesLiteral = Files.readAllLines(resolvedNormalized);

        final InputFile subFile = InputFile.fromAbsolutePath(resolvedNormalized);
        final Preprocessor subFilePreprocessor = new Preprocessor(subFile);

        try {
            final List<String> linesConverted = subFilePreprocessor.processFile(linesLiteral, fileDir);
            outputLines.addAll(linesConverted);
        } catch (PreprocessorException e) {
            throw new PreprocessorException(
                    "In the expansion of file '" + inputFile.getFullName() + "'",
                    e, inputFile
            );
        }
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
                    lastLines,
                    inputFile
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
                    },
                    inputFile
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
                    },
                    inputFile
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
                    withoutInclude.length() - 2,
                    inputFile
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
                    lastLines,
                    inputFile
            );
        }
    }
}

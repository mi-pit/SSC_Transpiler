package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.Processor;
import cz.mipit.sscc.ssc.exceptions.children.PreprocessorException;
import cz.mipit.sscc.util.ExitValue;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Preprocessor implements Processor {
    private static final String SSC_HEADER_FILE_SUFFIX = "ssch";

    private static final String INCLUDE_DIRECTIVE_NAME = "include";

    private static final int N_LINES = 4;

    private final InputFile inputFile;
    private final Path outputFile;

    private final LinkedList<EnumeratedLine> lastLines;

    private int currentLineNumber;
    private String currentLine;
    private boolean comment;

    public Preprocessor(@NotNull final InputFile inputFile,
                        @Nullable final Path outputFileAbsolutePath) {
        if (outputFileAbsolutePath != null && !outputFileAbsolutePath.isAbsolute()) {
            throw new IllegalArgumentException("Output file path must be absolute");
        }

        currentLineNumber = 1;
        currentLine = null;
        comment = false;

        lastLines = new LinkedList<>();

        this.inputFile = Objects.requireNonNull(inputFile);
        this.outputFile = outputFileAbsolutePath;
    }

    private Preprocessor(final InputFile inputFile) {
        this(inputFile, null);
    }

    private static List<String> getLinesFromPath(final Path path) throws IOException {
        final String read = Files.readString(path);
        return SSCCUtil.Text.splitLogicalLines(read);
    }

    public ExitValue run() throws IOException {
        final List<String> preprocessedLines = processLines(
                getLinesFromPath(inputFile.toAbsolutePath()),
                inputFile.dir()
        );

        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }

        Files.write(outputFile.toAbsolutePath(), preprocessedLines);
        return ExitValue.SUCCESS;
    }

    private List<String> processLines(final List<String> lines,
                                      final Path dir) throws IOException {
        final List<String> outputLines = new ArrayList<>(lines.size());

        for (final String line : lines) {
            currentLine = line;

            lastLines.add(new EnumeratedLine(currentLineNumber++, line));
            if (lastLines.size() > N_LINES) {
                lastLines.poll();
            }

            processLine(outputLines, dir);
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

    private void processLine(final List<String> outputLines,
                             final Path baseDir)
            throws IOException {
        final String commentsRemoved = removeComments(currentLine);
        final Optional<String> maybeWithoutHash = getWithoutHash(commentsRemoved);
        if (maybeWithoutHash.isEmpty()) {
            outputLines.add(commentsRemoved);
            return;
        }

        final String withoutHash = maybeWithoutHash.get();
        Main.logger.printDebug("\tWithout hash:    '" + withoutHash + "'");

        if (withoutHash.startsWith(INCLUDE_DIRECTIVE_NAME)) {
            processDirectiveInclude(outputLines, baseDir, withoutHash, commentsRemoved);
            return;
        }

        Main.logger.printDebug("\tNot an include");
        outputLines.add(commentsRemoved);
    }

    private void processDirectiveInclude(final List<String> outputLines,
                                         final Path baseDir, final String withoutHash,
                                         final String commentsRemoved)
            throws IOException {
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
                    index,
                    index + 1,
                    inputFile
            );
        }
        final char firstChar = getFirstChar(withoutInclude);
        if (firstChar == '<') {
            Main.logger.printDebug("\tNot quoted include");
            outputLines.add(commentsRemoved);
            return;
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

        final Path resolvedNormalized = tryGetPathFromString(filePathString, baseDir)
                .toAbsolutePath()
                .normalize();

        final boolean isSscHeader = SSC_HEADER_FILE_SUFFIX.equals(InputFile.fromAbsolutePath(resolvedNormalized).suffix());
        if (!isSscHeader) {
            final String newIncludeLine =
                    "#" + INCLUDE_DIRECTIVE_NAME + " \"" + resolvedNormalized + "\"" +
                            " /* resolved from " + filePathString + " */";
            outputLines.add(newIncludeLine);
            return;
        }

        Main.logger.printDebug("\tFile path:       '" + resolvedNormalized + "'");

        if (!Files.exists(resolvedNormalized)) {
            throw new PreprocessorException(
                    "Included file '" + resolvedNormalized + "' does not exist",
                    lastLines, currentLine.indexOf('"'), currentLine.lastIndexOf('"'), inputFile
            );
        }

        final List<String> linesLiteral = getLinesFromPath(resolvedNormalized);

        final InputFile subFile = InputFile.fromAbsolutePath(resolvedNormalized);
        final Preprocessor subFilePreprocessor = new Preprocessor(subFile);
        try {
            final Path fileDir = resolvedNormalized.getParent();
            final List<String> linesConverted = subFilePreprocessor.processLines(linesLiteral, fileDir);
            outputLines.addAll(linesConverted);
        } catch (final PreprocessorException e) {
            throw new PreprocessorException(
                    "In the expansion of file '" + inputFile.getFullName() + "'",
                    e, inputFile
            );
        }
    }

    private static Optional<String> getWithoutHash(String commentsRemoved) {
        final String trimmed = commentsRemoved.trim();

        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        if (!trimmed.startsWith("#")) {
            Main.logger.printDebug("\tNot a directive");
            return Optional.empty();
        }

        return Optional.of(trimmed.substring(1).trim());
    }

    private char getFirstChar(String withoutInclude) {
        final char firstChar = withoutInclude.charAt(0);
        final char lastChar = withoutInclude.charAt(withoutInclude.length() - 1);

        if (!(firstChar == '"' && lastChar == '"' || firstChar == '<' && lastChar == '>')) {
            throw new PreprocessorException(
                    String.format("Include argument `%s` is not terminated properly", withoutInclude),
                    lastLines,
                    new int[]{
                            currentLine.indexOf(firstChar),
                            currentLine.lastIndexOf(lastChar)
                    },
                    inputFile
            );
        }
        return firstChar;
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

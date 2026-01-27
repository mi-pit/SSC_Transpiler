package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.Processor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

    private static List<String> getPreprocessorLines(final Path path) throws IOException {
        final String read = Files.readString(path);
        return SSCCUtil.Text.splitLogicalLines(read);
    }

    public ExitValue run() throws IOException, SSCTranspilerException {
        final List<String> preprocessedLines = processLines(
                getPreprocessorLines(inputFile.toAbsolutePath()),
                inputFile.dir()
        );

        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
        }

        Files.write(outputFile, preprocessedLines);
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

            if (!line.isBlank()) {
                processLine(outputLines, dir);
            } else {
                outputLines.add(currentLine);
            }
        }

        return outputLines;
    }

    private String removeComments(final String line) {
        final StringBuilder dynstring = new StringBuilder();

        /* `bool stringLiteral` is local since strings may only span one line.
         * `bool comment` may span multiple lines => global */
        boolean stringLiteral = false;

        for (int i = 0; i < line.length(); ++i) {
            final char curr = line.charAt(i);

            if (comment) {
                if (i + 1 < line.length() && curr == '*' && line.charAt(i + 1) == '/') {
                    comment = false;
                    ++i;
                }
                continue;
            }

            if (curr == '"') {
                stringLiteral = !stringLiteral;
                dynstring.append(curr);
                continue;
            }

            if (i + 1 >= line.length()) {
                dynstring.append(curr);
                break;
            }

            if (!stringLiteral && curr == '/') {
                final char next = line.charAt(i + 1);
                // `/*`
                if (next == '*') {
                    comment = true;
                    ++i;
                    continue;
                }
                // `//`
                if (next == '/') {
                    /* line comment => skip rest of line */
                    break;
                }
            }

            dynstring.append(curr);
        }

        return dynstring.toString();
    }

    private void processLine(final List<String> outputLines,
                             final Path baseDir)
            throws IOException {
        final String commentsRemoved = removeComments(currentLine);
        if (commentsRemoved.isEmpty()) {
            return;
        }
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
                    "Include directive argument must be more than one character",
                    lastLines,
                    index,
                    index + 1,
                    inputFile
            );
        }
        final char firstChar = getFirstChar(withoutInclude);
        final String strippedIncludeArg = withoutInclude
                .substring(1, withoutInclude.length() - 1);
        if (strippedIncludeArg.isBlank()) {
            throw new PreprocessorException(
                    "Empty file path string",
                    lastLines,
                    currentLine.lastIndexOf(withoutInclude) + 1,
                    withoutInclude.length() - 2,
                    inputFile
            );
        }

        if (firstChar == '<') {
            Main.logger.printDebug("\tNot quoted include");
            return;
        }

        final Path resolvedNormalized = tryGetPathFromString(strippedIncludeArg, baseDir)
                .toAbsolutePath()
                .normalize();

        final InputFile newFile = InputFile.fromAbsolutePath(resolvedNormalized);
        final boolean isSscHeader = SSC_HEADER_FILE_SUFFIX.equals(newFile.suffix());
        if (!isSscHeader) {
            handleNonSSCHeaders(strippedIncludeArg, outputLines, newFile, resolvedNormalized);
            return;
        }

        Main.logger.printDebug("\tFile path:       '" + resolvedNormalized + "'");

        if (!Files.exists(resolvedNormalized)) {
            throw new PreprocessorException(
                    "Included file '" + resolvedNormalized + "' does not exist",
                    lastLines, currentLine.indexOf('"'), currentLine.lastIndexOf('"'), inputFile
            );
        }

        final InputFile subFile = InputFile.fromAbsolutePath(resolvedNormalized);
        processSubFile(outputLines, subFile, resolvedNormalized);
    }

    private void processSubFile(final List<String> outputLines,
                                final InputFile subFile,
                                final Path subFilePath) throws IOException {
        final List<String> linesLiteral = getPreprocessorLines(subFilePath);
        final Preprocessor subFilePreprocessor = new Preprocessor(subFile);
        try {
            final Path fileDir = subFilePath.getParent();
            final List<String> linesConverted = subFilePreprocessor.processLines(linesLiteral, fileDir);
            outputLines.addAll(linesConverted);
        } catch (final PreprocessorException e) {
            throw new PreprocessorException(e, inputFile);
        }
    }

    private void handleNonSSCHeaders(final String strippedIncludeArg,
                                     final List<String> outputLines,
                                     final InputFile subFile,
                                     final Path subFilePath) throws IOException {
        if (Files.exists(subFilePath)) {
            processSubFile(outputLines, subFile, subFilePath);
            return;
        }

        Main.logger.printDebug("Included file not found: '" + subFilePath + "'");
        Main.logger.printDebug("Treating as a <std> header");

        final String converted = "#include <" + strippedIncludeArg + "> /* resolved from " + inputFile.getFullName() + " */";
        outputLines.add(converted);

        Main.logger.printDebug("Added include: " + converted);

        throw new PreprocessorException("Could not find file \"" + strippedIncludeArg + "\"", lastLines, inputFile);
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

    /**
     * Verifies file and returns first character ({@code "} or {@code <})
     */
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

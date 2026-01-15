package cz.mipit.sscc.ssc.preprocessor;

import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.macro.Macro;
import cz.mipit.sscc.ssc.compiler.data.macro.MacroBodyMember;
import cz.mipit.sscc.ssc.exceptions.children.PreprocessorException;
import cz.mipit.sscc.util.SSCCUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Preprocessor {
    private static final String SSCH_FILE_SUFFIX = "ssch";

    private static final String INCLUDE_DIRECTIVE_NAME = "include";
    private static final String DEFINE_DIRECTIVE_NAME = "define";

    private static final int N_LINES = 4;

    private final InputFile inputFile;

    private final LinkedList<EnumeratedLine> lastLines;
    private final Map<String, Macro> macros;

    private int currentLineNumber;
    private String currentLine;
    private boolean comment;

    private Preprocessor(final InputFile inputFile, final Map<String, Macro> macros) {
        currentLineNumber = 1;
        currentLine = null;
        comment = false;

        lastLines = new LinkedList<>();
        this.macros = macros;

        this.inputFile = inputFile;
    }

    public static boolean preprocessSSC(final InputFile inputFile,
                                        final Path outputFileAbsolutePath,
                                        final Map<String, Macro> macros) throws IOException {
        assert outputFileAbsolutePath.isAbsolute();
        if (!new Preprocessor(inputFile, macros).writeToOutput(outputFileAbsolutePath)) {
            return false;
        }

        Main.logger.printDebug("Preprocessing success");
        return true;
    }

    private static List<String> getLinesFromPath(final Path path) throws IOException {
        final String read = Files.readString(path);
        return SSCCUtil.Text.splitLogicalLines(read);
    }

    private boolean writeToOutput(final Path outputFileAbsolutePath)
            throws IOException {
        if (!outputFileAbsolutePath.isAbsolute()) {
            throw new IllegalArgumentException("Output file path must be absolute");
        }

        final List<String> preprocessedLines = processLines(
                getLinesFromPath(inputFile.toAbsolutePath()),
                inputFile.dir()
        );

        if (!Files.exists(outputFileAbsolutePath)) {
            Files.createFile(outputFileAbsolutePath);
        }

        Files.write(outputFileAbsolutePath.toAbsolutePath(), preprocessedLines);
        return true;
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
        } else if (withoutHash.startsWith(DEFINE_DIRECTIVE_NAME)) {
            outputLines.add(currentLine);
            processDirectiveDefine(withoutHash);
            return;
        }

        Main.logger.printDebug("\tNot an include or define");
        outputLines.add(commentsRemoved);
    }

    private void processDirectiveDefine(final String withoutHash) {
        final String withoutDefine = withoutHash.substring(DEFINE_DIRECTIVE_NAME.length());
        Main.logger.printDebug("\tWithout define:  '" + withoutDefine + "'");
        if (withoutDefine.isEmpty()) {
            throw new PreprocessorException("Empty define directive", lastLines, inputFile);
        }

        if (!Character.isWhitespace(withoutDefine.charAt(0))) {
            throw new PreprocessorException("Invalid directive", lastLines, inputFile);
        }

        final Macro macro = parseMacro(withoutDefine.trim());
        Main.logger.printDebug(macro.toString());
        macros.put(macro.identifier(), macro);
    }

    private Macro parseMacro(final String withoutDefine) {
        if (Character.isDigit(withoutDefine.charAt(0))) {
            throw new PreprocessorException(
                    "Invalid macro identifier character", lastLines,
                    new int[]{currentLine.indexOf(withoutDefine)}, inputFile
            );
        }
        final StringBuilder identifier = new StringBuilder();

        boolean stillIdentifier = true;
        List<String> args = null;
        List<MacroBodyMember> replacements = null;

        for (int i = 0; i < withoutDefine.length(); i++) {
            final char c = withoutDefine.charAt(i);
            if (stillIdentifier) {
                if (charIsIdentifier(c)) {
                    identifier.append(c);
                    continue;
                }
                stillIdentifier = false;

                if (c == '(') {
                    final int closingBracketIdx = withoutDefine.indexOf(')', i + 1);
                    if (closingBracketIdx == -1) {
                        throw new PreprocessorException("Missing ')'", lastLines,
                                new int[]{currentLine.indexOf(c)}, inputFile);
                    }
                    final String[] split = withoutDefine.substring(i + 1, closingBracketIdx).split(",");
                    for (int idx = 0; idx < split.length; idx++) {
                        split[idx] = split[idx].trim();
                    }
                    args = Arrays.asList(split);
                } else if (!Character.isWhitespace(c)) {
                    throw new PreprocessorException(
                            "Invalid macro identifier character", lastLines,
                            new int[]{currentLine.indexOf(c)}, inputFile
                    );
                }
            } else {
                replacements = new ArrayList<>();
                /* todo */
                if ("streq".contentEquals(identifier)) {
                    final String rep = "( strcmp( S1 , S2 ) == 0 )";
                    final String[] spl = rep.split("\\s+");
                    for (final String str : spl) {
                        replacements.add(new MacroBodyMember(str.equals("S1") || str.equals("S2"), str));
                    }
                } else {
                    replacements.add(new MacroBodyMember(false, withoutDefine.substring(i)));
                    break;
                }
            }
        }

        return new Macro(identifier.toString(), args, replacements);
    }

    private static boolean charIsIdentifier(final char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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

        final boolean isSscHeader = SSCH_FILE_SUFFIX.equals(InputFile.fromAbsolutePath(resolvedNormalized).suffix());
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
        final Preprocessor subFilePreprocessor = new Preprocessor(subFile, macros);
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

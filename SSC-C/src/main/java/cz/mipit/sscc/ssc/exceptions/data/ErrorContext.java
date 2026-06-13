package cz.mipit.sscc.ssc.exceptions.data;

import antlr.ssc.SSCLexer;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.util.color.ConsoleColor;
import cz.mipit.sscc.util.color.ConsoleColorFactory;
import cz.mipit.sscc.util.color.UnixTerminalColor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErrorContext {
    private static final ConsoleColor COLOR_CODE = ConsoleColorFactory.create(ConsoleColorFactory.Ground.FORE, ConsoleColorFactory.Color.WHITE);

    private static final ConsoleColor COLOR_CODE_BOLD;
    private static final Pattern LINE_NUM_PATTERN = Pattern.compile(
            "^#\\s*(?:line\\s+)?(?<num>\\d+)(?:\\s+\"(?<file>[^\"]+)\")?"
    );

    static {
        if (ConsoleColorFactory.FROM_OS == ConsoleColorFactory.UNIX) {
            COLOR_CODE_BOLD = new UnixTerminalColor("\u001B[1m" + COLOR_CODE);
        } else {
            COLOR_CODE_BOLD = COLOR_CODE;
        }
    }

    private final List<EnumeratedLine> enumeratedLines;
    private final String filename;


    public ErrorContext(
            List<EnumeratedLine> enumeratedLines,
            String filename
    ) {
        this.enumeratedLines = enumeratedLines;
        this.filename = filename;
    }

    public static ErrorContext getApparentContext(
            final Token token,
            final CommonTokenStream tokens,
            final int before,
            final int after
    ) {
        final String fullText = tokens.getTokenSource().getInputStream().toString();
        final String[] lines = fullText.split(System.lineSeparator(), -1);

        final int errorLineIdx = token.getLine() - 1;
        final int start = Math.max(0, errorLineIdx - before);
        final int end = Math.min(lines.length - 1, errorLineIdx + after);

        tokens.fill();

        final int errorTokenIndex = token.getTokenIndex();

        int directiveRawLine = -1;
        int directiveValue = -1;
        String apparentFilename = null;

        // Walk backward from the error token to find the nearest line directive
        for (int i = Math.min(errorTokenIndex, tokens.size() - 1); i >= 0; i--) {
            final Token t = tokens.get(i);
            if (t.getChannel() != SSCLexer.LINEDIRECTIVECHANNEL) {
                continue;
            }

            final Matcher matcher = LINE_NUM_PATTERN.matcher(t.getText().trim());
            if (!matcher.find()) {
                Main.logger.printDebug(
                        "Strange... A token in the line directive channel doesn't fit the line directive regex."
                );
                continue;
            }

            if (directiveValue == -1) {
                directiveValue = Integer.parseInt(matcher.group("num"));
                directiveRawLine = t.getLine();
            }

            // Keep walking backward if this specific directive lacked a filename string
            if (matcher.group("file") != null) {
                apparentFilename = matcher.group("file");
                break;
            }
        }
        assert apparentFilename != null;

        final Set<Integer> directiveLines = getVisibleDirectiveLines(
                start, end, tokens
        );

        final List<EnumeratedLine> ls = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            if (directiveLines.contains(i)) {
                continue;
            }

            final int rawLineNumber = i + 1;
            final int apparentLineNumber;

            if (directiveRawLine != -1 && rawLineNumber >= directiveRawLine) {
                apparentLineNumber = directiveValue + (rawLineNumber - (directiveRawLine + 1));
            } else {
                apparentLineNumber = rawLineNumber;
            }

            final String line = lines[i];
            ls.add(new EnumeratedLine(apparentLineNumber, line));
        }

        return new ErrorContext(ls, apparentFilename);
    }

    private static Set<Integer> getVisibleDirectiveLines(
            final int start,
            final int end,
            final CommonTokenStream tokens
    ) {
        final Set<Integer> visibleDirectiveLines = new HashSet<>();

        int rawWindowStartLine = start + 1;
        int rawWindowEndLine = end + 1;

        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            final int tokenLine = t.getLine();

            if (tokenLine < rawWindowStartLine) {
                continue;
            }
            if (tokenLine > rawWindowEndLine) {
                break;
            }

            if (t.getChannel() == SSCLexer.LINEDIRECTIVECHANNEL) {
                visibleDirectiveLines.add(tokenLine - 1);
            }
        }

        return visibleDirectiveLines;
    }

    public static ErrorContext getApparentContext(
            final ParseTree ctx,
            final CommonTokenStream tokens,
            final int linesBefore,
            final int linesAfter
    ) {
        if (ctx instanceof ParserRuleContext prc) {
            return getApparentContext(prc.getStart(), tokens, linesBefore, linesAfter);
        }
        if (ctx instanceof TerminalNode t) {
            return getApparentContext(t.getSymbol(), tokens, linesBefore, linesAfter);
        }
        throw new IllegalArgumentException("Unrecognized ParseTree type: " + ctx.getClass().getName());
    }

    public String filename() {
        return filename;
    }

    public int lastLineNumber() {
        return enumeratedLines.getLast().lineNumber();
    }

    @Override
    public String toString() {
        return EnumeratedLine.formatLines(enumeratedLines, COLOR_CODE_BOLD);
    }
}

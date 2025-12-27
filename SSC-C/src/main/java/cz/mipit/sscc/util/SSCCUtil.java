package cz.mipit.sscc.util;

import cz.mipit.sscc.ssc.preprocessor.EnumeratedLine;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.lineSeparator;

public final class SSCCUtil {
    private SSCCUtil() {
    }

    public static class Text {
        /**
         * Returns the exact text corresponding to a ParserRuleContext.
         * Works for any context.
         */
        public static String getLiteral(ParserRuleContext ctx, CommonTokenStream tokens) {
            final int start = ctx.getStart().getTokenIndex();
            final int stop = ctx.getStop().getTokenIndex();
            return tokens.getText(Interval.of(start, stop));
        }

        /**
         * Retrieves lines before and after the given token.
         *
         * @return {@link ArrayList} of {@code before + 1 + after}-many {@link EnumeratedLine}s
         */
        public static List<EnumeratedLine> getLinesAroundToken(final Token token,
                                                               final CommonTokenStream tokens,
                                                               final int before,
                                                               final int after) {
            final String fullText = tokens.getTokenSource().getInputStream().toString();
            final String[] lines = fullText.split(lineSeparator(), -1);

            final int lineIndex = token.getLine() - 1;
            final int start = Math.max(0, lineIndex - before);
            final int end = Math.min(lines.length - 1, lineIndex + after);

            final List<EnumeratedLine> ls = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                ls.add(new EnumeratedLine(i, lines[i]));
            }

            return ls;
        }

        public static List<String> splitLogicalLines(String input) {
            List<String> lines = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            final String[] physicalLines = input.split("\\R", -1);

            for (String line : physicalLines) {
                if (line.endsWith("\\")) {
                    /* remove the trailing backslash and continue */
                    current.append(line, 0, line.length() - 1);
                } else {
                    current.append(line);
                    lines.add(current.toString());
                    current.setLength(0);
                }
            }

            if (!current.isEmpty()) {
                lines.add(current.toString());
            }

            return lines;
        }
    }

    public static class Maths {
        public static int digitsof(int num) {
            int ndigs = 0;
            while (num > 0) {
                num /= 10;
                ++ndigs;
            }

            return ndigs;
        }

        public static int[] getRange(int from, int to) {
            int[] array = new int[to - from + 1];
            for (int i = 0; i < array.length; i++) {
                array[i] = i + from;
            }
            return array;
        }
    }
}

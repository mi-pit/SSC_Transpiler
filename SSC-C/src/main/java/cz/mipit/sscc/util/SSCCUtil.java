package cz.mipit.sscc.util;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.exceptions.data.EnumeratedLine;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

import static cz.mipit.sscc.util.SSCCUtil.Maths.digitsOf;
import static java.lang.System.lineSeparator;

public final class SSCCUtil {
    public static TerminalNode getIdentifierFromDeclarator(SSCParser.DeclaratorContext declarator) {
        // declarator: (pointer declarationSpecifiers?)* directDeclarator
        while (declarator.directDeclarator().declarator() != null) {
            declarator = declarator.directDeclarator().declarator();
        }

        return declarator.directDeclarator().Identifier();
    }

    public static class Text {
        public static final String INDENT = "    ";
        public static final int LINES_BEFORE = 4;
        public static final int LINES_AFTER = 0;

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
        public static List<EnumeratedLine> getLinesAroundToken(
                final Token token,
                final CommonTokenStream tokens,
                final int before,
                final int after
        ) {
            final String fullText = tokens.getTokenSource().getInputStream().toString();
            final String[] lines = fullText.split(lineSeparator(), -1);

            final int lineIndex = token.getLine() - 1;
            final int start = Math.max(0, lineIndex - before);
            final int end = Math.min(lines.length - 1, lineIndex + after);

            final List<EnumeratedLine> ls = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                ls.add(new EnumeratedLine(i + 1, lines[i]));
            }

            return ls;
        }

        public static int getLineNumberLength(final int min, final int max) {
            return Math.max(1, Math.max(digitsOf(min), digitsOf(max)));
        }
    }

    public static class Maths {
        public static int digitsOf(int num) {
            int ndigs = 0;
            while (num > 0) {
                num /= 10;
                ++ndigs;
            }

            return ndigs;
        }

        public static boolean isPowerOfTwo(long l) {
            return (l & (l - 1)) == 0;
        }
    }


    private SSCCUtil() {
    }
}

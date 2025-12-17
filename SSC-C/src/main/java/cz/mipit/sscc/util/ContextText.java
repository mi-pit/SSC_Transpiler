package cz.mipit.sscc.util;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cz.mipit.sscc.util.UnixTerminalColors.COLOR_RESET;

public final class ContextText {
    private ContextText() {
    }

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
     * Gets as many tokens from around the given one as you want. Collapses whitespace.
     */
    public static String getContextAroundToken(final Token token,
                                               final CommonTokenStream tokens,
                                               final int back,
                                               final int forward) {
        final List<Token> all = tokens.getTokens();
        final int n = all.size();
        final int index = token.getTokenIndex();

        final List<String> collected = new ArrayList<>(back + 1 + forward);

        // --- Collect backward tokens ---
        int i = index - 1;
        int backCount = 0;
        while (i >= 0 && backCount < back) {
            Token t = all.get(i);
            String text = t.getText();
            if (!text.isBlank()) {
                collected.add(0, text); // prepend in correct order
                backCount++;
            }
            i--;
        }

        // --- Current token ---
        collected.add(token.getText());

        // --- Collect forward tokens ---
        i = index + 1;
        int forwardCount = 0;
        while (i < n && forwardCount < forward) {
            Token t = all.get(i);
            String text = t.getText();
            if (!text.isBlank()) {
                collected.add(text);
                forwardCount++;
            }
            i++;
        }

        return String.join(" ", collected);
    }


    private static final int MAX_LINE_LEN = 150;

    public static String getLinesAroundToken(final Token token,
                                             final CommonTokenStream tokens,
                                             final int before,
                                             final int after) {
        String fullText = tokens.getTokenSource().getInputStream().toString();
        String[] lines = fullText.split("\n", -1);

        int lineIndex = token.getLine() - 1;
        int start = Math.max(0, lineIndex - before);
        int end = Math.min(lines.length - 1, lineIndex + after);

        final List<String> ls = new ArrayList<>(Arrays.asList(lines).subList(start, end + 1))
                .stream()
                .map(line -> line.length() < MAX_LINE_LEN
                        ? line
                        : line.substring(0, MAX_LINE_LEN - 2) + "...")
                .toList();
        return String.join("\n", ls);
    }


    public static String getLocalizationMessage(Token token, String color) {
        final int offset = Math.min(token.getCharPositionInLine(), MAX_LINE_LEN);
        final int desiredLen = token.getStopIndex() - token.getStartIndex() + 1;
        final int len = Math.min(desiredLen, MAX_LINE_LEN - offset);
        return " ".repeat(offset) + color + "^".repeat(len) + (desiredLen > len ? " there ->" : " here") + COLOR_RESET;
    }
}

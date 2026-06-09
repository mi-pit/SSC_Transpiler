package cz.mipit.sscc.ssc.exceptions.data;

import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.color.ConsoleColor;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

import static cz.mipit.sscc.util.SSCCUtil.Text.getLineNumberLength;
import static cz.mipit.sscc.util.Util.requirePositive;
import static cz.mipit.sscc.util.color.ConsoleColorFactory.COLOR_DEFAULT;
import static java.lang.System.lineSeparator;

public record EnumeratedLine(int lineNumber, String line) {
    public EnumeratedLine(int lineNumber, String line) {
        this.lineNumber = requirePositive(lineNumber);
        this.line = line;
    }


    public static List<EnumeratedLine> getLines(
            Token token, CommonTokenStream tokens,
            int linesBefore, int linesAfter
    ) {
        return SSCCUtil.getLinesAroundToken(token, tokens, linesBefore, linesAfter);
    }

    public static List<EnumeratedLine> getLines(
            final ParseTree ctx,
            final CommonTokenStream tokens,
            final int linesBefore,
            final int linesAfter
    ) {
        if (ctx instanceof ParserRuleContext prc) {
            return getLines(prc.getStart(), tokens, linesBefore, linesAfter);
        }
        if (ctx instanceof TerminalNode t) {
            return getLines(t.getSymbol(), tokens, linesBefore, linesAfter);
        }
        throw new IllegalArgumentException("Unrecognized ParseTree type: " + ctx.getClass().getName());
    }


    public static String formatLines(
            final List<EnumeratedLine> lines,
            final ConsoleColor codeColor
    ) {
        final StringBuilder sBuilder = new StringBuilder(256)
                .append(codeColor);

        final int fst = lines.getFirst().lineNumber();
        final int last = lines.getLast().lineNumber();
        final String fmtstr = "%" + getLineNumberLength(fst, last) + "d";

        for (int i = 0; i < lines.size(); i++) {
            final EnumeratedLine line = lines.get(i);

            final String formatted = String.format(fmtstr, line.lineNumber());
            sBuilder.append(formatted)
                    .append(SSCTranspilerException.LINENO_SEPARATOR)
                    .append(line.line());

            if (i < lines.size() - 1) {
                sBuilder.append(lineSeparator());
            }
        }

        return "" + sBuilder + COLOR_DEFAULT;
    }
}

package cz.mipit.sscc.ssc.exceptions.data;

import cz.mipit.sscc.util.color.ConsoleColor;
import cz.mipit.sscc.util.color.ConsoleColorFactory;
import cz.mipit.sscc.util.color.UnixTerminalColor;

import java.util.List;

public class ErrorContext {
    private static final ConsoleColor COLOR_CODE = ConsoleColorFactory.create(ConsoleColorFactory.Ground.FORE, ConsoleColorFactory.Color.WHITE);

    private static final ConsoleColor COLOR_CODE_BOLD;

    static {
        if (ConsoleColorFactory.FROM_OS == ConsoleColorFactory.UNIX) {
            COLOR_CODE_BOLD = new UnixTerminalColor("\u001B[1m" + COLOR_CODE);
        } else {
            COLOR_CODE_BOLD = COLOR_CODE;
        }
    }

    private final List<EnumeratedLine> enumeratedLines;

    public ErrorContext(List<EnumeratedLine> enumeratedLines) {
        this.enumeratedLines = enumeratedLines;
    }

    @Override
    public String toString() {
        return EnumeratedLine.formatLines(enumeratedLines, COLOR_CODE_BOLD);
    }
}

package cz.mipit.sscc.ssc.preprocessor;

public record EnumeratedLine(int lineNumber, String line) {
    public EnumeratedLine(int lineNumber, String line) {
        this.lineNumber = requirePositive(lineNumber);
        this.line = line;
    }

    private static int requirePositive(final int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Invalid line number: " + n);
        }

        return n;
    }
}

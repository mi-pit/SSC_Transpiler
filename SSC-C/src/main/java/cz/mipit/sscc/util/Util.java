package cz.mipit.sscc.util;

public final class Util {
    private Util() {
    }

    public static String requireNonBlank(String string) {
        if (string.isBlank()) {
            throw new IllegalArgumentException();
        }
        return string;
    }

    public static long requirePositive(long number) {
        if (number <= 0) {
            throw new IllegalArgumentException();
        }
        return number;
    }

    public static int requirePositive(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException();
        }
        return number;
    }
}

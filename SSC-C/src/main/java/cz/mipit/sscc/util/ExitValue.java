package cz.mipit.sscc.util;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    LIBRARY_NOT_FOUND,
    TRANSPILATION_FAIL,
    INTERNAL_ERROR,
    C_COMPILATION_FAIL,
    IO_EXCEPTION,
    ;

    public String humanReadable() {
        return name()
                .replaceAll("_", " ")
                .toLowerCase();
    }
}

package cz.mipit.sscc.util;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    TRANSPILATION_FAIL,
    C_COMPILATION_FAIL,
    LIBRARY_NOT_FOUND,
    ;

    public String humanReadable() {
        return name()
                .replaceAll("_", " ")
                .toLowerCase();
    }
}

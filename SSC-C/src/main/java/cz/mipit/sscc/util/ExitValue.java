package cz.mipit.sscc.util;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    LIBRARY_NOT_FOUND,
    TRANSPILATION_FAIL,
    C_COMPILATION_FAIL,
    ;

    public String humanReadable() {
        return name()
                .replaceAll("_", " ")
                .toLowerCase();
    }
}

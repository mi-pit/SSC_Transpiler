package cz.mipit.sscc.util;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    TRANSPILATION_FAIL,
    C_PREPROCESSING_FAIL,
    C_VERIFICATION_FAIL,
    C_COMPILATION_FAIL,
    IO_EXCEPTION;

    @Override
    public String toString() {
        return super.toString().replaceAll("_", " ");
    }
}

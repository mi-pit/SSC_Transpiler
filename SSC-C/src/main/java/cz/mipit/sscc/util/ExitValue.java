package cz.mipit.sscc.util;

import java.io.IOException;
import java.util.function.BinaryOperator;

public enum ExitValue {
    SUCCESS /* = 0 */,
    INVALID_ARGUMENTS,
    LIBRARY_NOT_FOUND,
    TRANSPILATION_FAIL,
    C_COMPILATION_FAIL,

    // Any unexpected runtime exception
    INTERNAL_ERROR,

    // The two non-runtime exceptions thrown in code
    IO_EXCEPTION,
    INTERRUPTED_EXCEPTION,
    ;

    public boolean isSuccess() {
        return this == SUCCESS;
    }

    public boolean isFailure() {
        return !isSuccess();
    }


    public String humanReadable() {
        return name()
                .replaceAll("_", " ")
                .toLowerCase();
    }

    public static ExitValue fromException(Exception e) {
        if (e instanceof IOException) {
            return IO_EXCEPTION;
        }
        if (e instanceof InterruptedException) {
            return INTERRUPTED_EXCEPTION;
        }
        return INTERNAL_ERROR;
    }

    public static final BinaryOperator<ExitValue> REDUCTION = (e1, e2) ->
            e1 == e2 || e1.ordinal() > e2.ordinal()
                    ? e1
                    : e2;
}

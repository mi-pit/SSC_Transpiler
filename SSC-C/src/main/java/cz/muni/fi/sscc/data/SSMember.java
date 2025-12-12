package cz.muni.fi.sscc.data;

import cz.muni.fi.sscc.util.Either;

import java.util.Objects;
import java.util.function.Function;

public record SSMember(Either<Field, FunctionDefinition> data) {
    public static SSMember field(final Field value) {
        return factory(false, value);
    }

    public static SSMember function(final FunctionDefinition value) {
        return factory(true, value);
    }

    private static SSMember factory(final boolean isFunction,
                                    final Object value) {
        Objects.requireNonNull(value);
        return new SSMember(
                new Either<>() {
                    @Override
                    public <T> T map(Function<? super Field, ? extends T> lFunc,
                                     Function<? super FunctionDefinition, ? extends T> rFunc) {
                        return isFunction
                                ? rFunc.apply((FunctionDefinition) value)
                                : lFunc.apply((Field) value);
                    }
                }
        );
    }
}

package cz.muni.fi.sscc.data;

import cz.muni.fi.sscc.util.Either;

import java.util.Objects;
import java.util.function.Function;

public record SSMember(Either<Declaration, FunctionDefinition> data) {
    public static SSMember declaration(Declaration value) {
        Objects.requireNonNull(value);
        return new SSMember(
                new Either<>() {
                    @Override
                    public <T> T map(Function<? super Declaration, ? extends T> lFunc,
                                     Function<? super FunctionDefinition, ? extends T> rFunc) {
                        return lFunc.apply(value);
                    }
                }
        );
    }

    public static SSMember function(FunctionDefinition value) {
        Objects.requireNonNull(value);
        return new SSMember(
                new Either<>() {
                    @Override
                    public <T> T map(Function<? super Declaration, ? extends T> lFunc,
                                     Function<? super FunctionDefinition, ? extends T> rFunc) {
                        return rFunc.apply(value);
                    }
                }
        );
    }

    public boolean isDeclaration() {
        return data.getRight().isEmpty();
    }

    public boolean isFunctionDefinition() {
        return data.getLeft().isEmpty();
    }
}

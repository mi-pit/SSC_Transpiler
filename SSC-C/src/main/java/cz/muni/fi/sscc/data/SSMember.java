package cz.muni.fi.sscc.data;

import cz.muni.fi.sscc.util.Either;

import java.util.Objects;

public record SSMember(Either<Declaration, FunctionDefinition> data) {
    public static SSMember declaration(Declaration value) {
        Objects.requireNonNull(value);
        return new SSMember(Either.left(value));
    }

    public static SSMember function(FunctionDefinition value) {
        Objects.requireNonNull(value);
        return new SSMember(Either.right(value));
    }

    public boolean isDeclaration() {
        return data.getRight().isEmpty();
    }

    public boolean isFunctionDefinition() {
        return data.getLeft().isEmpty();
    }
}

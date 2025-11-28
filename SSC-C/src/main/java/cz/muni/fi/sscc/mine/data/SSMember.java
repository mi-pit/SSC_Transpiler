package cz.muni.fi.sscc.mine.data;

import cz.muni.fi.sscc.util.Either;

import java.util.Objects;
import java.util.Optional;

public class SSMember {
    private final Either<Declaration, FunctionDefinition> data;

    private SSMember(Either<Declaration, FunctionDefinition> data) {
        this.data = data;
    }

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

    public Either<Declaration, FunctionDefinition> getData() {
        return data;
    }
}

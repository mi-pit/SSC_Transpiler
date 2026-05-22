package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.util.Either;

public class SSMember {
    private final Either<Field, SuperstructMethod> data;

    private SSMember(final Either<Field, SuperstructMethod> data) {
        this.data = data;
    }

    public static SSMember field(final Field value) {
        return new SSMember(Either.left(value));
    }

    public static SSMember function(final SuperstructMethod value) {
        return new SSMember(Either.right(value));
    }

    public Either<Field, SuperstructMethod> data() {
        return data;
    }

    @Override
    public String toString() {
        return "SSMember{" +
                "data=" + data +
                '}';
    }
}

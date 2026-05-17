package cz.mipit.sscc.ssc.compiler.data.lambda;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class LambdaFunction implements Comparable<LambdaFunction> {
    private final String returnType;
    private final String params;
    private final String body;
    private final String attributes;

    private final String name;

    private final long id;
    private static final AtomicLong nextId = new AtomicLong(0);

    public static String createName(
            final String surroundingFunctionName
    ) {
        return "SSC_LAMBDA_DeclaredIn__" + surroundingFunctionName;
    }

    public LambdaFunction(
            String prettifiedName,
            String returnType,
            String params,
            String ctx,
            String attributes
    ) {
        this.returnType = Objects.requireNonNull(returnType);
        this.params = Objects.requireNonNull(params);
        this.body = Objects.requireNonNull(ctx);
        this.attributes = Objects.requireNonNull(attributes);

        id = nextId.getAndIncrement();

        this.name = prettifiedName + "__ID" + id;
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return attributes + " static " + returnType + " " + getName() + "(" + params + ")" + body;
    }

    @Override
    public int compareTo(LambdaFunction o) {
        Objects.requireNonNull(o);
        return Long.compare(id, o.id);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof LambdaFunction that))
            return false;
        return compareTo(that) == 0;
    }

    @Override
    public int hashCode() {
        return (int) id;
    }
}

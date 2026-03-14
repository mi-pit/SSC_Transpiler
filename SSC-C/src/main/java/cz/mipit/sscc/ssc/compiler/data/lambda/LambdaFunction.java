package cz.mipit.sscc.ssc.compiler.data.lambda;

import cz.mipit.sscc.file.InputFile;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class LambdaFunction implements Comparable<LambdaFunction> {
    private final String returnType;
    private final String params;
    private final String body;
    private final String attributes;

    private final String prettifier;

    private final long id;
    private static final AtomicLong nextId = new AtomicLong(0);

    public LambdaFunction(
            InputFile inFile,
            String functionName,
            String returnType,
            String params,
            String ctx,
            String attributes
    ) {
        this.returnType = Objects.requireNonNull(returnType);
        this.params = Objects.requireNonNull(params);
        this.body = Objects.requireNonNull(ctx);
        this.attributes = Objects.requireNonNull(attributes);

        final String fileNamePrettifier =
                (inFile.name() + "_" + inFile.suffix())
                        .chars()
                        .mapToObj(i -> {
                            if ((i >= 'A' && i <= 'Z') || (i >= 'a' && i <= 'z'))
                                return (char) i;
                            else
                                return '_';
                        })
                        .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
                        .toString();

        this.prettifier = fileNamePrettifier + "_" + functionName;

        id = nextId.getAndIncrement();
    }

    public String getName() {
        return "SSC_LAMBDA_FUNCTION__" + id + "__" + prettifier;
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

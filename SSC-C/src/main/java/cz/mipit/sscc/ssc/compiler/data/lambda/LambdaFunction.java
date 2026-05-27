package cz.mipit.sscc.ssc.compiler.data.lambda;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public final class LambdaFunction {
    private final String returnType;
    private final String params;
    private final String body;
    private final String attributes;

    private final String name;

    private static final AtomicLong nextId = new AtomicLong(0);

    public static String createName(
            final String surroundingFunctionName
    ) {
        return "SSClambda_from_" + surroundingFunctionName;
    }

    public LambdaFunction(
            String prettifiedName,
            String returnType,
            String params,
            String ctx,
            String attributes
    ) {
        this.returnType = " " + requireNonBlank(requireNonNull(returnType)) + " ";
        this.params = padIfBlank(requireNonNull(params), s -> " " + s + " ");
        this.body = requireNonNull(ctx);
        this.attributes = padIfBlank(requireNonNull(attributes), s -> s + " ");

        this.name = "ID" + nextId.getAndIncrement() + "_" + prettifiedName;
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return attributes + "static" + returnType + getName() + "(" + params + ")" + body;
    }

    private static String padIfBlank(
            final String string,
            final Function<String, String> mapper
    ) {
        if (!string.isBlank()) {
            return string;
        }

        return mapper.apply(string);
    }

    private static String requireNonBlank(String string) {
        if (string.isBlank()) {
            throw new IllegalArgumentException(string);
        }

        return string;
    }
}

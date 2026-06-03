package cz.mipit.sscc.ssc.compiler.data.lambda;

import cz.mipit.sscc.util.SSCCUtil;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import static cz.mipit.sscc.util.Util.requireNonBlank;
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
        return SSCCUtil.createNameWithID("__ssc_lambda", nextId, surroundingFunctionName);
    }

    public LambdaFunction(
            String prettifiedName,
            String returnType,
            String params,
            String ctx,
            String attributes
    ) {
        this.returnType = " " + requireNonBlank(requireNonNull(returnType)) + " ";
        this.params = padIfNotBlank(requireNonNull(params), s -> " " + s + " ");
        this.body = requireNonNull(ctx);
        this.attributes = padIfNotBlank(requireNonNull(attributes), s -> s + " ");

        this.name = requireNonNull(prettifiedName);
    }

    public String getName() {
        return name;
    }

    public String getDefinition() {
        return attributes + "static" + returnType + getName() + "(" + params + ")" + body;
    }

    private static String padIfNotBlank(
            final String string,
            final Function<String, String> mapper
    ) {
        if (!string.isBlank()) {
            return string;
        }

        return mapper.apply(string);
    }
}

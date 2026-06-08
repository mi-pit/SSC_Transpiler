package cz.mipit.sscc.ssc.compiler.data.lambda;

import cz.mipit.sscc.ssc.compiler.data.var.LiteralVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cz.mipit.sscc.util.Util.requireNonBlank;
import static java.util.Objects.requireNonNull;

public final class LambdaFunction {
    private final String returnType;
    private final String params;
    private final String body;
    private final String attributes;

    private final String name;


    public LambdaFunction(
            String prettifiedName,
            String returnType,
            String params,
            String ctx,
            String attributes,
            VisitorDispatcher dispatcher,
            List<LiteralVariable> captures
    ) {
        final String capturesJoined = (!captures.isEmpty() && !params.isBlank() ? ", " : "")
                                      + captures.stream()
                                              .map(v -> "/* capture */ __attribute__((unused)) " + v.getDeclaration(dispatcher))
                                              .collect(Collectors.joining(", "));

        this.returnType = " " + requireNonBlank(requireNonNull(returnType)) + " ";
        this.params = padIfNotBlank(requireNonNull(params), s -> " " + s + " ") + capturesJoined;
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

    public static String padIfNotBlank(
            final String string,
            final Function<String, String> mapper
    ) {
        if (string.isBlank()) {
            return string;
        }

        return mapper.apply(string);
    }
}

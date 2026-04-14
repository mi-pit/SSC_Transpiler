package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.Optional;
import java.util.SequencedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class LambdaConvertor
        extends AbstractConvertor<SSCParser.LambdaFunctionContext>
        implements EmittingConvertor<SSCParser.LambdaFunctionContext> {

    // must be sequenced so that nested lambdas get defined in the right order
    private final SequencedSet<LambdaFunction> lambdasCollectedInCurrentFunction;

    public LambdaConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);

        lambdasCollectedInCurrentFunction = new TreeSet<>();
    }

    @Override
    public String convert(SSCParser.LambdaFunctionContext ctx) {
        final LambdaFunction lambda = new LambdaFunction(
                dispatcher.getCurrentFile(),
                dispatcher.getCurrentFunctionName(),
                dispatcher.visitTypeName(ctx.typeName()),
                dispatcher.visitParameterTypeList(ctx.parameterTypeList()),
                dispatcher.visitFunctionBody(ctx.functionBody()),
                ctx.lambdaAttributes() != null
                        ? dispatcher.visitLambdaAttributes(ctx.lambdaAttributes())
                        : ""
        );
        lambdasCollectedInCurrentFunction.add(lambda);

        return lambda.getName();
    }

    /// Returns string of all lambdas cached and clears the cache
    @Override
    public Optional<String> emit() {
        if (lambdasCollectedInCurrentFunction.isEmpty()) {
            return Optional.empty();
        }

        final String lambdas = lambdasCollectedInCurrentFunction.stream()
                .map(LambdaFunction::getDefinition)
                .collect(Collectors.joining(System.lineSeparator()));
        lambdasCollectedInCurrentFunction.clear();
        return Optional.of(lambdas);
    }
}

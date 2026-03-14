package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;

import java.util.SequencedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;

public class LambdaConvertor extends Convertor<SSCParser.LambdaFunctionContext> {
    // must be sequenced so that nested lambdas get defined in the right order
    private final SequencedSet<LambdaFunction> lambdasCollectedInCurrentFunction;

    public LambdaConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);

        lambdasCollectedInCurrentFunction = new TreeSet<>();
    }

    @Override
    public String convert(SSCParser.LambdaFunctionContext ctx) {
        final LambdaFunction lambda = new LambdaFunction(
                dispatcher.currentFile,
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

    public String emit() {
        final String lambdas = lambdasCollectedInCurrentFunction.stream()
                .map(LambdaFunction::getDefinition)
                .collect(Collectors.joining(lineSeparator()));
        lambdasCollectedInCurrentFunction.clear();
        return lambdas;
    }
}

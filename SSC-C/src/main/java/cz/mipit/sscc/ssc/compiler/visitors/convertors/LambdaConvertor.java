package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

public class LambdaConvertor
        extends AbstractConvertor<SSCParser.LambdaFunctionContext> {

    public LambdaConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.LambdaFunctionContext ctx) {
        final String surroundingFunctionName = dispatcher.getCurrentFunctionName();
        final String lambdaName = LambdaFunction.createName(surroundingFunctionName);

        dispatcher.pushFunction(lambdaName);

        final String returnType = dispatcher.visitTypeName(ctx.typeName());
        final String parameters = dispatcher.visitParameterTypeList(ctx.parameterTypeList());
        final String body = dispatcher.visitFunctionBody(ctx.functionBody());
        final String lambdaAttributes = ctx.lambdaAttributes() != null
                ? dispatcher.visitLambdaAttributes(ctx.lambdaAttributes())
                : "";

        final LambdaFunction lambda = new LambdaFunction(
                lambdaName,
                returnType,
                parameters,
                body,
                lambdaAttributes
        );

        dispatcher.popFunction();
        dispatcher.addExternalDeclarationToEmitBefore(lambda.getDefinition());

        return lambda.getName();
    }
}

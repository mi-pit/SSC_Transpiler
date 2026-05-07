package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

public class LambdaConvertor
        extends AbstractConvertor<SSCParser.LambdaFunctionContext>
        implements Convertor<SSCParser.LambdaFunctionContext> {

    public LambdaConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
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
        dispatcher.addMethodToEmit(lambda.getDefinition());

        return lambda.getName();
    }
}

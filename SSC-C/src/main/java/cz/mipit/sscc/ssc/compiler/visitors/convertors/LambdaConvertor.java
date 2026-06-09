package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.Collections;

import static cz.mipit.sscc.util.SSCCUtil.createTypedef;

public class LambdaConvertor extends AbstractConvertor<SSCParser.LambdaFunctionContext> {
    public LambdaConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.LambdaFunctionContext.class);
    }

    @Override
    public String convert(SSCParser.LambdaFunctionContext ctx) {
        final String surroundingFunctionName = dispatcher.getCurrentFunctionName();
        final String lambdaName = SSCCUtil.createNameWithID("__ssc_lambda", surroundingFunctionName);

        dispatcher.pushFunction(lambdaName, null);

        final SSCParser.TypeNameContext typeName = ctx.typeName();

        final String typedefIdentifier = createTypedef(
                dispatcher,
                "__ssc_lambda_type",
                surroundingFunctionName,
                typeName
        );

        final String parameters = dispatcher.visit(ctx.parameterTypeList());
        final String body = dispatcher.visit(ctx.functionBody());
        final String lambdaAttributes = ctx.lambdaAttributes() != null
                ? dispatcher.visit(ctx.lambdaAttributes())
                : "";

        final LambdaFunction lambda = new LambdaFunction(
                lambdaName,
                typedefIdentifier,
                parameters,
                body,
                lambdaAttributes,
                dispatcher,
                Collections.emptyList()
        );

        dispatcher.popFunction();
        dispatcher.addExternalDeclarationToEmitBefore(lambda.getDefinition());

        return lambda.getName();
    }
}

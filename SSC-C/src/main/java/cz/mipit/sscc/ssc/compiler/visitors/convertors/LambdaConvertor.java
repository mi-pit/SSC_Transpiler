package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.concurrent.atomic.AtomicLong;

import static cz.mipit.sscc.util.SSCCUtil.insertIdentifierIntoDeclarator;

public class LambdaConvertor extends AbstractConvertor<SSCParser.LambdaFunctionContext> {
    public static final AtomicLong ids = new AtomicLong();

    public LambdaConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.LambdaFunctionContext.class);
    }

    @Override
    public String convert(SSCParser.LambdaFunctionContext ctx) {
        final String surroundingFunctionName = dispatcher.getCurrentFunctionName();
        final String lambdaName = LambdaFunction.createName(surroundingFunctionName);

        dispatcher.pushFunction(lambdaName, null);

        final SSCParser.TypeNameContext typeName = ctx.typeName();

        final String typedefIdentifier = SSCCUtil.createNameWithID("__ssc_lambda_typedef", ids, surroundingFunctionName);
        final String typedefDeclarator = insertIdentifierIntoDeclarator(dispatcher, typeName.abstractDeclarator(), typedefIdentifier);
        final String typedefSpecifiersQualifiers = dispatcher.visit(typeName.specifierQualifierList());
        final String typedef = "typedef " + typedefSpecifiersQualifiers + " " + typedefDeclarator + ";";
        dispatcher.addExternalDeclarationToEmitBefore(typedef);

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
                lambdaAttributes
        );

        dispatcher.popFunction();
        dispatcher.addExternalDeclarationToEmitBefore(lambda.getDefinition());

        return lambda.getName();
    }
}

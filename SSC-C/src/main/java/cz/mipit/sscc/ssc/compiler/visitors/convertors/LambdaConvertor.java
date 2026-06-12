package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.data.var.LiteralVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

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

        final StringJoiner bodybuilder = new StringJoiner("\n    ", "\n{\n    ", "\n}\n");

        //final List<LiteralVariable> captures = dispatcher.state.nonGlobalVariables();
        final List<TerminalNode> captureIdentifiers = Optional.ofNullable(ctx.identifierList())
                .map(SSCParser.IdentifierListContext::Identifier)
                .map(ls ->
                        ls
                                .stream()
                                .toList()
                )
                .orElse(Collections.emptyList());

        final List<LiteralVariable> captures = new ArrayList<>();
        for (final TerminalNode captureIdentifier : captureIdentifiers) {
            final String identifier = dispatcher.visit(captureIdentifier);
            final LiteralVariable var = dispatcher.state.getLiteralVariable(identifier);
            if (var == null) {
                throw dispatcher.getSSCLanguageException(
                        "No variable named '" + captureIdentifier + "' is in scope",
                        captureIdentifier
                );
            }
            captures.add(var);
        }

        final List<String> assignmentsBeforeLambda = new ArrayList<>();
        saveCaptures(
                captures,
                surroundingFunctionName,
                assignmentsBeforeLambda,
                bodybuilder
        );

        final SSCParser.TypeNameContext typeName = ctx.typeName();

        final String typedefIdentifier = createTypedef(
                dispatcher,
                "__ssc_lambda_type",
                surroundingFunctionName,
                typeName
        );

        final String parameters = dispatcher.visit(ctx.parameterTypeList());
        final String origBody = dispatcher.visit(ctx.functionBody());
        final String lambdaAttributes = ctx.lambdaAttributes() != null
                ? dispatcher.visit(ctx.lambdaAttributes())
                : "";

        bodybuilder.add(
                origBody /* added as a compound statement within the function body */
        );

        final LambdaFunction lambda = new LambdaFunction(
                lambdaName,
                typedefIdentifier,
                parameters,
                bodybuilder.toString(),
                lambdaAttributes,
                Collections.emptyList()
        );

        dispatcher.popFunction();
        dispatcher.addExternalDeclarationToEmitBefore(lambda.getDefinition());

        for (final String assignment : assignmentsBeforeLambda) {
            dispatcher.addBlockItemToEmitBefore(assignment);
        }

        return lambda.getName();
    }

    private void saveCaptures(List<LiteralVariable> captures, String surroundingFunctionName, List<String> assignmentsBeforeLambda, StringJoiner bodybuilder) {
        for (final LiteralVariable variable : captures) {
            final String staticCaptureIdent = SSCCUtil.createNameWithID("__ssc_lmbd_cap", surroundingFunctionName)
                                              + "_" + variable.getIdentifier();

            final String staticVariable = variable.getDeclarationForLambda(
                    true,
                    staticCaptureIdent
            );
            final String localVariable = variable.getDeclarationForLambda(
                    false,
                    variable.getIdentifier()
            );

            assignmentsBeforeLambda.add(staticCaptureIdent + " = " + variable.getIdentifier() + ";");

            final String staticDecl = "static " + staticVariable + " = 0;";
            dispatcher.addExternalDeclarationToEmitBefore(staticDecl);

            final String localDecl = localVariable + " = " + staticCaptureIdent + ";";
            bodybuilder.add(localDecl);
        }
    }
}

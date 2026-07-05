package cz.mipit.sscc.ssc.compiler.data.lambda;

import cz.mipit.sscc.ssc.compiler.data.var.LambdaVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Function;

import static cz.mipit.sscc.util.Util.requireNonBlank;
import static java.util.Objects.requireNonNull;

public final class LambdaFunction {
    private final String returnType;
    private final String params;
    private final String body;
    private final String attributes;

    private final String name;


    private LambdaFunction(
            String prettifiedName,
            String returnType,
            String params,
            String ctx,
            String attributes
    ) {
        this.returnType = returnType;
        this.params = params;
        this.body = ctx;
        this.attributes = attributes;
        this.name = prettifiedName;
    }

    public static LambdaFunction withoutCaptures(
            final String prettifiedName,
            String returnType,
            String params,
            String body,
            String attributes
    ) {
        return new LambdaFunction(prettifiedName, returnType, params, body, attributes);
    }

    public static LambdaFunction withCaptures(
            final VisitorDispatcher dispatcher,
            final String prettifiedName,
            final String surroundingFunctionName,
            final String returnType,
            final String attributes,
            final String origBody,
            final String params,
            final List<LambdaVariable> captures,
            final boolean capturesMayBeUnused
    ) {
        final StringJoiner bodybuilder = new StringJoiner("\n    ", "\n{\n    ", "\n}\n");

        final String _returnType = " " + requireNonBlank(requireNonNull(returnType)) + " ";
        final String _params = requireNonNull(params);
        final String _attributes = padIfNotBlank(requireNonNull(attributes), s -> s + " ");

        final String _name = requireNonNull(prettifiedName);

        final List<String> assignmentsBeforeLambda = new ArrayList<>();
        saveCaptures(
                dispatcher,
                captures,
                surroundingFunctionName,
                assignmentsBeforeLambda,
                bodybuilder,
                capturesMayBeUnused
        );

        bodybuilder.add(
                origBody /* added as a compound statement within the function body */
        );
        final String _body = requireNonNull(bodybuilder.toString());

        final LambdaFunction lambdaFunction = new LambdaFunction(
                _name, _returnType, _params, _body, _attributes
        );

        for (final String assignment : assignmentsBeforeLambda) {
            dispatcher.addBlockItemToEmitBefore(assignment);
        }

        return lambdaFunction;
    }

    private static void saveCaptures(
            final VisitorDispatcher dispatcher,
            List<LambdaVariable> captures,
            String surroundingFunctionName,
            List<String> assignmentsBeforeLambda,
            StringJoiner bodybuilder,
            final boolean capturesMayBeUnused
    ) {
        for (final LambdaVariable variable : captures) {
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

            final String un = capturesMayBeUnused ? "__attribute__((unused)) " : "";
            final String localDecl = un + localVariable + " = " + staticCaptureIdent + ";";
            bodybuilder.add(localDecl);
        }
    }

    public String getName() {
        return name;
    }

    public String getHeader() {
        return attributes + "static" + returnType + getName() + "(" + params + ")";
    }

    public String getDefinition() {
        return getHeader() + body;
    }


    private static String padIfNotBlank(
            final String string,
            final Function<String, String> mapper
    ) {
        if (string.isBlank()) {
            return string;
        }

        return mapper.apply(string);
    }
}

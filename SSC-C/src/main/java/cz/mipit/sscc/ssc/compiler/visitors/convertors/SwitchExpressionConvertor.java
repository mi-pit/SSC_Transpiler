package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.Util;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNullElse;

public class SwitchExpressionConvertor extends AbstractConvertor<SSCParser.SwitchExpressionContext> {
    private static final AtomicLong FUNCTION_IDS = new AtomicLong();
    private static final AtomicLong VARIABLE_IDS = new AtomicLong();

    public SwitchExpressionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SwitchExpressionContext.class);
    }

    //     : 'swex' '(' expression ')' '->' typeName '{'
    //          ('case'    (constant | StringLiteral) '=>' statement)+
    //          ('default'                            '=>' statement)?
    //      '}'
    @Override
    public String convert(SSCParser.SwitchExpressionContext ctx) {
        final String surroundingFunctionName = requireNonNullElse(dispatcher.getCurrentFunctionName(), "Global");
        final String functionName = generateFunctionName(surroundingFunctionName);

        dispatcher.pushFunction(functionName, null);

        final String expression = dispatcher.visit(ctx.expression());

        final String variableHolderIdentifier = generateVariableName(surroundingFunctionName);

        final String auxVariableDeclarationAndAssignment =
                "const typeof_unqual( %s ) %s = %s;".formatted(expression, variableHolderIdentifier, expression);
        dispatcher.addBlockListItemToEmitBefore(auxVariableDeclarationAndAssignment);

        final String swexFunctionCall =
                "%s( &%s, sizeof %s )".formatted(functionName, variableHolderIdentifier, variableHolderIdentifier);

        final String returnType = Util.requireNonBlank(dispatcher.visit(ctx.typeName()));

        if (!dispatcher.hasType("size_t")) {
            dispatcher.addExternalDeclarationToEmitBefore(
                    "\n#include <stddef.h> /* generated in SwitchExpressionConvertor */\n"
            );
        }
        final String function = "%s %s( const void *__restrict data, size_t nbytes )%n".formatted(returnType, functionName);

        dispatcher.addExternalDeclarationToEmitBefore(
                function + ";\n/* TODO: define function body */\n"
        );

        return swexFunctionCall;
    }

    private static String generateFunctionName(
            final String surroundingFunctionName
    ) {
        return SSCCUtil.createNameWithID("__ssc_swex_fn", FUNCTION_IDS, surroundingFunctionName);
    }

    private static String generateVariableName(
            final String surroundingFunctionName
    ) {
        return SSCCUtil.createNameWithID("__ssc_swex_var", VARIABLE_IDS, surroundingFunctionName);
    }
}

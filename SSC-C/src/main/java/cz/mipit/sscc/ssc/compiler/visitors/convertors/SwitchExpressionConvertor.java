package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.data.var.LiteralVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.collection.Box;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction.padIfNotBlank;

public class SwitchExpressionConvertor extends AbstractConvertor<SSCParser.SwitchExpressionContext> {
    private static final AtomicLong FUNCTION_IDS = new AtomicLong();
    private static final AtomicLong VARIABLE_IDS = new AtomicLong();

    public SwitchExpressionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SwitchExpressionContext.class);
    }

    //  'swex' '(' expression ')' '->' typeName '{'
    //      branch+
    //  '}'
    @Override
    public String convert(SSCParser.SwitchExpressionContext ctx) {
        if (dispatcher.getCurrentFunctionName() == null) {
            throw dispatcher.getSSCLanguageException(
                    "Switch expressions may not be used outside of functions", ctx
            );
        }

        final SSCParser.TypeNameContext typeName = ctx.typeName();

        final String surroundingFunctionName = dispatcher.getCurrentFunctionName();
        final String swexFunctionName = SSCCUtil.createNameWithID("__ssc_swex_fn", surroundingFunctionName);

        final String typedefIdentifier = SSCCUtil.createTypedef(
                dispatcher,
                "__ssc_swex_typedef",
                surroundingFunctionName,
                typeName
        );

        final List<LiteralVariable> captures = dispatcher.state.nonGlobalVariables(); // TODO: only pass the used ones
        final String capturesAsParams = captures.stream()
                .map(LiteralVariable::getIdentifier)
                .collect(Collectors.joining(", "));

        final Pair<String, String> paramAndBody = getBody(
                ctx, surroundingFunctionName, typedefIdentifier, captures, capturesAsParams
        );

        final LambdaFunction lambda = new LambdaFunction(
                swexFunctionName,
                typedefIdentifier,
                paramAndBody.a,
                paramAndBody.b,
                "",
                dispatcher,
                captures
        );

        dispatcher.addExternalDeclarationToEmitBefore(lambda.getDefinition());

        final String expression = dispatcher.visit(ctx.expression());

        return String.format("%s( %s, %s )", swexFunctionName, expression, capturesAsParams);
    }

    // switchExpressionBranch
    //    : ('case' (constant | StringLiteral) '=>' switchExpressionResult)
    //    | ('default'                         '=>' switchExpressionResult)
    private Pair<String, String> getBody(
            final SSCParser.SwitchExpressionContext ctx,
            final String surroundingFunctionName,
            final String returnType,
            final List<LiteralVariable> captures,
            final String capturesAsParams
    ) {
        final StringJoiner body = new StringJoiner("\n", "{\n    switch ( hash ) {\n", "\n    }\n}");

        final Box<SSCParser.SwitchExpressionBranchContext> defaultBranchCtx = new Box<>();
        final Box<ExpressionType> switchedType = new Box<>();

        for (final SSCParser.SwitchExpressionBranchContext branchCtx : ctx.switchExpressionBranch()) {
            if (branchCtx.Default() != null) {
                if (defaultBranchCtx.item != null) {
                    throw dispatcher.getSSCCallbackException(
                            "Duplicate default branch in switch expression",
                            branchCtx,
                            defaultBranchCtx.item
                    );
                }

                body.add(String.format("""
                                        default:
                                            return %s
                                """,
                        convertExpression(
                                branchCtx.switchExpressionResult(),
                                surroundingFunctionName,
                                returnType,
                                captures,
                                capturesAsParams
                        )));
                defaultBranchCtx.item = branchCtx;
                continue;
            }

            final ExpressionType typeOfExpressionInCurrentCase;
            if (branchCtx.constant() != null) {
                typeOfExpressionInCurrentCase = fromConstant(branchCtx.constant());
            } else {
                typeOfExpressionInCurrentCase = fromConstant(branchCtx.StringLiteral());
            }
            if (switchedType.item == null) {
                switchedType.item = typeOfExpressionInCurrentCase;
            } else if (switchedType.item != typeOfExpressionInCurrentCase) {
                throw dispatcher.getSSCLanguageException(
                        "Invalid value in switch expression case", // provide better message
                        branchCtx.Case()
                );
            }

            final String case_ = dispatcher.visit(branchCtx.Case());
            final String constant = dispatcher.visit(branchCtx.constant());
            final String result = convertExpression(
                    branchCtx.switchExpressionResult(),
                    surroundingFunctionName,
                    returnType,
                    captures,
                    capturesAsParams
            );

            final String branch = String.format("""
                                    %s %s:
                                        return %s
                            """,
                    case_,
                    constant,
                    result
            );
            body.add(branch);
        }
        assert switchedType.item != null;
        final String cType = switchedType.item.toCType();
        final String param = cType + " hash";

        return new Pair<>(param, body.toString());
    }

    //    : expression ';'
    //    | compoundStatement
    private String convertExpression(
            SSCParser.SwitchExpressionResultContext ctx,
            String surroundingFunctionName,
            String returnType,
            List<LiteralVariable> captures,
            String capturesAsParams
    ) {
        if (ctx.expression() != null) {
            return dispatcher.visit(ctx.expression()) + dispatcher.visit(ctx.Semi());
        }

        final String params = ""; // TODO

        final LambdaFunction subLambda = new LambdaFunction(
                SSCCUtil.createNameWithID("__ssc_swex_branch", surroundingFunctionName),
                returnType,
                params,
                dispatcher.visit(ctx.compoundStatement()),
                "",
                dispatcher,
                captures
        );

        dispatcher.addExternalDeclarationToEmitBefore(subLambda.getDefinition());

        return subLambda.getName() + "(" + padIfNotBlank(params, s -> " " + s + ", ") + capturesAsParams + ");";
    }


    // constant
    //    : IntegerConstant
    //    | FloatingConstant
    //    //|   EnumerationConstant
    //    | CharacterConstant
    //    | predefinedConstant
    enum ExpressionType {
        INTEGRAL,
        FLOATING,
        CHARACTER,
        BOOL,
        STRING,
        ;

        public String toCType() {
            return switch (this) {
                case BOOL -> "bool";
                case FLOATING -> "double";
                case CHARACTER -> "char";
                case INTEGRAL -> "int";
                case STRING -> "const char *";
            };
        }
    }

    private ExpressionType fromConstant(TerminalNode node) {
        if (node.getSymbol().getType() == SSCParser.StringLiteral) {
            return ExpressionType.STRING;
        }
        throw new IllegalArgumentException("Unknown constant type: " + node.getSymbol());
    }

    private ExpressionType fromConstant(SSCParser.ConstantContext ctx) {
        if (ctx.IntegerConstant() != null) {
            return ExpressionType.INTEGRAL;
        }
        if (ctx.FloatingConstant() != null) {
            return ExpressionType.FLOATING;
        }
        if (ctx.CharacterConstant() != null) {
            return ExpressionType.CHARACTER;
        }
        final SSCParser.PredefinedConstantContext predefCtx = ctx.predefinedConstant();
        assert predefCtx != null;
        if (predefCtx.Nulptr() != null) {
            throw dispatcher.getSSCLanguageException(
                    "nullptr is not a valid case expression", predefCtx.Nulptr()
            );
        }
        if (predefCtx.False_() != null || predefCtx.True_() != null) {
            return ExpressionType.BOOL;
        }

        throw new IllegalStateException("Unknown constant type: " + dispatcher.visit(ctx));
    }
}

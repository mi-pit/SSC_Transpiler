package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.lambda.LambdaFunction;
import cz.mipit.sscc.ssc.compiler.data.var.LiteralVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.collection.Box;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class SwitchExpressionConvertor extends AbstractConvertor<SSCParser.SwitchExpressionContext> {
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

        {
            dispatcher.addExternalDeclarationToEmitBefore("\n#include <ssclib/.internal/hash.h>\n");
        }

        final SSCParser.TypeNameContext typeName = ctx.typeName();

        final String surroundingFunctionName = dispatcher.getCurrentFunctionName();
        final String swexFunctionName = SSCCUtil.createNameWithID("__ssc_swex_fn", surroundingFunctionName);

        final String typedefIdentifier = SSCCUtil.createTypedef(
                dispatcher,
                "__ssc_swex_type",
                surroundingFunctionName,
                typeName
        );

        final List<LiteralVariable> captures = dispatcher.state.nonGlobalVariables(); // TODO: only pass the used ones
        final String capturesAsParams = captures.stream()
                .map(LiteralVariable::getIdentifier)
                .collect(Collectors.joining(", "));

        final ExpressionType type;
        final String body;
        {
            final Pair<ExpressionType, String> typeAndBody = getParameterAndBody(
                    ctx, surroundingFunctionName, typedefIdentifier, captures, capturesAsParams
            );
            type = typeAndBody.a;
            body = typeAndBody.b;
        }

        final String paramType = type.getParameterType();
        final String param = paramType + " hash";

        final LambdaFunction lambda = new LambdaFunction(
                swexFunctionName,
                typedefIdentifier,
                param,
                body,
                "",
                dispatcher,
                captures
        );
        assert lambda.getName().equals(swexFunctionName);

        dispatcher.addExternalDeclarationToEmitBefore(lambda.getDefinition());

        // 'swex' '(' expression ')' '->' typeName
        // assignmentExpression (',' assignmentExpression)*

        final String expression = dispatcher.visit(ctx.expression());
        final String hashedExpression = switch (type) {
            case INTEGRAL, UNSIGNED, BOOL, CHARACTER -> "( " + paramType + " ) ( " + expression + " )";

            case STRING -> "__ssc_hash_string( " + expression + " )";
        };

        final String sep = capturesAsParams.isBlank() ? "" : ", ";
        return String.format("%s( %s%s %s )", lambda.getName(), hashedExpression, sep, capturesAsParams);
    }

    // switchExpressionBranch
    //    : ('case' (constant | StringLiteral) '=>' switchExpressionResult)
    //    | ('default'                         '=>' switchExpressionResult)
    private Pair<ExpressionType, String> getParameterAndBody(
            final SSCParser.SwitchExpressionContext ctx,
            final String surroundingFunctionName,
            final String returnType,
            final List<LiteralVariable> captures,
            final String capturesAsParams
    ) {
        final StringJoiner body = new StringJoiner("\n", "{\n    switch ( hash ) {\n", "\n    }\n}");

        final Box<SSCParser.SwitchExpressionBranchContext> defaultBranchCtx = new Box<>();
        final Box<ExpressionType> switchedType = new Box<>();

        final boolean[] alreadyParsedBools = new boolean[2];
        final Set<String> alreadyParsedStrings = new HashSet<>();
        final Set<Long> hashes = new HashSet<>();

        for (final SSCParser.SwitchExpressionBranchContext branchCtx : ctx.switchExpressionBranch()) {
            if (branchCtx.Case() != null) {
                processCaseBranch(
                        surroundingFunctionName, returnType, captures, capturesAsParams, branchCtx,
                        switchedType, alreadyParsedBools, alreadyParsedStrings, hashes, body
                );
                continue;
            }

            if (alreadyParsedBools[0] && alreadyParsedBools[1]) {
                throw dispatcher.getSSCLanguageException(
                        "Default branch used in a boolean switch expression after defining both true and false branch",
                        branchCtx
                );
            }
            processDefaultBranch(
                    surroundingFunctionName, returnType, captures, capturesAsParams,
                    branchCtx, defaultBranchCtx, body
            );
        }

        if (switchedType.item == null) {
            throw dispatcher.getSSCLanguageException(
                    "Switch expression must have at least one non-default branch", ctx
            );
        }

        final String un = "__builtin_unreachable";
        final String unreachable = dispatcher.hasSymbol(un) ? un : "";
        body.add("""
                    }
                
                    {
                        %s
                        return 0;
                """.formatted(unreachable)
        );

        return new Pair<>(switchedType.item, body.toString());
    }

    private void processCaseBranch(
            String surroundingFunctionName,
            String returnType,
            List<LiteralVariable> captures,
            String capturesAsParams,
            SSCParser.SwitchExpressionBranchContext branchCtx,
            Box<ExpressionType> switchedType,
            boolean[] alreadyParsedBools,
            Set<String> alreadyParsedStrings,
            Set<Long> hashes,
            StringJoiner body
    ) {
        final ExpressionType typeOfExpressionInCurrentCase;
        if (branchCtx.constant() != null) {
            typeOfExpressionInCurrentCase = ExpressionType.fromConstant(dispatcher, branchCtx.constant());
        } else {
            typeOfExpressionInCurrentCase = ExpressionType.fromConstant(branchCtx.StringLiteral());
        }
        if (switchedType.item == null) {
            switchedType.item = typeOfExpressionInCurrentCase;
        } else if (switchedType.item != typeOfExpressionInCurrentCase) {
            throw dispatcher.getSSCLanguageException(
                    "Invalid value in switch expression case", // todo: provide better message
                    branchCtx.Case()
            );
        }

        final String case_ = dispatcher.visit(branchCtx.Case());
        final String constant = convertConstant(branchCtx, switchedType, alreadyParsedStrings, hashes, alreadyParsedBools);
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

    private void processDefaultBranch(String surroundingFunctionName,
                                      String returnType,
                                      List<LiteralVariable> captures,
                                      String capturesAsParams,
                                      SSCParser.SwitchExpressionBranchContext branchCtx,
                                      Box<SSCParser.SwitchExpressionBranchContext> defaultBranchCtx,
                                      StringJoiner body) {
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
    }

    private String convertConstant(
            final SSCParser.SwitchExpressionBranchContext ctx,
            final Box<ExpressionType> switchedType,
            final Set<String> alreadyParsedStrings,
            final Set<Long> hashes,
            final boolean[] boolBranches
    ) {
        return switch (switchedType.item) {
            case INTEGRAL, UNSIGNED, CHARACTER -> dispatcher.visit(ctx.constant());

            case BOOL -> {
                final boolean parsed = ctx.constant().predefinedConstant().True_() != null;
                final int idx = parsed ? 1 : 0;
                if (boolBranches[idx]) {
                    throw dispatcher.getSSCLanguageException(
                            "Duplicate boolean branch", ctx
                    );
                }
                boolBranches[idx] = true;
                yield parsed ? "1" : "0";
            }

            case STRING -> {
                final String literal;
                {
                    final String tmp = dispatcher.visit(ctx.StringLiteral());
                    assert tmp.charAt(0) == '"' && tmp.charAt(tmp.length() - 1) == '"';
                    literal = tmp.substring(1, tmp.length() - 1);
                }
                if (alreadyParsedStrings.contains(literal)) {
                    throw dispatcher.getSSCLanguageException(
                            "Duplicate string branch", ctx
                    );
                }

                final long hash = hashString(literal);
                yield String.valueOf(hash);
            }
        };
    }

    /// Same function is in ssclib/.internal
    private long hashString(String s) {
        final byte[] bytes = s.getBytes(StandardCharsets.US_ASCII);

        long hash = 23L * bytes.length;
        for (final byte b : bytes) {
            hash = (hash << 4) + b;
        }

        return hash;
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

        return subLambda.getName() + "( " + params + capturesAsParams + " );";
    }


    // constant
    //    : IntegerConstant
    //    | FloatingConstant
    //    //|   EnumerationConstant
    //    | CharacterConstant
    //    | predefinedConstant
    private enum ExpressionType {
        INTEGRAL,
        UNSIGNED,
        CHARACTER,
        BOOL,
        STRING,
        ;

        String getParameterType() {
            return switch (this) {
                case BOOL, CHARACTER -> "int"; // characters are often ints anyway

                case INTEGRAL, STRING -> "long long"; // assume `long long` is exactly 64 bits

                case UNSIGNED -> "unsigned long long";
            };
        }

        static ExpressionType fromConstant(TerminalNode node) {
            if (node.getSymbol().getType() == SSCParser.StringLiteral) {
                return ExpressionType.STRING;
            }
            throw new IllegalArgumentException("Unknown constant type: " + node.getSymbol());
        }

        static ExpressionType fromConstant(VisitorDispatcher dispatcher, SSCParser.ConstantContext ctx) {
            if (ctx.IntegerConstant() != null) {
                if (dispatcher.visit(ctx.IntegerConstant()).toLowerCase().contains("u")) {
                    return ExpressionType.UNSIGNED;
                }
                return ExpressionType.INTEGRAL;
            }
            if (ctx.FloatingConstant() != null) {
                throw dispatcher.getSSCLanguageException(
                        "Floating constants are now allowed in switch expressions", ctx.FloatingConstant()
                );
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

            throw new IllegalStateException("Unknown constant type: " + dispatcher.getLiteral(ctx));
        }
    }
}

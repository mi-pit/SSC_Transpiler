package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.ss.SSMember;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.util.Either;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.System.lineSeparator;

public class PostfixExpressionConvertor extends Convertor<SSCParser.PostfixExpressionContext> {
    public PostfixExpressionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.PostfixExpressionContext ctx) {
        /* Compound literals for some reason count as postfix expressions */
        final Optional<String> res = getCompoundLiteralReplaced(ctx);
        if (res.isPresent()) {
            return res.get();
        }

        if (!ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty()) {
            return convertMethodCall(ctx);
        }
        if (!ctx.DoubleColon().isEmpty()) {
            return convertStaticFunctionCall(ctx);
        }
        return dispatcher.super_visitPostfixExpression(ctx);
    }

    private Optional<String> getCompoundLiteralReplaced(SSCParser.PostfixExpressionContext ctx) {
        /* postfixExpression.typeName implies compound literal */
        if (ctx.typeName() == null) {
            return Optional.empty();
        }

        final String res = dispatcher.super_visitPostfixExpression(ctx);

        Main.logger.printDebug(() -> "superStructSpecifier in: "
                + SSCCUtil.Text.getLiteral(ctx, dispatcher.tokens).replace(lineSeparator(), " ")
                + lineSeparator() + "\t\tReturning: " + res.replace(lineSeparator(), " "));

        return Optional.of(res);
    }

    public String convertMethodCall(final SSCParser.PostfixExpressionContext ctx) {
        enum ArrowOrDot {Arrow, Dot, Neither}
        final ArrowOrDot arrowOrDot =
                !ctx.Arrow().isEmpty() ? ArrowOrDot.Arrow
                        : !ctx.Dot().isEmpty() ? ArrowOrDot.Dot
                        : ArrowOrDot.Neither;

        Main.logger.printDebug(() -> arrowOrDot + " in: " + SSCCUtil.Text.getLiteral(ctx, dispatcher.tokens));
        assert arrowOrDot != ArrowOrDot.Neither;

        final String objectName = dispatcher.visitPrimaryExpression(ctx.primaryExpression());
        if (ctx.Identifier().isEmpty())
            throw getSSCSyntaxException(arrowOrDot + " expression has no right side expression", ctx);

        final String currentFunctionName = dispatcher.getCurrentFunctionName();
        final Optional<SuperstructVariable> maybeVar = dispatcher.findSuperstructVariable(currentFunctionName, objectName);
        if (maybeVar.isEmpty()) {
            Main.logger.printDebug(() -> "\tVariable is not superstruct\t\tlocal vars: "
                    + dispatcher.data.functionVariables().get(currentFunctionName));
            return dispatcher.super_visitPostfixExpression(ctx);
        }
        final SuperstructVariable var = maybeVar.get();

        final SuperStruct superstruct = dispatcher.getSuperStructFromVariable(ctx, var);

        final boolean hasLeftParen = !ctx.LeftParen().isEmpty();
        assert hasLeftParen == !ctx.RightParen().isEmpty();
        if (!hasLeftParen) {
            Main.logger.printDebug(() -> "\tNo parentheses");
            return getFieldAccessString(ctx, superstruct);
        }

        final String methodName = dispatcher.visitTerminal(ctx.Identifier(0));
        final Optional<FunctionDefinition> maybeMethod = dispatcher.findMethodInSuperstruct(superstruct, methodName);

        if (maybeMethod.isEmpty()) {
            Main.logger.printDebug(() -> "Variable does not have such a method");
            if (superstruct
                    .members()
                    .stream()
                    .map(SSMember::data)
                    .map(Either::getLeft)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .anyMatch(decl -> decl.getName().equals(methodName))
            ) {
                return dispatcher.super_visitPostfixExpression(ctx);
            }

            Main.logger.printDebug(() ->
                    "Did not find method `" + methodName + "`. " +
                            "Converting anyway and hoping it gets defined later"
            );
        }

        if (arrowOrDot == ArrowOrDot.Dot && var.pointer() != 0) {
            throw getSSCSyntaxException("Cannot access non-local superstruct variable using `.`", ctx);
        }
        if (arrowOrDot == ArrowOrDot.Arrow && var.pointer() != 1) {
            throw getSSCSyntaxException("Variable '" + var.getName() + "' is not a pointer to struct", ctx);
        }

        maybeMethod.ifPresent(functionDefinition -> {
            if (functionDefinition.isPrivate()) {
                Main.logger.printDebug(() -> "Method '" + methodName + "' is private. Going to check if it may be used here...");
                if (dispatcher.data.currentSS().isEmpty()
                        || !dispatcher.data.currentSS().get().name().equals(superstruct.name())) {
                    throw getSSCSyntaxException(
                            "Cannot access private method `" + methodName + "` from outside the superstruct", ctx);
                }
            }
        });

        final String ssName = superstruct.name();

        final StringBuilder expressionBuilder =
                new StringBuilder(ssName)
                        .append("__")
                        .append(methodName)
                        .append("(");

        if (arrowOrDot == ArrowOrDot.Dot) {
            expressionBuilder.append("&");
        }
        expressionBuilder.append(objectName);

        if (!ctx.argumentExpressionList().isEmpty()) {
            expressionBuilder.append(", ");
        }

        final List<String> args = new ArrayList<>();
        for (SSCParser.ArgumentExpressionListContext argListCtx : ctx.argumentExpressionList()) {
            for (SSCParser.AssignmentExpressionContext assExprCtx : argListCtx.assignmentExpression()) {
                args.add(dispatcher.visitAssignmentExpression(assExprCtx));
            }
        }

        expressionBuilder
                .append(String.join(", ", args))
                .append(")");

        final String finalExpression = expressionBuilder.toString();
        Main.logger.printDebug(() -> "\tFinal Expression: " + finalExpression);
        return finalExpression;
    }

    public String convertStaticFunctionCall(final SSCParser.PostfixExpressionContext ctx) {
        Main.logger.printDebug(() -> "Double colon in: " + SSCCUtil.Text.getLiteral(ctx, dispatcher.tokens));

        if (ctx.primaryExpression() == null) {
            throw getSSCSyntaxException("Double colon expression has no left side (Superstruct name) expression", ctx);
        }
        final String className = dispatcher.visitPrimaryExpression(ctx.primaryExpression());

        if (ctx.Identifier().isEmpty()) {
            throw getSSCSyntaxException("Double colon expression has no right side (function) expression", ctx);
        }
        final String methodName = ctx.Identifier().getFirst().toString();

        verifyStaticCall(ctx, className, methodName);

        final String namespacedMethodName = className + "__" + methodName;

        final boolean noCall = ctx.LeftParen().isEmpty();
        if (noCall) {
            return namespacedMethodName;
        }

        final List<String> args = new ArrayList<>();
        for (SSCParser.ArgumentExpressionListContext argListCtx : ctx.argumentExpressionList()) {
            for (SSCParser.AssignmentExpressionContext assExprCtx : argListCtx.assignmentExpression()) {
                args.add(dispatcher.visitAssignmentExpression(assExprCtx));
            }
        }

        final String result = namespacedMethodName + "( " + String.join(", ", args) + " )";
        Main.logger.printDebug(() -> "\tResult: " + result);
        return result;
    }

    private void verifyStaticCall(final SSCParser.PostfixExpressionContext ctx,
                                  final String className,
                                  final String methodName) {
        final Optional<SuperStruct> maybeSS = dispatcher.findSuperstructByName(className);
        if (maybeSS.isEmpty()) {
            throw getSSCSyntaxException("Could not find superstruct with name `" + className + "`", ctx);
        }
        final SuperStruct superstruct = maybeSS.get();

        final Optional<FunctionDefinition> maybeMethod = dispatcher.findMethodInSuperstruct(superstruct, methodName);
        if (maybeMethod.isEmpty()) {
            throw getSSCSyntaxException(
                    "Superstruct with name `" + className
                            + "` has no method called `" + methodName
                            + "`", ctx);
        }
        final FunctionDefinition method = maybeMethod.get();

        if (method.isPrivate()) {
            Main.logger.printDebug(() -> "Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (dispatcher.data.currentSS().isEmpty()
                    || !dispatcher.data.currentSS().get().name().equals(className)) {
                throw getSSCSyntaxException(
                        "Cannot access private static method `" + methodName + "` from outside the superstruct", ctx
                );
            }
        }
    }

    private String getFieldAccessString(SSCParser.PostfixExpressionContext ctx,
                                        SuperStruct superstruct) {
        final String fieldName = dispatcher.visitTerminal(ctx.Identifier(0));

        final List<Field> allMatching = superstruct.members()
                .stream()
                .map(SSMember::data)
                .filter(either -> either.getLeft().isPresent())
                .map(either -> either.getLeft().get())
                .filter(field -> field.getName().equals(fieldName))
                .toList();
        if (allMatching.size() > 1) {
            throw getSSCSyntaxException(
                    "Found more than one matching field in superstruct `" + superstruct.name() + "`", ctx
            );
        }
        if (allMatching.isEmpty()) {
            throw getSSCSyntaxException(
                    "Found no matching field in superstruct `" + superstruct.name() + "`", ctx
            );
        }

        final Field field = allMatching.getFirst();

        final String primaryExpression = dispatcher.visitPrimaryExpression(ctx.primaryExpression());
        if (field.isPrivate()) {
            final boolean inSSMethod = dispatcher
                    .findSuperstructVariable(dispatcher.getCurrentFunctionName(), primaryExpression)
                    .isPresent();
            Main.logger.printDebug(() -> "Field `" + fieldName
                    + "` is private. Going to check if it may be used here...");

            if (!inSSMethod) {
                throw getSSCSyntaxException(
                        "Cannot access private field `" + fieldName + "` from outside the superstruct", ctx
                );
            }
        }

        return dispatcher.super_visitPostfixExpression(ctx);
    }
}

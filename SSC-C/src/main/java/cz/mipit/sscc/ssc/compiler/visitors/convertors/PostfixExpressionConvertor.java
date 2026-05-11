package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.Function;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.System.lineSeparator;

public class PostfixExpressionConvertor extends AbstractConvertor<SSCParser.PostfixExpressionContext> {
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
        return dispatcher.visitSuper(ctx);
    }

    private Optional<String> getCompoundLiteralReplaced(SSCParser.PostfixExpressionContext ctx) {
        /* postfixExpression.typeName implies compound literal */
        if (ctx.typeName() == null) {
            return Optional.empty();
        }

        final String res = dispatcher.visitSuper(ctx);

        Main.logger.printDebug(() -> "superStructSpecifier in: "
                + dispatcher.getLiteral(ctx).replace(lineSeparator(), " ")
                + lineSeparator() + "\t\tReturning: " + res.replace(lineSeparator(), " "));

        return Optional.of(res);
    }

    public String convertMethodCall(final SSCParser.PostfixExpressionContext ctx) {
        enum ArrowOrDot {Arrow, Dot, Neither}
        final ArrowOrDot arrowOrDot =
                !ctx.Arrow().isEmpty() ? ArrowOrDot.Arrow
                        : !ctx.Dot().isEmpty() ? ArrowOrDot.Dot
                        : ArrowOrDot.Neither;

        Main.logger.printDebug(() -> arrowOrDot + " in: " + dispatcher.getLiteral(ctx));
        assert arrowOrDot != ArrowOrDot.Neither;

        if (ctx.Identifier().isEmpty())
            throw getSSCSyntaxException(arrowOrDot + " expression has no right side expression", ctx);

        final String objectName = dispatcher.visitPrimaryExpression(ctx.primaryExpression());
        final String currentFunctionName = dispatcher.getCurrentFunctionName();
        final Optional<SuperstructVariable> maybeVar = dispatcher.findSuperstructVariable(currentFunctionName, objectName);
        if (maybeVar.isEmpty()) {
            Main.logger.printDebug(() -> "\tVariable is not superstruct\t\tlocal vars: "
                    + dispatcher.data.functionVariables().get(currentFunctionName));
            return dispatcher.visitSuper(ctx);
        }
        final SuperstructVariable var = maybeVar.get();

        final SuperStruct superStruct = dispatcher.data.superStructs().get(var.ssName());
        if (superStruct == null) {
            throw dispatcher.getSSCSyntaxException(
                    "`superstruct " + var.ssName() + "` "
                            + "(type of variable \"" + var.getIdentifier() + "\") is not properly defined",
                    ctx
            );
        }

        final boolean hasLeftParen = !ctx.LeftParen().isEmpty();
        assert hasLeftParen == !ctx.RightParen().isEmpty();
        if (!hasLeftParen) {
            Main.logger.printDebug(() -> "\tNo parentheses");
            return getFieldAccessString(ctx, superStruct);
        }

        final String methodName = dispatcher.visitTerminal(ctx.Identifier().getFirst());
        final Optional<Function> maybeMethod = superStruct.findMethod(methodName);

        if (maybeMethod.isEmpty()) {
            Main.logger.printDebug(() -> "Variable does not have such a method");
            if (superStruct.fields()
                    .stream()
                    .anyMatch(decl -> decl.getName().equals(methodName))
            ) {
                return dispatcher.visitSuper(ctx);
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
            throw getSSCSyntaxException("Variable '" + var.getIdentifier() + "' is not a pointer to struct", ctx);
        }

        maybeMethod.ifPresent(functionDefinition -> {
            if (functionDefinition.isPrivate()) {
                Main.logger.printDebug(() -> "Method '" + methodName + "' is private. Going to check if it may be used here...");
                if (dispatcher.data.currentSS().isEmpty()
                        || !dispatcher.data.currentSS().get().name().equals(superStruct.name())) {
                    throw getSSCSyntaxException(
                            "Cannot access private method `" + methodName + "` from outside the superstruct", ctx);
                }
            }
        });

        final String ssName = superStruct.name();

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
        Main.logger.printDebug(() -> "Double colon in: " + dispatcher.getLiteral(ctx));

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

        final Optional<Function> maybeMethod = superstruct.findMethod(methodName);
        if (maybeMethod.isEmpty()) {
            throw getSSCSyntaxException(
                    "Superstruct '" + className
                            + "' has no method called '" + methodName
                            + "'", ctx);
        }
        final Function method = maybeMethod.get();

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

    private String getFieldAccessString(
            final SSCParser.PostfixExpressionContext ctx,
            final SuperStruct superstruct
    ) {
        final String fieldName = dispatcher.visitTerminal(ctx.Identifier(0));

        final List<Field> allMatching = superstruct.fields()
                .stream()
                .filter(field -> field.getName().equals(fieldName))
                .toList();
        if (allMatching.size() > 1) {
            throw getSSCSyntaxException(
                    "Found more than one matching field in superstruct `" + superstruct.name() + "`", ctx
            );
        }
        if (allMatching.isEmpty()) {
            throw getSSCSyntaxException(
                    "Found no field '" + fieldName + "' in superstruct `" + superstruct.name() + "`", ctx
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

        return dispatcher.visitSuper(ctx);
    }
}

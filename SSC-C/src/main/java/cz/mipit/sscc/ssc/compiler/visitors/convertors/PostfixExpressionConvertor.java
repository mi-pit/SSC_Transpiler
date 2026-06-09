package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Optional;

import static java.lang.System.lineSeparator;

public class PostfixExpressionConvertor
        extends AbstractConvertor<SSCParser.PostfixExpressionContext> {
    public PostfixExpressionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.PostfixExpressionContext.class);
    }

    @Override
    public String convert(SSCParser.PostfixExpressionContext ctx) {
        /* Compound literals for some reason count as postfix expressions */
        {
            final Optional<String> res = getCompoundLiteralReplaced(ctx);
            if (res.isPresent()) {
                return res.get();
            }
        }

        if (
                ctx.children.size() >= 3
                // must be first child
                && ctx.children.get(1) instanceof TerminalNode t
                && (t.getSymbol().getType() == SSCParser.Arrow || t.getSymbol().getType() == SSCParser.Dot)
        ) {
            return convertMethodCall(ctx);
        }
        return dispatcher.visitSuper(ctx);
    }

    private Optional<String> getCompoundLiteralReplaced(SSCParser.PostfixExpressionContext ctx) {
        // TODO: check private fields?

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
        enum ArrowOrDot {Arrow, Dot}

        assert ctx.typeName() == null;
        assert !ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty();

        assert !ctx.children.isEmpty();
        assert ctx.children.getFirst() instanceof SSCParser.PrimaryExpressionContext;
        assert ctx.children.get(1) instanceof TerminalNode t
               && (t.getSymbol().getType() == SSCParser.Arrow || t.getSymbol().getType() == SSCParser.Dot);

        final ArrowOrDot arrowOrDot = !ctx.Arrow().isEmpty()
                ? ArrowOrDot.Arrow
                : ArrowOrDot.Dot;

        Main.logger.printDebug(() -> arrowOrDot + " in '" + dispatcher.getLiteral(ctx) + "'");

        final StringBuilder expressionBuilder = new StringBuilder();

        final SSCParser.PrimaryExpressionContext primaryExprCtx = ctx.primaryExpression();
        if (primaryExprCtx.Identifier() == null
            && primaryExprCtx.templateDispatch() == null) {
            throw dispatcher.getSSCLanguageException(
                    "Invalid left-side operand of a " + arrowOrDot + " expression",
                    primaryExprCtx
            );
        }
        final String objectName = dispatcher.visit(primaryExprCtx);
        final String currentFn = dispatcher.getCurrentFunctionName();

        final Optional<SuperstructVariable> maybeSSVar = dispatcher.findSuperstructVariable(objectName);
        if (maybeSSVar.isEmpty()) {
            Main.logger.printDebug(() -> "\tVariable is not superstruct; vars (" + currentFn + "): "
                                         + dispatcher.state.currentVariables());
            return dispatcher.visitSuper(ctx);
        }
        final SuperstructVariable ssVar = maybeSSVar.get();

        final String superstructName = ssVar.getSuperstructName();
        final SuperStruct superStruct = dispatcher.state.getSuperstruct(superstructName);
        if (superStruct == null) {
            throw dispatcher.getSSCLanguageException(
                    "Could not find superstruct named '" + superstructName + "'",
                    primaryExprCtx
            );
        }

        final String methodName;
        {
            final ParseTree thirdChild = ctx.children.get(2);
            if (!(thirdChild instanceof TerminalNode t) || t.getSymbol().getType() != SSCParser.Identifier) {
                throw dispatcher.getSSCLanguageException(
                        "Invalid right-side operand of a " + arrowOrDot + " expression",
                        ctx
                );
            }
            methodName = dispatcher.visit(t);
        }

        final boolean isAField = superStruct.fields()
                .stream()
                .anyMatch(decl -> decl.getName().equals(methodName));
        if (isAField) {
            Main.logger.printDebug(() -> "\t\tSeems to be a field. No conversion");
            return dispatcher.visitSuper(ctx);
        }


        final Optional<SuperstructMethod> maybeMethod = superStruct.findMethod(methodName);

        if (maybeMethod.isEmpty()) {
            Main.logger.printDebug(() -> "\tVariable does not have such a method");

            if (dispatcher.state.currentSuperstruct().isEmpty() ||
                !dispatcher.state.currentSuperstruct().get().equals(superStruct)) {
                throw dispatcher.getSSCLanguageException(
                        "Superstruct '" + superStruct.name() + "' has no method called '" + methodName + "'",
                        ctx
                );
            }

            // TODO: save and check all the end of superstruct definition
            Main.logger.printDebug(() ->
                    "Did not find method `" + methodName + "`. " +
                    "Converting anyway and hoping it gets defined later"
            );
        } else if (maybeMethod.get().metadata().isPrivate()) {
            Main.logger.printDebug(() -> "Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (dispatcher.state.currentSuperstruct().isEmpty()
                || !dispatcher.state.currentSuperstruct().get().name().equals(superStruct.name())) {
                throw dispatcher.getSSCLanguageException(
                        "Cannot access private method `" + methodName + "` from outside the superstruct", ctx);
            }
        }
        final String qualifiedName = superStruct.qualifyName(methodName);
        expressionBuilder.append(qualifiedName);

        {
            if (ctx.children.size() <= 3
                || !(ctx.children.get(3) instanceof TerminalNode t)
                || t.getSymbol().getType() != SSCParser.LeftParen) {
                Main.logger.printDebug(() -> "\tNo parentheses");
                return dispatcher.visitSuper(ctx);
            }
        }
        expressionBuilder.append("( ");

        if (arrowOrDot == ArrowOrDot.Dot && !ssVar.getPointers().isEmpty()) {
            throw dispatcher.getSSCLanguageException("Cannot access non-local superstruct variable using `.`", ctx);
        }
        if (arrowOrDot == ArrowOrDot.Arrow && ssVar.getPointers().size() != 1) {
            throw dispatcher.getSSCLanguageException("Variable '" + ssVar.getIdentifier() + "' is not a pointer to struct", ctx);
        }

        if (arrowOrDot == ArrowOrDot.Dot) {
            expressionBuilder.append('&');
        }
        expressionBuilder.append(objectName);

        int currentChildIndex = 4;
        {
            final ParseTree fifthChild = ctx.children.get(currentChildIndex++);
            if (fifthChild instanceof SSCParser.ArgumentExpressionListContext argumentExprLs) {
                expressionBuilder.append(", ");
                expressionBuilder.append(
                        dispatcher.visit(argumentExprLs)
                );
            } else if (fifthChild instanceof TerminalNode t && t.getSymbol().getType() == SSCParser.RightParen) {
                expressionBuilder.append(" ");
                expressionBuilder.append(dispatcher.visit(t));
            } else {
                throw new AssertionError("Left-paren not followed by either arguments list or right-paren");
            }
        }

        assert ctx.children.size() == ctx.getChildCount();
        for (; currentChildIndex < ctx.getChildCount(); ++currentChildIndex) {
            final ParseTree child = ctx.children.get(currentChildIndex);

            assert !(child instanceof SSCParser.PrimaryExpressionContext);

            if (child instanceof TerminalNode t &&
                (t.getSymbol().getType() == SSCParser.RightParen ||
                 t.getSymbol().getType() == SSCParser.RightBracket ||
                 t.getSymbol().getType() == SSCParser.RightBrace)
            ) {
                expressionBuilder.append(' ');
            }

            expressionBuilder.append(dispatcher.visit(child));

            if (child instanceof TerminalNode t &&
                (t.getSymbol().getType() == SSCParser.LeftParen ||
                 t.getSymbol().getType() == SSCParser.LeftBracket ||
                 t.getSymbol().getType() == SSCParser.LeftBrace)
            ) {
                expressionBuilder.append(' ');
            }
        }
        assert currentChildIndex == ctx.getChildCount();

        Main.logger.printDebug(() -> "Final expression: " + expressionBuilder);

        return expressionBuilder.toString();
    }
}

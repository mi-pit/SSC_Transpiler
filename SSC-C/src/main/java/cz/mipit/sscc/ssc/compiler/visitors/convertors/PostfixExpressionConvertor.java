package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.collection.Box;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class PostfixExpressionConvertor
        extends AbstractConvertor<SSCParser.PostfixExpressionContext> {
    public PostfixExpressionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.PostfixExpressionContext.class);
    }

    @Override
    public String convert(SSCParser.PostfixExpressionContext ctx) {
        /* Compound literals for some reason count as postfix expressions */
        if (ctx.typeName() != null) {
            /* postfixExpression.typeName implies compound literal */
            // TODO: check private fields?
            return dispatcher.visitSuper(ctx);
        }

        if (ctx.children.size() >= 3
            && ctx.children.get(1) instanceof TerminalNode arrowOrDotNode
            && (arrowOrDotNode.getSymbol().getType() == SSCParser.Arrow
                || arrowOrDotNode.getSymbol().getType() == SSCParser.Dot)
        ) {
            return convertMethodCall(ctx, arrowOrDotNode);
        }

        return dispatcher.visitSuper(ctx);
    }

    //    : templateDispatch
    //    | (Identifier | templateDispatch) '::' Identifier
    //    | Identifier {this.LookupSymbol();}
    //    | constant
    //    | StringLiteral+
    //    | '(' expression ')'
    //    | genericSelection
    //    | lambdaFunction
    //    | switchExpression
    private Optional<SuperstructVariable> getSuperstructPrimaryExpression(SSCParser.PostfixExpressionContext ctx) {
        assert ctx.primaryExpression() != null;
        final SSCParser.PrimaryExpressionContext primaryExpression = ctx.primaryExpression();
        if (!primaryExpression.Identifier().isEmpty()) {
            final String varName = dispatcher.visit(
                    primaryExpression.Identifier().getFirst()
            );
            return (dispatcher.state.getSuperstructVariable(varName));
        }

        if (primaryExpression.LeftParen() != null) {
            final String visited = dispatcher.visit(
                    primaryExpression.expression()
            );
            return (dispatcher.state.getSuperstructVariable(visited));
        }

        return Optional.empty();
    }

    private enum ArrowOrDot {
        Arrow, Dot,
        ;

        private static ArrowOrDot fromCtx(TerminalNode node) {
            return switch (node.getSymbol().getType()) {
                case SSCParser.Arrow -> Arrow;
                case SSCParser.Dot -> Dot;
                default -> throw new IllegalStateException("Unknown ArrowOrDot: " + node.getSymbol().getType());
            };
        }
    }

    // postfixExpression
    //    : primaryExpression
    //   (
    //        '[' expression ']'
    //         | '(' argumentExpressionList? ')'                           /* function call */
    //         | ('.' | '->')  Identifier '(' argumentExpressionList? ')'  // SSC: Object method call
    //         | ('.' | '->')  Identifier                                  /* Attribute access (plain C) */
    //         | '++'
    //         | '--'
    //    )*
    public String _convertMethodCall(
            final SuperstructVariable ssVar,
            final String prevExpression,
            final List<ParseTree> children,
            final ParseTree ctx
    ) {
        Main.logger.printDebug("ssVar: " + ssVar + " for expr: " + prevExpression);
        if (children.isEmpty()
            || ssVar == null
            || !(children.getFirst() instanceof TerminalNode t
                 && (t.getSymbol().getType() == SSCParser.Arrow
                     || t.getSymbol().getType() == SSCParser.Dot))) {
            return prevExpression + SSCCUtil.getRestOfChildren(dispatcher, children);
        }

        final ArrowOrDot arrowOrDot = ArrowOrDot.fromCtx(t);
        if (!(children.get(1) instanceof TerminalNode methodNameNode) || methodNameNode.getSymbol().getType() != SSCParser.Identifier)
            throw new AssertionError();

        ensureExpressionPointerLevel(ssVar, ctx, arrowOrDot);

        final String methodName = dispatcher.visit(methodNameNode);
        final SuperStruct ss = ssVar.superstruct();

        final Optional<SuperstructMethod> maybeMethod = ss.findMethod(methodName);
        checkMethodAvailability(ctx, maybeMethod, ss, methodName);

        final String qualifiedName = ss.qualifyName(methodName);

        final SuperstructVariable typeOfExpression = maybeMethod
                .map(SuperstructMethod::returnType)
                .orElse(null);

        final Box<Integer> currentChildIndex = new Box<>(2);
        {
            final ParseTree nextChild = children.get(currentChildIndex.item++);
            assert nextChild instanceof TerminalNode lp && lp.getSymbol().getType() == SSCParser.LeftParen;
        }
        final String finalExpression = getFinalExpression(
                children, prevExpression, currentChildIndex, qualifiedName
        );

        return _convertMethodCall(
                typeOfExpression,
                finalExpression,
                children.subList(currentChildIndex.item, children.size()),
                ctx
        );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void checkMethodAvailability(ParseTree ctx,
                                         Optional<SuperstructMethod> maybeMethod,
                                         SuperStruct ss,
                                         String methodName) {
        if (maybeMethod.isEmpty()) {
            Main.logger.printDebug(() -> "\tVariable does not have such a method");

            if (dispatcher.state.currentSuperstruct().isEmpty() ||
                !dispatcher.state.currentSuperstruct().get().equals(ss)) {
                throw dispatcher.getSSCLanguageException(
                        "Superstruct '" + ss.name() + "' has no method called '" + methodName + "'",
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
                || !dispatcher.state.currentSuperstruct().get().name().equals(ss.name())) {
                throw dispatcher.getSSCLanguageException(
                        "Cannot access private method `" + methodName + "` from outside the superstruct", ctx);
            }
        }
    }

    private void ensureExpressionPointerLevel(SuperstructVariable ssVar, ParseTree ctx, ArrowOrDot arrowOrDot) {
        if (arrowOrDot == ArrowOrDot.Dot && !ssVar.getPointers().isEmpty()) {
            throw dispatcher.getSSCLanguageException(
                    "Cannot access non-local superstruct variable using '.'",
                    ctx
            );
        }
        final int ptrLvl = ssVar.getPointers().size();
        if (arrowOrDot == ArrowOrDot.Arrow && ptrLvl != 1) {
            final String hintString;
            if (ptrLvl == 0) {
                hintString = "(use '.')";
            } else {
                hintString = "(dereference with " + "*".repeat(ptrLvl) + ")";
            }
            throw dispatcher.getSSCLanguageException(
                    "Variable '" + ssVar.getIdentifier() + "' is not a pointer to struct " + hintString,
                    ctx
            );
        }
    }

    public String convertMethodCall(
            final SSCParser.PostfixExpressionContext ctx,
            final TerminalNode arrowOrDotNode
    ) {
        assert ctx.typeName() == null;
        assert !ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty();

        assert !ctx.children.isEmpty();
        assert ctx.children.getFirst() instanceof SSCParser.PrimaryExpressionContext;
        assert ctx.children.get(1) instanceof TerminalNode t
               && (t.getSymbol().getType() == SSCParser.Arrow || t.getSymbol().getType() == SSCParser.Dot);

        final ArrowOrDot arrowOrDot = ArrowOrDot.fromCtx(arrowOrDotNode);

        Main.logger.printDebug(() -> arrowOrDot + " in '" + dispatcher.getLiteral(ctx) + "'");

        final SSCParser.PrimaryExpressionContext primaryExprCtx = ctx.primaryExpression();
        if (primaryExprCtx.Identifier() == null
            && primaryExprCtx.templateDispatch() == null) {
            throw dispatcher.getSSCLanguageException(
                    "Invalid left-side operand of a " + arrowOrDot + " expression",
                    primaryExprCtx
            );
        }
        final String currentFn = dispatcher.getCurrentFunctionName();

        final Optional<SuperstructVariable> maybeSSVar = getSuperstructPrimaryExpression(ctx);
        if (maybeSSVar.isEmpty()) {
            Main.logger.printDebug(() -> "\tExpression \"" + dispatcher.getLiteral(ctx.primaryExpression())
                                         + "\" is not superstruct; vars (" + currentFn + "): "
                                         + dispatcher.state.currentVariables());
            return dispatcher.visitSuper(ctx);
        }
        final SuperstructVariable ssVar = maybeSSVar.get();
        final SuperStruct superStruct = ssVar.superstruct();

        final ParseTree thirdChild = ctx.children.get(2);
        if (!(thirdChild instanceof TerminalNode methodNameIdentifierNode) || methodNameIdentifierNode.getSymbol().getType() != SSCParser.Identifier) {
            throw dispatcher.getSSCLanguageException(
                    "Invalid right-side operand of a " + arrowOrDot + " expression: expected method name identifier",
                    ctx
            );
        }
        final String methodName = dispatcher.visit(methodNameIdentifierNode);

        final boolean isAField = superStruct.fields()
                .stream()
                .anyMatch(decl -> decl.getName().equals(methodName));
        if (isAField) {
            Main.logger.printDebug(() -> "\t\tSeems to be a field. No conversion");
            return dispatcher.visitSuper(ctx);
        }


        final Optional<SuperstructMethod> maybeMethod = superStruct.findMethod(methodName);
        checkMethodAvailability(ctx, maybeMethod, superStruct, methodName);

        final String qualifiedName = superStruct.qualifyName(methodName);

        {
            if (ctx.children.size() <= 3
                || !(ctx.children.get(3) instanceof TerminalNode t)
                || t.getSymbol().getType() != SSCParser.LeftParen) {
                Main.logger.printDebug(() -> "\tNo parentheses");
                return dispatcher.visitSuper(ctx);
            }
        }

        ensureExpressionPointerLevel(ssVar, ctx.primaryExpression(), arrowOrDot);

        final StringBuilder selfRef = new StringBuilder();

        if (arrowOrDot == ArrowOrDot.Dot) {
            selfRef.append('&');
        }
        selfRef.append(ssVar.getIdentifier());

        final Box<Integer> currentChildIndex = new Box<>(4);

        final String finalExpression = getFinalExpression(
                ctx.children,
                selfRef.toString(),
                currentChildIndex,
                qualifiedName
        );

        Main.logger.printDebug(() -> "Final expression: " + finalExpression);

        final SuperstructVariable typeOfExpression = maybeMethod.map(SuperstructMethod::returnType).orElse(null);

        return _convertMethodCall(
                typeOfExpression,
                finalExpression,
                ctx.children.subList(currentChildIndex.item, ctx.children.size()),
                methodNameIdentifierNode
        );
    }

    private String getFinalExpression(
            List<ParseTree> children,
            String selfRef,
            Box<Integer> currentChildIndex,
            String qualifiedName
    ) {
        final StringJoiner argsJoiner = new StringJoiner(", ");
        argsJoiner.add(selfRef);
        {
            final ParseTree nextChild = children.get(currentChildIndex.item);
            if (nextChild instanceof SSCParser.ArgumentExpressionListContext) {
                currentChildIndex.item++;
                argsJoiner.add(
                        dispatcher.visit(nextChild)
                );
            }
        }
        {
            final ParseTree nextChild = children.get(currentChildIndex.item++);
            assert nextChild instanceof TerminalNode rp && rp.getSymbol().getType() == SSCParser.RightParen;
        }

        return qualifiedName + "( " + argsJoiner + " )";
    }
}

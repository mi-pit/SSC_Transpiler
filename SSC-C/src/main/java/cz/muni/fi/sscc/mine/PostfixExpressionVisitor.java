package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.SSCSyntaxException;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.data.SSMember;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PostfixExpressionVisitor extends ExpressionVisitor {
    public PostfixExpressionVisitor(CommonTokenStream tokens, Collection<SuperStructRepre> sss) {
        super(tokens, sss);
    }

    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        // FIXME
        if (!ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty()) {
            return convertMethod(ctx);
        }
        if (!ctx.DoubleColon().isEmpty()) {
            return convertStaticFunction(ctx);
        }
        return Util.getContextText(ctx, tokens);
    }

    public String convertStaticFunction(SSCParser.PostfixExpressionContext ctx) {
        System.out.println("[DEBUG] Double colon in: " + Util.getContextText(ctx, tokens));

        boolean hasParens = ctx.LeftParen() != null && ctx.RightParen() != null;
        if (!hasParens) {
            throw new SSCSyntaxException("Can not access static `superstruct` member", ctx, tokens);
        }

        if (ctx.primaryExpression() == null)
            throw new SSCSyntaxException("Arrow or dot expression has no left side (Superstruct name) expression", ctx, tokens);
        final String className = Util.getContextText(ctx.primaryExpression(), tokens);

        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException("Double colon expression has no right side (function) expression", ctx, tokens);
        final String methodName = ctx.Identifier().get(0).toString();

        verifyStaticCall(ctx, className, methodName);

        List<String> args = new ArrayList<>();
        for (SSCParser.ArgumentExpressionListContext argListCtx : ctx.argumentExpressionList()) {
            for (SSCParser.AssignmentExpressionContext assExprCtx : argListCtx.assignmentExpression()) {
                // recurse into nested expressions
                args.add(visit(assExprCtx));
            }
        }

        final String result = className + "__" + methodName + "(" + String.join(", ", args) + ")";
        System.out.println("\t[DEBUG] Result: " + result);
        return result;
    }

    private void verifyStaticCall(SSCParser.PostfixExpressionContext ctx, String className, String methodName) {
        if (superstructs.stream().noneMatch(s -> s.name().equals(className))) {
            throw new SSCSyntaxException("Could not find superstruct with name `" + className + "`", ctx, tokens);
        }

        if (superstructs.stream()
                .filter(s -> s.name().equals(className))
                .findFirst().orElseThrow(() -> new IllegalStateException("Must be present"))
                .member().stream()
                .filter(SSMember::isFunctionDefinition)
                .map(ssMember -> ssMember.getData().getRight().get(/* safe */).name())
                .noneMatch(methodName::equals)) {
            throw new SSCSyntaxException("superstruct with name `" + className + "` has no method called `" + methodName + "`", ctx, tokens);
        }
    }


    public String convertMethod(SSCParser.PostfixExpressionContext ctx) {
        System.out.println("[DEBUG] Arrow or Dot in: " + Util.getContextText(ctx, tokens));

        enum ArrowOrDot {Arrow, Dot, Neither}
        final ArrowOrDot arrowOrDot =
                !ctx.Arrow().isEmpty() ? ArrowOrDot.Arrow
                        : ctx.Dot().isEmpty() ? ArrowOrDot.Dot
                        : ArrowOrDot.Neither;

        assert arrowOrDot != ArrowOrDot.Neither;

        final boolean hasLeftParen = ctx.LeftParen() != null;
        final boolean hasRightParen = ctx.RightParen() != null;
        final boolean hasParens = hasLeftParen && hasRightParen;
        if (!hasParens) {
            System.out.println("[DEBUG] No parentheses");
            return Util.getContextText(ctx, tokens);
        }

        final StringBuilder finalExpression = new StringBuilder();

        final String objectName = Util.getContextText(ctx.primaryExpression(), tokens);
        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException("Arrow or dot expression has no right side expression", ctx, tokens);
        final String methodName = ctx.Identifier().get(0).toString();

        finalExpression
                .append(/* fixme: missing ss name SSNAME__ */methodName)
                .append("(")
                .append(objectName);

        if (!ctx.argumentExpressionList().isEmpty()) {
            finalExpression.append(", ");
        }

        for (var arg : ctx.argumentExpressionList()) {
            try {
                // FIXME
                finalExpression.append(visitPostfixExpression(
                        arg.assignmentExpression(0)
                                .assignmentExpression()
                                .conditionalExpression()
                                .logicalOrExpression()
                                .logicalAndExpression(0)
                                .inclusiveOrExpression(0)
                                .exclusiveOrExpression(0)
                                .andExpression(0)
                                .equalityExpression(0)
                                .relationalExpression(0)
                                .shiftExpression(0)
                                .additiveExpression(0)
                                .multiplicativeExpression(0)
                                .castExpression(0)
                                .unaryExpression()
                                .postfixExpression()));
            } catch (NullPointerException e) {
                System.out.println("\t[DEBUG] Appending raw text");
                finalExpression.append(Util.getContextText(arg, tokens));
                //e.printStackTrace();
            }
        }
        finalExpression.append(')');

        System.out.println("\t[DEBUG] Final Expression: " + finalExpression);
        return finalExpression.toString();
    }
}

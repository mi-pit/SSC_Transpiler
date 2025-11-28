package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.SSCSyntaxException;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.data.*;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static cz.muni.fi.sscc.util.Util.getContextText;
import static cz.muni.fi.sscc.util.Util.printDebug;

public class PostfixExpressionConvertorVisitor extends ConvertorVisitor {
    private final Collection<SuperStructRepre> superstructs;

    public final Map<String/* Function name */, List<SuperstructVariable>> functionVariables = new HashMap<>();

    public PostfixExpressionConvertorVisitor(CommonTokenStream tokens,
                                             Collection<SuperStructRepre> sss) {
        super(tokens);
        this.superstructs = sss;
    }

    @Override
    public String visitFunctionDefinition(SSCParser.FunctionDefinitionContext ctx) {
        assert ctx.compoundStatement() != null;

        String funcName = FunctionDefinition.parseName(ctx.declarator(), tokens);
        functionVariables.put(funcName, new ArrayList<>());

        if (ctx.compoundStatement().blockItemList() == null) {
            // no function body
            return super.visitFunctionDefinition(ctx);
        }

        List<SSCParser.DeclarationContext> declarations = ctx.compoundStatement()
                .blockItemList()
                .blockItem()
                .stream()
                .map(SSCParser.BlockItemContext::declaration)
                .filter(Objects::nonNull)
                .filter(d -> d.staticAssertDeclaration() == null)
                .toList();

        extractFunctionVariables(declarations, funcName);
        // System.out.println("[DEBUG] In function `" + funcName + "`\tvars: " + functionVariables.get(funcName));

        return super.visitFunctionDefinition(ctx);
    }

    private void extractFunctionVariables(List<SSCParser.DeclarationContext> declarations, String funcName) {
        for (SSCParser.DeclarationContext declaration : declarations) {
            final List<SSCParser.DeclarationSpecifierContext> specs =
                    declaration.declarationSpecifiers().declarationSpecifier();

            final Optional<String> compoundTypeName = specs.stream()
                    .filter(Objects::nonNull)
                    .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                    .filter(Objects::nonNull)
                    .filter(spec -> spec.superStructSpecifier() != null
                            || (spec.structOrUnionSpecifier() != null
                            && spec.structOrUnionSpecifier().structOrUnion().Struct() != null))
                    .map(s -> s.superStructSpecifier() != null
                            ? s.superStructSpecifier().Identifier().getText()
                            : s.structOrUnionSpecifier().Identifier().getText()
                    )
                    .findFirst();
            if (compoundTypeName.isEmpty()) {
                // not ss
                continue;
            }

            if (declaration.initDeclaratorList() == null) {
                printDebug("Decl without name: `" + getContextText(declaration, tokens) + "`");

                final String name = getContextText(specs.get(specs.size() - 1).typeSpecifier(), tokens);

                functionVariables.get(funcName).add(new SuperstructVariable(
                        compoundTypeName.get(),
                        false,
                        name
                ));
                continue;
            }

            // init decl list
            functionVariables.get(funcName).addAll(
                    declaration
                            .initDeclaratorList()
                            .initDeclarator()
                            .stream()
                            .map(initDecl -> new SuperstructVariable(
                                    compoundTypeName.get(),
                                    initDecl.declarator().pointer() != null,
                                    initDecl.declarator().directDeclarator().Identifier().getText()
                            ))
                            .toList()
            );
        }
    }


    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        assert ctx != null;
        ParserRuleContext parent = ctx;
        while (parent != null && !(parent instanceof SSCParser.FunctionDefinitionContext)) {
            parent = parent.getParent();
        }
        if (parent == null) {
            return super.visitPostfixExpression(ctx);
        }
        final SSCParser.FunctionDefinitionContext funcCtx = (SSCParser.FunctionDefinitionContext) parent;
        final String functionName = FunctionDefinition.parseName(funcCtx.declarator(), tokens);


        if (!ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty()) {
            return convertMethod(ctx, functionName);
        }
        if (!ctx.DoubleColon().isEmpty()) {
            return convertStaticFunction(ctx);
        }
        return getContextText(ctx, tokens);
    }

    public String convertStaticFunction(SSCParser.PostfixExpressionContext ctx) {
        printDebug("Double colon in: %s\n", getContextText(ctx, tokens));

        boolean hasParens = ctx.LeftParen() != null && ctx.RightParen() != null;
        if (!hasParens) {
            throw new SSCSyntaxException("Can not access static `superstruct` member", ctx, tokens);
        }

        if (ctx.primaryExpression() == null)
            throw new SSCSyntaxException("Arrow or dot expression has no left side (Superstruct name) expression", ctx, tokens);
        final String className = getContextText(ctx.primaryExpression(), tokens);

        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException("Double colon expression has no right side (function) expression", ctx, tokens);
        final String methodName = ctx.Identifier().get(0).toString();

        verifyStaticCall(ctx, className, methodName);

        final List<String> args = new ArrayList<>();
        for (SSCParser.ArgumentExpressionListContext argListCtx : ctx.argumentExpressionList()) {
            for (SSCParser.AssignmentExpressionContext assExprCtx : argListCtx.assignmentExpression()) {
                args.add(visit(assExprCtx));
            }
        }

        final String result = className + "__" + methodName + "( " + String.join(", ", args) + " )";
        printDebug("\tResult: " + result);
        return result;
    }

    private void verifyStaticCall(SSCParser.PostfixExpressionContext ctx, String className, String methodName) {
        if (superstructs.stream().noneMatch(s -> s.name().equals(className))) {
            throw new SSCSyntaxException("Could not find superstruct with name `" + className + "`", ctx, tokens);
        }

        //noinspection OptionalGetWithoutIsPresent
        if (superstructs.stream()
                .filter(s -> s.name().equals(className))
                .findFirst().orElseThrow(() -> new IllegalStateException("Must be present"))
                .member().stream()
                .filter(SSMember::isFunctionDefinition)
                .map(ssMember -> ssMember.getData().getRight().get(/* safe because ifFnDef above */).name())
                .noneMatch(methodName::equals)) {
            throw new SSCSyntaxException(
                    "superstruct with name `" + className
                            + "` has no method called `" + methodName
                            + "`", ctx, tokens);
        }
    }

    public String convertMethod(SSCParser.PostfixExpressionContext ctx, String functionName) {
        enum ArrowOrDot {Arrow, Dot, Neither}
        final ArrowOrDot arrowOrDot =
                !ctx.Arrow().isEmpty() ? ArrowOrDot.Arrow
                        : !ctx.Dot().isEmpty() ? ArrowOrDot.Dot
                        : ArrowOrDot.Neither;

        assert arrowOrDot != ArrowOrDot.Neither;
        printDebug(arrowOrDot + " in: " + getContextText(ctx, tokens));

        final boolean hasLeftParen = !ctx.LeftParen().isEmpty();
        final boolean hasRightParen = !ctx.RightParen().isEmpty();
        final boolean hasParens = hasLeftParen && hasRightParen;
        if (!hasParens) {
            printDebug("\tNo parentheses");
            return getContextText(ctx, tokens);
        }

        final StringBuilder finalExpression = new StringBuilder();

        final String objectName = getContextText(ctx.primaryExpression(), tokens);
        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException("Arrow or dot expression has no right side expression", ctx, tokens);
        final String methodName = ctx.Identifier().get(0).toString();

        final Optional<SuperstructVariable> foundVar = functionVariables.get(functionName).stream()
                .filter(var -> var.name().equals(objectName))
                .findFirst();
        if (foundVar.isEmpty() || superstructs.stream().noneMatch(s -> s.name().equals(foundVar.get().type()))) {
            // Variable is not superstruct or doesn't have such a method
            return getContextText(ctx, tokens);
        }
        if (foundVar.get().pointer() && arrowOrDot == ArrowOrDot.Dot) {
            throw new SSCSyntaxException("Pointer to superstruct must be accessed with `->`", ctx, tokens);
        }

        final String ssName = foundVar.get().type();

        finalExpression
                .append(ssName)
                .append("__")
                .append(methodName)
                .append("( ");

        if (arrowOrDot == ArrowOrDot.Dot) {
            finalExpression.append("&");
        }
        finalExpression.append(objectName);

        if (!ctx.argumentExpressionList().isEmpty()) {
            finalExpression.append(", ");
        }

        List<String> args = new ArrayList<>();
        for (SSCParser.ArgumentExpressionListContext argListCtx : ctx.argumentExpressionList()) {
            for (SSCParser.AssignmentExpressionContext assExprCtx : argListCtx.assignmentExpression()) {
                args.add(visit(assExprCtx));
            }
        }

        finalExpression
                .append(String.join(", ", args))
                .append(" )");

        printDebug("\tFinal Expression: " + finalExpression);
        return finalExpression.toString();
    }


    @Override
    public String visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        if (ctx.superStructBody() == null) {
            return "struct " + ctx.Identifier().getText() + " ";
        }
        return super.visitSuperStructSpecifier(ctx);
    }
}

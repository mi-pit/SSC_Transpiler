package cz.muni.fi.sscc.visitors;

import antlr.SSCParser;
import cz.muni.fi.sscc.Main;
import cz.muni.fi.sscc.data.FunctionDefinition;
import cz.muni.fi.sscc.data.SSMember;
import cz.muni.fi.sscc.data.SuperStructRepre;
import cz.muni.fi.sscc.data.SuperstructVariable;
import cz.muni.fi.sscc.exceptions.SSCSyntaxException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

import static cz.muni.fi.sscc.util.Util.getContextText;

public class PostfixExpressionConvertorVisitor extends ConvertorVisitor {
    private final Collection<SuperStructRepre> superstructs;

    public final Map<String/* Function name */, List<SuperstructVariable>> functionVariables = new HashMap<>();

    public PostfixExpressionConvertorVisitor(CommonTokenStream tokens,
                                             Collection<SuperStructRepre> sss) {
        super(tokens);
        this.superstructs = sss;
    }

    @Override
    public String visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
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

        extractFunctionVariables(ctx, declarations, funcName);
        // Main.logger.printDebug("In function `" + funcName + "`\tvars: " + functionVariables.get(funcName));

        return super.visitFunctionDefinition(ctx);
    }

    private void extractFunctionVariables(
            final SSCParser.FunctionDefinitionContext ctx,
            final List<SSCParser.DeclarationContext> declarations,
            final String funcName
    ) {
        final List<SuperstructVariable> args = new ArrayList<>();
        if (ctx.declarator().directDeclarator().parameterTypeList() != null) {
            /* else function declaration without a prototype */
            for (SSCParser.ParameterDeclarationContext param :
                    ctx.declarator().directDeclarator().parameterTypeList().parameterList().parameterDeclaration()) {
                if (param.declarationSpecifiers() == null) {
                    continue;
                }
                final Optional<String> struct = param
                        .declarationSpecifiers()
                        .declarationSpecifier()
                        .stream()
                        .filter(specifier -> specifier.typeSpecifier() != null
                                && specifier.typeSpecifier().superStructSpecifier() != null)
                        .map(s -> s.typeSpecifier().superStructSpecifier().Identifier().getText())
                        .findFirst();
                if (struct.isEmpty()) {
                    continue; // no ss
                }
                final SuperstructVariable var = new SuperstructVariable(
                        struct.get(),
                        param.declarator().pointer() != null,
                        param.declarator().directDeclarator().Identifier().getText()
                );
                args.add(var);
            }

            functionVariables.get(funcName).addAll(args);
        }

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
                Main.logger.printDebug("Decl without name: `" + getContextText(declaration, tokens) + "`");

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
                                    getContextText(initDecl.declarator().directDeclarator(), tokens)
                            ))
                            .toList()
            );
        }
    }


    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        assert ctx != null;

        /* Compound literals for some reason have to be converted here */
        Optional<String> res = getCompoundLiteralReplaced(ctx);
        if (res.isPresent()) {
            return res.get();
        }

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
        return visitChildren(ctx);
    }

    private Optional<String> getCompoundLiteralReplaced(SSCParser.PostfixExpressionContext ctx) {
        try {
            var ss = ctx.typeName().specifierQualifierList().typeSpecifier().superStructSpecifier();
            if (ss != null) {
                final String res = getContextText(ctx, tokens)
                        .replaceFirst("\\(\\s*superstruct\\s+", "( struct ");
                Main.logger.printDebug("superStructSpecifier in: " + getContextText(ctx, tokens).replace("\n", " ")
                        + "\n\t\tReturning: " + res.replace("\n", " "));
                return Optional.of(res);
            }
        } catch (NullPointerException ignored) {
            //System.out.println("[DEBUG] Null pointer exception in: " + getContextText(ctx, tokens).replace("\n", ""));
        }
        return Optional.empty();
    }

    public String convertStaticFunction(SSCParser.PostfixExpressionContext ctx) {
        Main.logger.printDebug("Double colon in: %s\n", getContextText(ctx, tokens));

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
        Main.logger.printDebug("\tResult: " + result);
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
                .map(ssMember -> ssMember.data().getRight().get(/* safe because ifFnDef above */).name())
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
        Main.logger.printDebug(arrowOrDot + " in: " + getContextText(ctx, tokens));

        final boolean hasLeftParen = !ctx.LeftParen().isEmpty();
        final boolean hasRightParen = !ctx.RightParen().isEmpty();
        final boolean hasParens = hasLeftParen && hasRightParen;
        if (!hasParens) {
            Main.logger.printDebug("\tNo parentheses");
            return getContextText(ctx, tokens);
        }

        final String objectName = getContextText(ctx.primaryExpression(), tokens);
        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException("Arrow or dot expression has no right side expression", ctx, tokens);
        final String methodName = ctx.Identifier().get(0).toString();

        final Optional<SuperstructVariable> foundVar = functionVariables.get(functionName).stream()
                .filter(var -> var.name().equals(objectName))
                .findFirst();
        if (foundVar.isEmpty()) {
            Main.logger.printDebug("\tVariable is not superstruct");
            Main.logger.printDebug("\t\tlocal vars: " + functionVariables.get(functionName));
            return getContextText(ctx, tokens);
        }

        final Optional<SuperStructRepre> optSS = superstructs
                .stream()
                .filter(s -> s.name().equals(foundVar.get().type()))
                .findAny();
        if (optSS.isEmpty()) {
            throw new RuntimeException(
                    "Could not find superstruct with name `" + foundVar.get().name() + "` even after finding such a variable");
        }

        boolean found = false;
        for (FunctionDefinition func : optSS.get().member().stream()
                .filter(SSMember::isFunctionDefinition)
                .map(member -> member.data().getRight().get())
                .toList()) {
            if (func.name().equals(methodName)) {
                found = true;
                break;
            }
        }
        if (!found) {
            Main.logger.printDebug("Variable does not have such a method");
            if (optSS.get().member()
                    .stream()
                    .filter(SSMember::isDeclaration)
                    .map(mem -> mem.data().getLeft().get())
                    .noneMatch(decl -> decl.data().contains(methodName))) {
                throw new SSCSyntaxException(
                        "superstruct " + optSS.get().name() + " has no member called `" + methodName + "`",
                        ctx, tokens);
            }
            return getContextText(ctx, tokens);
        }

        if (foundVar.get().pointer() && arrowOrDot == ArrowOrDot.Dot) {
            throw new SSCSyntaxException("Pointer to superstruct must be accessed with `->`", ctx, tokens);
        }

        final String ssName = optSS.get().name();

        final StringBuilder finalExpression =
                new StringBuilder(ssName)
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

        final List<String> args = new ArrayList<>();
        for (SSCParser.ArgumentExpressionListContext argListCtx : ctx.argumentExpressionList()) {
            for (SSCParser.AssignmentExpressionContext assExprCtx : argListCtx.assignmentExpression()) {
                args.add(visit(assExprCtx));
            }
        }

        finalExpression
                .append(String.join(", ", args))
                .append(" )");

        Main.logger.printDebug("\tFinal Expression: " + finalExpression);
        return finalExpression.toString();
    }


    @Override
    public String visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        if (ctx.superStructBody() == null) {
            return "struct " + ctx.Identifier().getText() + " ";
        }
        return super.visitSuperStructSpecifier(ctx);
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        if (node.getText().equals("superstruct")) {
            return "struct ";
        }
        return super.visitTerminal(node);
    }
}

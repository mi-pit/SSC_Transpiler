package cz.muni.fi.sscc.visitors;

import antlr.SSCParser;
import cz.muni.fi.sscc.Main;
import cz.muni.fi.sscc.data.Field;
import cz.muni.fi.sscc.data.FunctionDefinition;
import cz.muni.fi.sscc.data.SSMember;
import cz.muni.fi.sscc.data.SuperStructRepre;
import cz.muni.fi.sscc.data.SuperstructVariable;
import cz.muni.fi.sscc.exceptions.SSCSyntaxException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

import static cz.muni.fi.sscc.util.Strings.getContextText;

public class PostfixExpressionConvertorVisitor extends ConvertorVisitor {
    private final Collection<SuperStructRepre> superstructs;

    public final Map<String /* Function name */, Set<SuperstructVariable>> functionVariables = new HashMap<>();

    public PostfixExpressionConvertorVisitor(CommonTokenStream tokens,
                                             Collection<SuperStructRepre> sss) {
        super(tokens);
        this.superstructs = sss;
    }

    private String currentMethodName = null;

    @Override
    public String visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
        assert ctx.compoundStatement() != null;

        if (ctx.declarationList() != null) {
            throw new SSCSyntaxException("K&R C-style declarations are invalid in SSC", ctx, tokens);
        }

        final String funcName = FunctionDefinition.parseName(ctx.declarator(), tokens);
        functionVariables.put(funcName, new HashSet<>());

        if (ctx.compoundStatement().blockItemList() == null) {
            // no function body
            return super.visitFunctionDefinition(ctx);
        }

        extractFunctionVariables(ctx, funcName);

        Main.logger.printDebug("In function `" + funcName + "`; vars: " + functionVariables.get(funcName));

        return super.visitFunctionDefinition(ctx);
    }

    private void extractFunctionVariables(
            final SSCParser.FunctionDefinitionContext ctx,
            final String funcName
    ) {
        if ("loop".equals(funcName)) {
            Main.logger.printDebug("here");
        }

        extractFunctionArguments(ctx, functionVariables.get(funcName));

        /*
         * TODO: recurse
         *  currently, this only reads top level declarations
         */
        final List<SSCParser.DeclarationContext> declarations =
                getDeclarationsFromCompoundStatement(ctx.compoundStatement());

        for (SSCParser.DeclarationContext declaration : declarations) {
            final List<SSCParser.DeclarationSpecifierContext> specs =
                    declaration.declarationSpecifiers().declarationSpecifier();

            final Optional<String> superstructName = findSuperstructNameFromDeclSpecs(specs);
            if (superstructName.isEmpty()) {
                continue;
            }

            if (declaration.initDeclaratorList() == null) {
                Main.logger.printDebug("Decl without name: `" + getContextText(declaration, tokens) + "`");

                final String name = getContextText(specs.get(specs.size() - 1).typeSpecifier(), tokens);

                functionVariables.get(funcName).add(new SuperstructVariable(
                        superstructName.get(),
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
                                    superstructName.get(),
                                    initDecl.declarator().pointer() != null,
                                    getContextText(initDecl.declarator().directDeclarator(), tokens)
                            ))
                            .toList()
            );
        }
    }

    public static List<SSCParser.DeclarationContext> getDeclarationsFromCompoundStatement(
            final SSCParser.CompoundStatementContext ctx
    ) {
        if (ctx.blockItemList() == null) {
            return Collections.emptyList();
        }

        final List<SSCParser.DeclarationContext> list = new ArrayList<>();
        for (var blockItemContext : ctx.blockItemList().blockItem()) {
            final var statement = blockItemContext.statement();
            if (statement != null && statement.compoundStatement() != null) {
                list.addAll(getDeclarationsFromCompoundStatement(blockItemContext.statement().compoundStatement()));
            }

            final var declaration = blockItemContext.declaration();
            if (declaration != null && declaration.staticAssertDeclaration() == null) {
                list.add(declaration);
            }
        }
        return list;
    }

    private static Optional<String> findSuperstructNameFromDeclSpecs(List<SSCParser.DeclarationSpecifierContext> specs) {
        for (SSCParser.DeclarationSpecifierContext spec : specs) {
            if (spec != null) {
                var typeSpec = spec.typeSpecifier();
                if (typeSpec != null && typeSpec.superStructSpecifier() != null) {
                    return Optional.of(typeSpec.superStructSpecifier().Identifier().getText());
                }
            }
        }
        return Optional.empty();
    }

    private void extractFunctionArguments(SSCParser.FunctionDefinitionContext ctx,
                                          final Set<SuperstructVariable> vars) {
        for (SSCParser.ParameterDeclarationContext param :
                ctx.declarator().directDeclarator().parameterTypeList().parameterList().parameterDeclaration()) {
            if (param.declarationSpecifiers() == null) {
                continue;
            }

            final Optional<String> superstructName = param
                    .declarationSpecifiers()
                    .declarationSpecifier()
                    .stream()
                    .filter(specifier -> specifier.typeSpecifier() != null
                            && specifier.typeSpecifier().superStructSpecifier() != null)
                    .map(s -> s.typeSpecifier().superStructSpecifier().Identifier().getText())
                    .findFirst();
            if (superstructName.isEmpty()) {
                continue;
            }

            vars.add(new SuperstructVariable(
                    superstructName.get(),
                    param.declarator().pointer() != null,
                    param.declarator().directDeclarator().Identifier().getText()
            ));
        }
    }

    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        assert ctx != null;

        /* Compound literals for some reason have to be converted here */
        final Optional<String> res = getCompoundLiteralReplaced(ctx);
        if (res.isPresent()) {
            return res.get();
        }

        Optional<ParserRuleContext> parent = getParent(ctx);
        if (parent.isEmpty()) {
            return super.visitPostfixExpression(ctx);
        }
        final SSCParser.FunctionDefinitionContext funcCtx = (SSCParser.FunctionDefinitionContext) parent.get();
        final String functionName = FunctionDefinition.parseName(funcCtx.declarator(), tokens);

        if (!ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty()) {
            return convertMethodCall(ctx, functionName);
        }
        if (!ctx.DoubleColon().isEmpty()) {
            return convertStaticFunctionCall(ctx, functionName);
        }
        return visitChildren(ctx);
    }

    private static Optional<ParserRuleContext> getParent(SSCParser.PostfixExpressionContext ctx) {
        ParserRuleContext parent = ctx;
        while (parent != null && !(parent instanceof SSCParser.FunctionDefinitionContext)) {
            parent = parent.getParent();
        }
        return Optional.ofNullable(parent);
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
        }
        return Optional.empty();
    }

    public String convertStaticFunctionCall(final SSCParser.PostfixExpressionContext ctx,
                                            final String ctxFunctionName) {
        Main.logger.printDebug("Double colon in: %s\n", getContextText(ctx, tokens));

        if (ctx.LeftParen() == null || ctx.RightParen() == null) {
            assert false; // TODO? add to grammar for better error messages
            throw new SSCSyntaxException("Static superstruct access not a function call", ctx, tokens);
        }

        if (ctx.primaryExpression() == null)
            throw new SSCSyntaxException("Double colon expression has no left side (Superstruct name) expression", ctx, tokens);
        final String className = getContextText(ctx.primaryExpression(), tokens);

        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException("Double colon expression has no right side (function) expression", ctx, tokens);
        final String methodName = ctx.Identifier().get(0).toString();

        verifyStaticCall(ctx, ctxFunctionName, className, methodName);

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

    private void verifyStaticCall(final SSCParser.PostfixExpressionContext ctx,
                                  final String ctxFunctionName,
                                  final String className,
                                  final String methodName) {
        final Optional<SuperStructRepre> maybeSS = findSuperstructByName(className);
        if (maybeSS.isEmpty()) {
            throw new SSCSyntaxException("Could not find superstruct with name `" + className + "`", ctx, tokens);
        }
        final SuperStructRepre superstruct = maybeSS.get();

        final Optional<FunctionDefinition> maybeMethod = findSuperstructMethod(methodName, superstruct);
        if (maybeMethod.isEmpty()) {
            throw new SSCSyntaxException(
                    "Superstruct with name `" + className
                            + "` has no method called `" + methodName
                            + "`", ctx, tokens);
        }
        final FunctionDefinition method = maybeMethod.get();

        if (method.isPrivate()) {
            Main.logger.printDebug("Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (notInSuperstructMethod(ctxFunctionName)) {
                throw new SSCSyntaxException(
                        "Cannot access private static method `" + methodName + "` from outside the superstruct", ctx, tokens
                );
            }
        }
    }

    private static Optional<FunctionDefinition> findSuperstructMethod(final String methodName,
                                                                      final SuperStructRepre ss) {
        for (SSMember mem : ss.members()) {
            final Optional<FunctionDefinition> maybeFunc = mem.data().getRight();
            if (maybeFunc.isPresent()
                    && methodName.equals(maybeFunc.get().getName())) {
                return maybeFunc;
            }
        }
        return Optional.empty();
    }

    private Optional<SuperStructRepre> findSuperstructByName(final String className) {
        for (SuperStructRepre s : superstructs) {
            if (s.name().equals(className)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public String convertMethodCall(final SSCParser.PostfixExpressionContext ctx,
                                    final String functionName) {
        enum ArrowOrDot {Arrow, Dot, Neither}
        final ArrowOrDot arrowOrDot =
                !ctx.Arrow().isEmpty() ? ArrowOrDot.Arrow
                        : !ctx.Dot().isEmpty() ? ArrowOrDot.Dot
                        : ArrowOrDot.Neither;

        Main.logger.printDebug(arrowOrDot + " in: " + getContextText(ctx, tokens));
        assert arrowOrDot != ArrowOrDot.Neither;

        final String objectName = getContextText(ctx.primaryExpression(), tokens);
        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException(arrowOrDot + " expression has no right side expression", ctx, tokens);

        final Optional<SuperstructVariable> maybeVar = findSuperstructVariable(functionName, objectName);
        if (maybeVar.isEmpty()) {
            Main.logger.printDebug("\tVariable is not superstruct");
            Main.logger.printDebug("\t\tlocal vars: " + functionVariables.get(functionName));
            return getContextText(ctx, tokens);
        }
        final SuperstructVariable var = maybeVar.get();

        final SuperStructRepre superstruct = getSuperStructFromVariable(ctx, var);

        final boolean hasLeftParen = !ctx.LeftParen().isEmpty();
        final boolean hasRightParen = !ctx.RightParen().isEmpty();
        assert hasLeftParen == hasRightParen;
        if (!hasLeftParen) {
            Main.logger.printDebug("\tNo parentheses");
            return getFieldAccessString(ctx, functionName, superstruct);
        }

        final String methodName = ctx.Identifier(0).getText();
        final Optional<FunctionDefinition> maybeMethod = findMethod(superstruct, methodName);

        if (maybeMethod.isEmpty()) {
            Main.logger.printDebug("Variable does not have such a method");
            if (superstruct.members()
                    .stream()
                    .filter(mem -> mem.data().getLeft().isPresent())
                    .map(mem -> mem.data().getLeft().get())
                    .noneMatch(decl -> decl.getData().contains(methodName))) {
                throw new SSCSyntaxException(
                        "superstruct '" + superstruct.name() + "' has no members called `" + methodName + "`",
                        ctx, tokens);
            }
            return getContextText(ctx, tokens);
        }
        final FunctionDefinition method = maybeMethod.get();

        if (var.pointer() && arrowOrDot == ArrowOrDot.Dot) {
            throw new SSCSyntaxException("Pointer to superstruct must be accessed with `->`", ctx, tokens);
        }

        if (method.isPrivate()) {
            Main.logger.printDebug("Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (notInSuperstructMethod(functionName)) {
                throw new SSCSyntaxException(
                        "Cannot access private method `" + methodName + "` from outside the superstruct", ctx, tokens
                );
            }
        }

        final String ssName = superstruct.name();

        final StringBuilder finalExpression =
                new StringBuilder(ssName)
                        .append("__")
                        .append(methodName)
                        .append("(");

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
                .append(")");

        Main.logger.printDebug("\tFinal Expression: " + finalExpression);
        return finalExpression.toString();
    }

    private Optional<SuperstructVariable> findSuperstructVariable(String functionName, String objectName) {
        for (SuperstructVariable var : functionVariables.get(functionName)) {
            if (var.name().equals(objectName)) {
                return Optional.of(var);
            }
        }
        return Optional.empty();
    }

    /**
     * Always returns a valid superstruct.
     * If one is not found -> throw.
     *
     * @param ctx postfix expression
     * @param var local variable
     * @return valid ss
     */
    private SuperStructRepre getSuperStructFromVariable(final SSCParser.PostfixExpressionContext ctx,
                                                        final SuperstructVariable var) {
        final Optional<SuperStructRepre> optSS = findSuperStructFromVariable(var);
        if (optSS.isEmpty()) {
            throw new SSCSyntaxException(
                    "`superstruct " + var.type() + "` "
                            + "(type of variable \"" + var.name() + "\") is not properly defined",
                    ctx, tokens
            );
        }
        return optSS.get();
    }

    /**
     * Tries finding a valid superstruct
     *
     * @param var local variable
     * @return empty if no ss matches
     */
    private Optional<SuperStructRepre> findSuperStructFromVariable(final SuperstructVariable var) {
        for (SuperStructRepre candidate : superstructs) {
            if (candidate.name().equals(var.type())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String getFieldAccessString(SSCParser.PostfixExpressionContext ctx, String functionName, SuperStructRepre superstruct) {
        final String fieldName = ctx.Identifier(0).getText();

        final var allMatching = superstruct.members()
                .stream()
                .map(SSMember::data)
                .filter(either -> either.getLeft().isPresent())
                .map(either -> either.getLeft().get())
                .filter(field -> field.getData().matches(".*?" + fieldName + "\\s*;"))
                .toList();
        if (allMatching.size() > 1) {
            throw new SSCSyntaxException(
                    "Found more than one matching field in superstruct `" + superstruct.name() + "`", ctx, tokens
            );
        }
        if (allMatching.isEmpty()) {
            throw new SSCSyntaxException(
                    "Found no matching field in superstruct `" + superstruct.name() + "`", ctx, tokens
            );
        }

        final Field field = allMatching.get(0);

        if (field.isPrivate()) {
            Main.logger.printDebug("Field `" + fieldName + "` is private. Going to check if it may be used here...");
            if (notInSuperstructMethod(functionName)) {
                throw new SSCSyntaxException(
                        "Cannot access private field `" + fieldName + "` from outside the superstruct", ctx, tokens
                );
            }
        }

        return getContextText(ctx, tokens);
    }

    private boolean notInSuperstructMethod(final String functionName) {
        /* TODO (?) look if methods was defined inside the ss
         *  (currently checks namespace only)
         */
        Main.logger.printDebug("Looking for function: " + functionName);
        for (SuperStructRepre ssr : superstructs) {
            Main.logger.printDebug("\tin superstruct: " + ssr.name());
            if (functionName.startsWith(ssr.name() + "__")) {
                Main.logger.printDebug("... found");
                return false;
            }
            Main.logger.printDebug("... not found");
        }

        return true;
    }

    private Optional<FunctionDefinition> findMethod(final SuperStructRepre ssr,
                                                    final String methodName) {
        for (final FunctionDefinition func : ssr.members().stream()
                .map(m -> m.data().getRight())
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList()) {
            if (func.getName().equals(methodName)) {
                return Optional.of(func);
            }
        }

        return Optional.empty();
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

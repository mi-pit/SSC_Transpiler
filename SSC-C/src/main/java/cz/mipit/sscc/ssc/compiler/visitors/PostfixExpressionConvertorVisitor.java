package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.Field;
import cz.mipit.sscc.ssc.compiler.data.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.SSMember;
import cz.mipit.sscc.ssc.compiler.data.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.SuperstructVariable;
import cz.mipit.sscc.ssc.exceptions.SSCSyntaxException;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static cz.mipit.sscc.util.ContextText.getLiteral;

public class PostfixExpressionConvertorVisitor extends SSCConvertorVisitor {
    private final Collection<SuperStruct> superstructs;

    public final Map<String /* Function name */, Set<SuperstructVariable>> functionVariables = new HashMap<>();

    public PostfixExpressionConvertorVisitor(CommonTokenStream tokens,
                                             Collection<SuperStruct> sss) {
        super(tokens);
        this.superstructs = sss;

        functionVariables.put(null /* Global variables */, new HashSet<>());
    }

    private String currentFunctionName = null; /* null => no function => global */

    @Override
    public String visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
        // Set currentFunctionName
        assert ctx.compoundStatement() != null;

        if (ctx.declarationList() != null) {
            throw new SSCSyntaxException("K&R C-style declarations are invalid in SSC", ctx, tokens);
        }

        currentFunctionName = FunctionDefinition.parseName(ctx.declarator(), tokens);
        functionVariables.put(currentFunctionName, new HashSet<>());

        getSuperstructArgs(ctx);

        final String ret = super.visitFunctionDefinition(ctx);
        currentFunctionName = null;
        return ret;
    }

    private void getSuperstructArgs(final SSCParser.FunctionDefinitionContext ctx) {
        final SSCParser.ParameterTypeListContext paramTypeList =
                ctx.declarator().directDeclarator().parameterTypeList();
        if (paramTypeList == null) {
            throw new SSCSyntaxException("Invalid function definition", ctx, tokens);
        }

        final List<SSCParser.ParameterDeclarationContext> paramList =
                paramTypeList.parameterList().parameterDeclaration();

        for (final SSCParser.ParameterDeclarationContext paramDecl : paramList) {
            if (paramDecl.declarationSpecifiers2() != null) {
                assert paramDecl.abstractDeclarator() != null;
                continue;
            }
            final var declarator = paramDecl.declarator();

            final Optional<String> maybeSSName = findSSNameInDeclSpecs(
                    paramDecl.declarationSpecifiers().declarationSpecifier()
            );
            if (maybeSSName.isEmpty()) {
                continue;
            }
            final String ssName = maybeSSName.get();

            tryGetSuperstructVariableFromDeclarator(ssName, declarator)
                    .ifPresent(ssVar -> functionVariables.get(currentFunctionName).add(ssVar));
        }
    }

    @Override
    public String visitDeclaration(SSCParser.DeclarationContext ctx) {
        collectSuperstructVariablesFromDeclaration(ctx);
        return super.visitDeclaration(ctx);
    }

    private void collectSuperstructVariablesFromDeclaration(
            final SSCParser.DeclarationContext ctx
    ) {
        if (ctx.staticAssertDeclaration() != null) {
            assert ctx.declarationSpecifiers() == null;
            return;
        }
        assert ctx.declarationSpecifiers() != null;
        assert !ctx.declarationSpecifiers().declarationSpecifier().isEmpty();

        if (ctx.initDeclaratorList() == null) {
            // e.g. `struct s;`
            return;
        }

        final List<SSCParser.DeclarationSpecifierContext> declSpecs = ctx.declarationSpecifiers().declarationSpecifier();
        final Optional<String> maybeSSName = findSSNameInDeclSpecs(declSpecs);
        if (maybeSSName.isEmpty()) {
            return;
        }
        final String ssName = maybeSSName.get();

        final List<SSCParser.InitDeclaratorContext> initDeclList = ctx.initDeclaratorList().initDeclarator();

        for (final SSCParser.InitDeclaratorContext initDeclarator : initDeclList) {
            final var declarator = initDeclarator.declarator();

            tryGetSuperstructVariableFromDeclarator(ssName, declarator)
                    .ifPresent(ssVar -> functionVariables.get(currentFunctionName).add(ssVar));
        }
    }

    private Optional<SuperstructVariable> tryGetSuperstructVariableFromDeclarator(
            final String ssName,
            final SSCParser.DeclaratorContext declarator
    ) {
        final var directDecl = declarator.directDeclarator();
        if (directDecl.Identifier() == null) {
            return Optional.empty();
        }

        final boolean pointer = declarator.pointer() != null;
        final String varName = directDecl.Identifier().getText();

        final SuperstructVariable ssVar = new SuperstructVariable(ssName, pointer, varName);
        return Optional.of(ssVar);
    }

    private Optional<String> findSSNameInDeclSpecs(
            final List<SSCParser.DeclarationSpecifierContext> declSpecs
    ) {
        for (final var declSpec : declSpecs) {
            if (declSpec.typeSpecifier() == null) {
                continue;
            }

            final var typeSpec = declSpec.typeSpecifier();
            if (typeSpec.superStructSpecifier() == null) {
                continue;
            }

            final var ssCtx = typeSpec.superStructSpecifier();
            final String ssName = ssCtx.Identifier().getText();
            return Optional.of(ssName);
        }

        return Optional.empty();
    }

    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
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
                final String res = getLiteral(ctx, tokens)
                        .replaceFirst("\\(\\s*superstruct\\s+", "( struct ");
                Main.logger.printDebug("superStructSpecifier in: " + getLiteral(ctx, tokens).replace("\n", " ")
                        + "\n\t\tReturning: " + res.replace("\n", " "));
                return Optional.of(res);
            }
        } catch (NullPointerException ignored) {
        }
        return Optional.empty();
    }

    public String convertStaticFunctionCall(final SSCParser.PostfixExpressionContext ctx,
                                            final String ctxFunctionName) {
        Main.logger.printDebug("Double colon in: %s\n", getLiteral(ctx, tokens));

        if (ctx.primaryExpression() == null)
            throw new SSCSyntaxException("Double colon expression has no left side (Superstruct name) expression", ctx, tokens);
        final String className = getLiteral(ctx.primaryExpression(), tokens);

        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException("Double colon expression has no right side (function) expression", ctx, tokens);
        final String methodName = ctx.Identifier().get(0).toString();

        verifyStaticCall(ctx, ctxFunctionName, className, methodName);

        final String namespacedMethodName = className + "__" + methodName;

        final boolean noCall = ctx.LeftParen().isEmpty();
        if (noCall) {
            return namespacedMethodName;
        }

        final List<String> args = new ArrayList<>();
        for (SSCParser.ArgumentExpressionListContext argListCtx : ctx.argumentExpressionList()) {
            for (SSCParser.AssignmentExpressionContext assExprCtx : argListCtx.assignmentExpression()) {
                args.add(visit(assExprCtx));
            }
        }

        final String result = namespacedMethodName + "( " + String.join(", ", args) + " )";
        Main.logger.printDebug("\tResult: " + result);
        return result;
    }

    private void verifyStaticCall(final SSCParser.PostfixExpressionContext ctx,
                                  final String ctxFunctionName,
                                  final String className,
                                  final String methodName) {
        final Optional<SuperStruct> maybeSS = findSuperstructByName(className);
        if (maybeSS.isEmpty()) {
            throw new SSCSyntaxException("Could not find superstruct with name `" + className + "`", ctx, tokens);
        }
        final SuperStruct superstruct = maybeSS.get();

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
                                                                      final SuperStruct ss) {
        for (SSMember mem : ss.members()) {
            final Optional<FunctionDefinition> maybeFunc = mem.data().getRight();
            if (maybeFunc.isPresent()
                    && methodName.equals(maybeFunc.get().getName())) {
                return maybeFunc;
            }
        }
        return Optional.empty();
    }

    private Optional<SuperStruct> findSuperstructByName(final String className) {
        for (SuperStruct s : superstructs) {
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

        Main.logger.printDebug(arrowOrDot + " in: " + getLiteral(ctx, tokens));
        assert arrowOrDot != ArrowOrDot.Neither;

        final String objectName = getLiteral(ctx.primaryExpression(), tokens);
        if (ctx.Identifier().isEmpty())
            throw new SSCSyntaxException(arrowOrDot + " expression has no right side expression", ctx, tokens);

        final Optional<SuperstructVariable> maybeVar = findSuperstructVariable(functionName, objectName);
        if (maybeVar.isEmpty()) {
            Main.logger.printDebug("\tVariable is not superstruct");
            Main.logger.printDebug("\t\tlocal vars: " + functionVariables.get(functionName));
            return getLiteral(ctx, tokens);
        }
        final SuperstructVariable var = maybeVar.get();

        final SuperStruct superstruct = getSuperStructFromVariable(ctx, var);

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
            return getLiteral(ctx, tokens);
        }
        final FunctionDefinition method = maybeMethod.get();

        if (var.isPointer() && arrowOrDot == ArrowOrDot.Dot) {
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
            if (var.getName().equals(objectName)) {
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
    private SuperStruct getSuperStructFromVariable(final SSCParser.PostfixExpressionContext ctx,
                                                   final SuperstructVariable var) {
        final Optional<SuperStruct> optSS = findSuperStructFromVariable(var);
        if (optSS.isEmpty()) {
            throw new SSCSyntaxException(
                    "`superstruct " + var.ssName() + "` "
                            + "(type of variable \"" + var.getName() + "\") is not properly defined",
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
    private Optional<SuperStruct> findSuperStructFromVariable(final SuperstructVariable var) {
        for (SuperStruct candidate : superstructs) {
            if (candidate.name().equals(var.ssName())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String getFieldAccessString(SSCParser.PostfixExpressionContext ctx, String functionName, SuperStruct superstruct) {
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

        return getLiteral(ctx, tokens);
    }

    private boolean notInSuperstructMethod(final String functionName) {
        /* TODO (?) look if methods was defined inside the ss
         *  (currently checks namespace only)
         */
        Main.logger.printDebug("Looking for function: " + functionName);
        for (SuperStruct ssr : superstructs) {
            Main.logger.printDebug("\tin superstruct: " + ssr.name());
            if (functionName.startsWith(ssr.name() + "__")) {
                Main.logger.printDebug("... found");
                return false;
            }
            Main.logger.printDebug("... not found");
        }

        return true;
    }

    private Optional<FunctionDefinition> findMethod(final SuperStruct ssr,
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

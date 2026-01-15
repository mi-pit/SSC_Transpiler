package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.macro.Macro;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.ss.SSMember;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructVariable;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static cz.mipit.sscc.util.SSCCUtil.Text.getLiteral;
import static java.lang.System.lineSeparator;

public class PostfixExpressionConvertorVisitor extends SSCConvertorVisitor {
    private final Set<SuperStruct> superstructs;

    private final Map<String /* Macro name */, Macro> macros;
    public final Map<@Nullable String /* Function name */, Set<SuperstructVariable>> functionVariables;

    public PostfixExpressionConvertorVisitor(final CommonTokenStream tokens,
                                             final Set<SuperStruct> sss,
                                             final Map<String, Macro> macros,
                                             final InputFile currentFile) {
        super(tokens, currentFile);
        this.superstructs = Collections.unmodifiableSet(sss);
        this.macros = Collections.unmodifiableMap(macros);

        functionVariables = new HashMap<>();
        functionVariables.put(null /* Global variables */, new HashSet<>());
    }

    private @Nullable String currentFunctionName = null; /* null => no function => global */

    @Override
    public String visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
        // Set currentFunctionName
        assert ctx.compoundStatement() != null;

        if (ctx.declarationList() != null) {
            throw getSSCSyntaxException("K&R C-style declarations are invalid in SSC", ctx);
        }

        currentFunctionName = FunctionDefinition.parseName(ctx.declarator(), tokens, currentFile);
        functionVariables.put(currentFunctionName, new HashSet<>());

        getFunctionSuperstructArgs(ctx);

        final String ret = super.visitFunctionDefinition(ctx);
        currentFunctionName = null;
        return ret;
    }

    private void getFunctionSuperstructArgs(final SSCParser.FunctionDefinitionContext ctx) {
        final SSCParser.ParameterTypeListContext paramTypeList =
                ctx.declarator().directDeclarator().parameterTypeList();
        if (paramTypeList == null) {
            throw getSSCSyntaxException("Invalid function definition", ctx);
        }

        final List<SSCParser.ParameterDeclarationContext> paramList =
                paramTypeList.parameterList().parameterDeclaration();

        for (final SSCParser.ParameterDeclarationContext paramDecl : paramList) {
            if (paramDecl.declarationSpecifiers2() != null) {
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

        final Optional<Macro> maybeMacro = getMacro(ctx);
        if (maybeMacro.isPresent()) {
            return replaceMacro(ctx, maybeMacro.get());
        }

        Optional<SSCParser.FunctionDefinitionContext> parent = getFunctionDefinitionParent(ctx);
        if (parent.isEmpty()) {
            return super.visitPostfixExpression(ctx);
        }
        final SSCParser.FunctionDefinitionContext funcCtx = parent.get();
        final String functionName = FunctionDefinition.parseName(funcCtx.declarator(), tokens, currentFile);

        if (!ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty()) {
            return convertMethodCall(ctx, functionName);
        }
        if (!ctx.DoubleColon().isEmpty()) {
            return convertStaticFunctionCall(ctx, functionName);
        }
        return super.visitPostfixExpression(ctx);
    }

    private Optional<Macro> getMacro(SSCParser.PostfixExpressionContext ctx) {
        if (ctx.LeftParen().isEmpty()) {
            return Optional.empty();
        }
        if (ctx.primaryExpression() == null) {
            return Optional.empty();
        }
        /* Macro invocation must start with an Identifier */
        if (ctx.primaryExpression().Identifier() == null) {
            return Optional.empty();
        }

        final String possibleMacroName = ctx.primaryExpression().Identifier().getText();

        /* assume function call if macro is null */
        return Optional.ofNullable(macros.get(possibleMacroName));
    }

    private String replaceMacro(SSCParser.PostfixExpressionContext ctx, Macro macro) {
        System.out.println("In PostfixExpressionConvertorVisitor.replaceMacro");
        System.out.println("ctx = `" + SSCCUtil.Text.getLiteral(ctx, tokens) + "`");
        System.out.println("macro = " + macro);

        final SSCParser.ArgumentExpressionListContext argsExprList = ctx.argumentExpressionList(0);
        final List<String> args = argsExprList.assignmentExpression().stream()
                .map(s -> SSCCUtil.Text.getLiteral(s, tokens))
                .toList();
        System.out.println("args = " + args);
        final String replacement = macro.replace(args);
        System.out.println("candidate replacement: `" + replacement + "`");
        return replacement;
    }

    private static Optional<SSCParser.FunctionDefinitionContext> getFunctionDefinitionParent(
            final SSCParser.PostfixExpressionContext ctx
    ) {
        ParserRuleContext parent = ctx;
        while (parent != null) {
            if (parent instanceof SSCParser.FunctionDefinitionContext func) {
                return Optional.of(func);
            }
            parent = parent.getParent();
        }
        return Optional.empty();
    }

    private Optional<String> getCompoundLiteralReplaced(SSCParser.PostfixExpressionContext ctx) {
        /* TODO */
//        if (ctx.typeName() == null) {
//            return Optional.empty();
//        }
//        final var typeName = ctx.typeName();
//        final var initializerList = ctx.initializerList();
//        final var specifierQualifierList = typeName.specifierQualifierList();
//
//        final List<String> typeSpecs = new ArrayList<>();
//        final List<String> typeQuals = new ArrayList<>();
//        for (var specOrQual = specifierQualifierList;
//             specOrQual.specifierQualifierList() != null;
//             specOrQual = specOrQual.specifierQualifierList()) {
//
//            if (specOrQual.typeSpecifier() != null) {
//                final SSCParser.TypeSpecifierContext typeSpec = specOrQual.typeSpecifier();
//                if (typeSpec.superStructSpecifier() != null) {
//                    typeSpecs.add("struct");
//                } else {
//                    typeSpecs.add(typeSpec.getText());
//                }
//            } else {
//                assert specOrQual.typeQualifier() != null;
//                typeQuals.add(specOrQual.typeQualifier().getText());
//            }
//        }

        try {
            var ss = ctx.typeName().specifierQualifierList().typeSpecifier().superStructSpecifier();
            if (ss != null) {
                final String res = getLiteral(ctx, tokens)
                        .replaceFirst("\\(\\s*superstruct\\s+", "( struct ");
                Main.logger.printDebug("superStructSpecifier in: "
                        + getLiteral(ctx, tokens).replace(lineSeparator(), " ")
                        + lineSeparator() + "\t\tReturning: " + res.replace(lineSeparator(), " "));
                return Optional.of(res);
            }
        } catch (NullPointerException ignored) {
        }
        return Optional.empty();
    }

    public String convertStaticFunctionCall(final SSCParser.PostfixExpressionContext ctx,
                                            final String ctxFunctionName) {
        Main.logger.printDebug("Double colon in: %s", getLiteral(ctx, tokens));

        if (ctx.primaryExpression() == null) {
            throw getSSCSyntaxException("Double colon expression has no left side (Superstruct name) expression", ctx);
        }
        final String className = getLiteral(ctx.primaryExpression(), tokens);

        if (ctx.Identifier().isEmpty()) {
            throw getSSCSyntaxException("Double colon expression has no right side (function) expression", ctx);
        }
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
            throw getSSCSyntaxException("Could not find superstruct with name `" + className + "`", ctx);
        }
        final SuperStruct superstruct = maybeSS.get();

        final Optional<FunctionDefinition> maybeMethod = findSuperstructMethod(methodName, superstruct);
        if (maybeMethod.isEmpty()) {
            throw getSSCSyntaxException(
                    "Superstruct with name `" + className
                            + "` has no method called `" + methodName
                            + "`", ctx);
        }
        final FunctionDefinition method = maybeMethod.get();

        if (method.isPrivate()) {
            Main.logger.printDebug("Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (notInSuperstructMethod(ctxFunctionName)) {
                throw getSSCSyntaxException(
                        "Cannot access private static method `" + methodName + "` from outside the superstruct", ctx
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
            throw getSSCSyntaxException(arrowOrDot + " expression has no right side expression", ctx);

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
                    .noneMatch(decl -> decl.getName().equals(methodName))) {
                throw getSSCSyntaxException(
                        "superstruct '" + superstruct.name() + "' has no members called `" + methodName + "`",
                        ctx);
            }
            return getLiteral(ctx, tokens);
        }
        final FunctionDefinition method = maybeMethod.get();

        if (var.isPointer() && arrowOrDot == ArrowOrDot.Dot) {
            throw getSSCSyntaxException("Pointer to superstruct must be accessed with `->`", ctx);
        }

        if (method.isPrivate()) {
            Main.logger.printDebug("Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (notInSuperstructMethod(functionName)) {
                throw getSSCSyntaxException(
                        "Cannot access private method `" + methodName + "` from outside the superstruct", ctx);
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
        /* check global variables too */
        for (SuperstructVariable var : functionVariables.get(null)) {
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
            throw getSSCSyntaxException(
                    "`superstruct " + var.ssName() + "` "
                            + "(type of variable \"" + var.getName() + "\") is not properly defined",
                    ctx
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

        final Field field = allMatching.get(0);

        if (field.isPrivate()) {
            Main.logger.printDebug("Field `" + fieldName + "` is private. Going to check if it may be used here...");
            if (notInSuperstructMethod(functionName)) {
                throw getSSCSyntaxException(
                        "Cannot access private field `" + fieldName + "` from outside the superstruct", ctx
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
        if (node.getSymbol().getType() == SSCParser.Superstruct) {
            return "struct ";
        }
        return super.visitTerminal(node);
    }
}

package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.ss.SSMember;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.TypedVariable;
import cz.mipit.sscc.util.Either;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;


public class SuperstructConvertorVisitor extends SSCConvertorVisitor {
    private final Map<String, SuperStruct> superStructs;
    private SuperStruct currentSS = null;

    public final Map<@Nullable String /* Function name */, Set<SuperstructVariable>> functionVariables;
    public String currentFunctionName = null; /* null => no function => global */

    public SuperstructConvertorVisitor(CommonTokenStream tokens, InputFile currentFile) {
        super(tokens, currentFile);
        superStructs = new HashMap<>();
        functionVariables = new HashMap<>();
        functionVariables.put(null /* Global variables */, new HashSet<>());
    }

    @Override
    public String visitSuperStructSpecifier(SSCParser.SuperStructSpecifierContext ctx) {
        final String thisSSName = ctx.Identifier().getText();

        if (ctx.superStructBody() == null) {
            // Usage in expression (e.g. `sizeof( superstruct )`)
            return "struct " + thisSSName;
        }

        if (superStructs.containsKey(thisSSName)) {
            throw getSSCSyntaxException("Superstruct with name " + thisSSName + " already exists", ctx);
        }

        final SuperStruct superStruct = this.currentSS = new SuperStruct(thisSSName);
        superStructs.put(thisSSName, currentSS);

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            processMemberCtx(memberCtx, thisSSName);
        }

        currentSS = null;

        return superStruct.convert();
    }

    private void processMemberCtx(final SSCParser.SuperStructMemberContext memberCtx,
                                  final String thisSSName) {
        final var declSpecsCtx = (memberCtx.functionDefinition() != null
                ? memberCtx.functionDefinition().declarationSpecifiers()
                : memberCtx.declaration().declarationSpecifiers());
        final var declSpecs = declSpecsCtx.declarationSpecifier();

        final boolean isPrivate = declSpecs
                .stream()
                .anyMatch(ds -> ds.functionSpecifier() != null
                        && ds.functionSpecifier().Private() != null);

        final List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs = declSpecs
                .stream()
                .filter(declSpec -> declSpec.functionSpecifier() == null
                        || declSpec.functionSpecifier().Private() == null)
                .toList();
        if (noPrivateSpecs.isEmpty()) {
            throw getSSCSyntaxException("No type specifier for superstruct member", memberCtx);
        }

        if (memberCtx.functionDefinition() != null) {
            processMemberFunction(memberCtx.functionDefinition(), thisSSName, declSpecs, noPrivateSpecs, isPrivate);
        } else {
            assert memberCtx.declaration() != null;
            processMemberField(memberCtx.declaration(), noPrivateSpecs, isPrivate);
        }
    }

    private void processMemberField(SSCParser.DeclarationContext memberCtx,
                                    List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs,
                                    boolean isPrivate) {
        final SSCParser.InitDeclaratorListContext initDeclaratorList =
                memberCtx.initDeclaratorList();

        final List<String> type = noPrivateSpecs
                .stream()
                .map(this::visitDeclarationSpecifier)
                .toList();

        if (initDeclaratorList.initDeclarator().isEmpty()) {
            throw getSSCSyntaxException(
                    "Init declarator empty `" + SSCCUtil.Text.getLiteral(memberCtx, tokens) + "`",
                    initDeclaratorList
            );
        }

        for (SSCParser.InitDeclaratorContext initDecl : initDeclaratorList.initDeclarator()) {
            if (initDecl.initializer() != null) {
                throw getSSCSyntaxException(
                        "Cannot initialize superstruct field (must use a constructor)",
                        initDecl.initializer()
                );
            }
            final SSCParser.DeclaratorContext declarator = initDecl.declarator();
            if (declarator.directDeclarator().Identifier() == null) {
                throw getSSCSyntaxException(
                        "Field has no identifier",
                        declarator.directDeclarator()
                );
            }

            final int ptrs;
            if (declarator.pointer().isEmpty()) {
                ptrs = 0;
            } else {
                assert declarator.pointer().size() == 1;
                ptrs = declarator.pointer().getFirst().Star().size();
            }
            final String name = declarator.directDeclarator().Identifier().getText();

            final Field field = new Field(isPrivate, new TypedVariable(type, ptrs, name));
            currentSS.addMember(SSMember.field(field));
        }
    }

    private void processMemberFunction(SSCParser.FunctionDefinitionContext functionCtx,
                                       String thisSSName,
                                       List<SSCParser.DeclarationSpecifierContext> declSpecs,
                                       List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs,
                                       boolean isPrivate) {
        assert functionCtx != null;
        assert currentSS != null : "Member of no struct";

        final boolean isStatic = hasDeclarationSpecifier(declSpecs, ds ->
                ds.storageClassSpecifier() != null && ds.storageClassSpecifier().Static() != null);
        final boolean isPure = hasDeclarationSpecifier(declSpecs, ds ->
                ds.functionSpecifier() != null && ds.functionSpecifier().Pure() != null);

        final List<String> withoutCustom = getDeclSpecsWithoutCustom(noPrivateSpecs);

        var declarator = functionCtx.declarator();
        var directDecl = declarator.directDeclarator();
        if (directDecl == null) {
            throw getSSCSyntaxException("Direct declarator is null", functionCtx);
        }

        final String unqualifiedName;
        if (directDecl.Identifier() != null) {
            unqualifiedName = directDecl.Identifier().getText();
        } else if (directDecl.LeftParen() == null || directDecl.RightParen() == null) {
            Main.logger.printDebug("No declarator parentheses. Trying to parse declarator.");
            unqualifiedName = visitDeclarator(functionCtx.declarator());
        } else {
            throw getSSCSyntaxException("Missing declarator identifier in function definition", directDecl);
        }

        currentFunctionName = currentSS.name() + "__" + unqualifiedName;
        if (functionVariables.put(currentFunctionName, new HashSet<>()) != null) {
            throw getSSCSyntaxException("Duplicate function definition", functionCtx);
        }

        if (!isStatic) {
            final SuperstructVariable selfReferenceVariable =
                    new SuperstructVariable(currentSS.name(), 1, "this");

            functionVariables
                    .get(currentFunctionName)
                    .add(selfReferenceVariable);
        }

        final FunctionDefinition functionDefinition = new FunctionDefinition(
                isStatic,
                isPure,
                isPrivate,
                withoutCustom,
                parseType(declSpecs, declarator),
                unqualifiedName,
                parseFunctionParameters(functionCtx.declarator()),
                this.visitFunctionBody(functionCtx.functionBody()),
                thisSSName
        );

        currentSS.addMember(SSMember.function(functionDefinition));

        currentFunctionName = null;
    }

    public String parseType(List<SSCParser.DeclarationSpecifierContext> declSpecs,
                            SSCParser.DeclaratorContext decl) {
        final List<String> builder = new ArrayList<>();

        for (var spec : declSpecs) {
            if (spec.typeSpecifier() == null) {
                continue;
            }
            final SSCParser.TypeSpecifierContext typeSpec = spec.typeSpecifier();
            if (typeSpec.superStructSpecifier() == null) {
                builder.add(this.visitTypeSpecifier(spec.typeSpecifier()));
                continue;
            }
            final SSCParser.SuperStructSpecifierContext superStructSpec = typeSpec.superStructSpecifier();
            builder.add(this.visitSuperStructSpecifier(superStructSpec));
        }

        if (decl.pointer() != null) {
            builder.add(decl
                    .pointer()
                    .stream()
                    .map(this::visitPointer)
                    .collect(Collectors.joining(""))
            );
        }

        return String.join(" ", builder);
    }

    private List<String> parseFunctionParameters(final SSCParser.DeclaratorContext ctx) {
        final List<String> args = new ArrayList<>();
        final var directDecl = ctx.directDeclarator();
        final List<SSCParser.ParameterTypeListContext> paramTypeList = directDecl.parameterTypeList();
        if (paramTypeList.size() > 1) {
            throw getSSCSyntaxException("Parameter type list has more than one parameter type", directDecl);
        }

        final SSCParser.ParameterTypeListContext paramType = paramTypeList.getFirst();
        for (final var param : paramType.parameterList().parameterDeclaration()) {
            if (param.declarationSpecifiers() == null) {
                /* Function with no parameters */
                break;
            }
            @Nullable String ssName = null;

            final List<String> curr = new ArrayList<>();
            for (final var declSpec : param.declarationSpecifiers().declarationSpecifier()) {
                if (declSpec.typeSpecifier() == null) {
                    curr.add(this.visitDeclarationSpecifier(declSpec));
                    continue;
                }
                final var typeSpecCtx = declSpec.typeSpecifier();
                if (typeSpecCtx.superStructSpecifier() == null) {
                    curr.add(this.visitTypeSpecifier(typeSpecCtx));
                    continue;
                }

                if (ssName != null) {
                    throw getSSCSyntaxException("Duplicate super struct specifier", declSpec);
                }
                final var superStructSpecCtx = typeSpecCtx.superStructSpecifier();
                ssName = superStructSpecCtx.Identifier().getText();
                curr.add("struct " + ssName);
            }
            final var declarator = param.declarator();
            if (declarator != null) {
                final int pointer = SSCCUtil.getPointerLevel(declarator);

                if (declarator.directDeclarator().Identifier() != null) {
                    final String varName = declarator.directDeclarator().Identifier().getText();
                    if (ssName != null) {
                        functionVariables.get(currentFunctionName).add(new SuperstructVariable(ssName, pointer, varName));
                    }
                }

                curr.add(this.visitDeclarator(declarator));
            }

            final String paramStr = String.join(" ", curr);
            if (!paramStr.isBlank()) {
                args.add(paramStr);
            }
        }

        if (paramType.Ellipsis() != null) {
            if (args.isEmpty()) {
                throw getSSCSyntaxException("Variable arguments list requires at least one parameter", directDecl);
            }

            args.add("...");
        }
        if (args.isEmpty()) {
            args.add("void");
        }

        return args;
    }

    private static boolean hasDeclarationSpecifier(List<SSCParser.DeclarationSpecifierContext> declSpecs,
                                                   Predicate<SSCParser.DeclarationSpecifierContext> matcher) {
        for (SSCParser.DeclarationSpecifierContext declSpec : declSpecs) {
            if (matcher.test(declSpec)) {
                return true;
            }
        }
        return false;
    }

    /**
     * filter out types & {@code pure} and {@code static}
     */
    private List<String> getDeclSpecsWithoutCustom(List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs) {
        final List<String> withoutCustom = new ArrayList<>();
        for (SSCParser.DeclarationSpecifierContext declSpec : noPrivateSpecs) {
            if (declSpec.typeSpecifier() != null) {
                continue;
            }

            if ((declSpec.functionSpecifier() == null || declSpec.functionSpecifier().Pure() == null)
                    && (declSpec.storageClassSpecifier() == null || declSpec.storageClassSpecifier().Static() == null)) {
                withoutCustom.add(this.visitDeclarationSpecifier(declSpec));
            }
        }
        return withoutCustom;
    }

    /* ExpressionConvertorVisitor */

    @Override
    public String visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
        // Set currentFunctionName
        assert ctx.functionBody().compoundStatement() != null;

        if (ctx.declarationList() != null) {
            throw getSSCSyntaxException("K&R C-style declarations are invalid in SSC", ctx.declarationList());
        }

        final String unqualifiedName = ctx.declarator().directDeclarator().Identifier().getText();
        currentFunctionName = currentSS == null
                ? unqualifiedName
                : currentSS.name() + "__" + unqualifiedName;
        if (functionVariables.put(currentFunctionName, new HashSet<>()) != null) {
            throw getSSCSyntaxException("Duplicate function definition", ctx);
        }

        getFunctionSuperstructArgs(ctx);

        final String ret = super.visitFunctionDefinition(ctx);
        currentFunctionName = null;
        return ret;
    }

    private void getFunctionSuperstructArgs(final SSCParser.FunctionDefinitionContext ctx) {
        final List<SSCParser.ParameterTypeListContext> ls = ctx.declarator().directDeclarator().parameterTypeList();
        if (ls.isEmpty()) {
            throw getSSCSyntaxException("Function definition has no parameter type list", ctx.declarator());
        }
        final SSCParser.ParameterTypeListContext paramTypeList = ls.getFirst();
        if (paramTypeList == null) {
            throw getSSCSyntaxException("Function definition has no parameter type list", ctx.declarator());
        }

        final List<SSCParser.ParameterDeclarationContext> paramList =
                paramTypeList.parameterList().parameterDeclaration();

        for (final SSCParser.ParameterDeclarationContext paramDecl : paramList) {
            final var declarator = paramDecl.declarator();
            if (paramDecl.declarationSpecifiers() == null) {
                /* no parameters */
                break;
            }

            final Optional<String> maybeSSName = findSSNameInDeclSpecs(
                    paramDecl.declarationSpecifiers().declarationSpecifier()
            );
            if (maybeSSName.isEmpty()) {
                continue;
            }
            final String ssName = maybeSSName.get();

            tryCreateSuperstructVariableFromDeclarator(ssName, declarator)
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

            final Optional<SuperstructVariable> maybeSSVar =
                    tryCreateSuperstructVariableFromDeclarator(ssName, declarator);

            if (maybeSSVar.isEmpty()) {
                continue;
            }

            functionVariables.get(currentFunctionName).add(maybeSSVar.get());
        }
    }

    private Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final String ssName,
            final SSCParser.DeclaratorContext declarator
    ) {
        if (declarator == null) {
            return Optional.empty();
        }
        final var directDecl = declarator.directDeclarator();
        if (directDecl.Identifier() == null) {
            return Optional.empty();
        }

        final int pointer = SSCCUtil.getPointerLevel(declarator);
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

        if (!ctx.Arrow().isEmpty() || !ctx.Dot().isEmpty()) {
            return convertMethodCall(ctx);
        }
        if (!ctx.DoubleColon().isEmpty()) {
            return convertStaticFunctionCall(ctx);
        }
        return super.visitPostfixExpression(ctx);
    }

    private Optional<String> getCompoundLiteralReplaced(SSCParser.PostfixExpressionContext ctx) {
        /* postfixExpression.typeName implies compound literal */
        if (ctx.typeName() == null
                || ctx.typeName().specifierQualifierList() == null
                || ctx.typeName().specifierQualifierList().typeSpecifierQualifier().isEmpty()
                || ctx.typeName().specifierQualifierList().typeSpecifierQualifier(0).typeSpecifier() == null
                || ctx.typeName().specifierQualifierList().typeSpecifierQualifier(0).typeSpecifier().superStructSpecifier() == null
        ) {
            return Optional.empty();
        }

        final String res = super.visitPostfixExpression(ctx);

        Main.logger.printDebug(() -> "superStructSpecifier in: "
                + SSCCUtil.Text.getLiteral(ctx, tokens).replace(lineSeparator(), " ")
                + lineSeparator() + "\t\tReturning: " + res.replace(lineSeparator(), " "));

        return Optional.of(res);
    }

    public String convertStaticFunctionCall(final SSCParser.PostfixExpressionContext ctx) {
        Main.logger.printDebug(() -> "Double colon in: " + SSCCUtil.Text.getLiteral(ctx, tokens));

        if (ctx.primaryExpression() == null) {
            throw getSSCSyntaxException("Double colon expression has no left side (Superstruct name) expression", ctx);
        }
        final String className = visitPrimaryExpression(ctx.primaryExpression());

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
                args.add(visitAssignmentExpression(assExprCtx));
            }
        }

        final String result = namespacedMethodName + "( " + String.join(", ", args) + " )";
        Main.logger.printDebug(() -> "\tResult: " + result);
        return result;
    }

    private void verifyStaticCall(final SSCParser.PostfixExpressionContext ctx,
                                  final String className,
                                  final String methodName) {
        final Optional<SuperStruct> maybeSS = findSuperstructByName(className);
        if (maybeSS.isEmpty()) {
            throw getSSCSyntaxException("Could not find superstruct with name `" + className + "`", ctx);
        }
        final SuperStruct superstruct = maybeSS.get();

        final Optional<FunctionDefinition> maybeMethod = findMethodInSuperstruct(superstruct, methodName);
        if (maybeMethod.isEmpty()) {
            throw getSSCSyntaxException(
                    "Superstruct with name `" + className
                            + "` has no method called `" + methodName
                            + "`", ctx);
        }
        final FunctionDefinition method = maybeMethod.get();

        if (method.isPrivate()) {
            Main.logger.printDebug(() -> "Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (currentSS == null || !currentSS.name().equals(className)) {
                throw getSSCSyntaxException(
                        "Cannot access private static method `" + methodName + "` from outside the superstruct", ctx
                );
            }
        }
    }

    private Optional<SuperStruct> findSuperstructByName(final String className) {
        for (SuperStruct s : superStructs.values()) {
            if (s.name().equals(className)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    public String convertMethodCall(final SSCParser.PostfixExpressionContext ctx) {
        enum ArrowOrDot {Arrow, Dot, Neither}
        final ArrowOrDot arrowOrDot =
                !ctx.Arrow().isEmpty() ? ArrowOrDot.Arrow
                        : !ctx.Dot().isEmpty() ? ArrowOrDot.Dot
                        : ArrowOrDot.Neither;

        Main.logger.printDebug(() -> arrowOrDot + " in: " + SSCCUtil.Text.getLiteral(ctx, tokens));
        assert arrowOrDot != ArrowOrDot.Neither;

        final String objectName = visitPrimaryExpression(ctx.primaryExpression());
        if (ctx.Identifier().isEmpty())
            throw getSSCSyntaxException(arrowOrDot + " expression has no right side expression", ctx);

        final Optional<SuperstructVariable> maybeVar = findSuperstructVariable(currentFunctionName, objectName);
        if (maybeVar.isEmpty()) {
            Main.logger.printDebug(() -> "\tVariable is not superstruct");
            if (currentFunctionName == null) {
                throw getSSCSyntaxException("Accessing superstruct method from global scope", ctx);
            }
            Main.logger.printDebug(() -> "\t\tlocal vars: " + functionVariables.get(currentFunctionName));
            return super.visitPostfixExpression(ctx);
        }
        final SuperstructVariable var = maybeVar.get();

        final SuperStruct superstruct = getSuperStructFromVariable(ctx, var);

        final boolean hasLeftParen = !ctx.LeftParen().isEmpty();
        assert hasLeftParen == !ctx.RightParen().isEmpty();
        if (!hasLeftParen) {
            Main.logger.printDebug(() -> "\tNo parentheses");
            return getFieldAccessString(ctx, superstruct);
        }

        final String methodName = ctx.Identifier(0).getText();
        final Optional<FunctionDefinition> maybeMethod = findMethodInSuperstruct(superstruct, methodName);

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
                return super.visitPostfixExpression(ctx);
            }

            Main.logger.printDebug(() ->
                    "Did not find method `" + methodName + "`. " +
                            "Converting anyway and hoping it gets defined later"
            );
        }

        if (var.pointer() == 1 && arrowOrDot == ArrowOrDot.Dot) {
            throw getSSCSyntaxException("Pointer to superstruct must be accessed with `->`", ctx);
        }

        maybeMethod.ifPresent(functionDefinition -> {
            if (functionDefinition.isPrivate()) {
                Main.logger.printDebug(() -> "Method '" + methodName + "' is private. Going to check if it may be used here...");
                if (currentSS == null || !currentSS.name().equals(superstruct.name())) {
                    throw getSSCSyntaxException(
                            "Cannot access private method `" + methodName + "` from outside the superstruct", ctx);
                }
            }
        });

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

        Main.logger.printDebug(() -> "\tFinal Expression: " + finalExpression);
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
        for (SuperStruct candidate : superStructs.values()) {
            if (candidate.name().equals(var.ssName())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String getFieldAccessString(SSCParser.PostfixExpressionContext ctx,
                                        SuperStruct superstruct) {
        final String fieldName = ctx.Identifier(0).getText();

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

        final String primaryExpression = visitPrimaryExpression(ctx.primaryExpression());
        if (field.isPrivate()) {
            final boolean inSSMethod = findSuperstructVariable(currentFunctionName, primaryExpression).isPresent();
            Main.logger.printDebug(() -> "Field `" + fieldName
                    + "` is private. Going to check if it may be used here...");

            if (!inSSMethod) {
                throw getSSCSyntaxException(
                        "Cannot access private field `" + fieldName + "` from outside the superstruct", ctx
                );
            }
        }

        return super.visitPostfixExpression(ctx);
    }

    private Optional<FunctionDefinition> findMethodInSuperstruct(final SuperStruct ssr,
                                                                 final String methodName) {
        for (final FunctionDefinition func : ssr.getFunctions()) {
            if (func.getUnqualifiedName().equals(methodName)) {
                return Optional.of(func);
            }
        }

        return Optional.empty();
    }

    @Override
    public String visitSscIncludeDirective(SSCParser.SscIncludeDirectiveContext ctx) {
        final String[] s = ctx.SSCDirective().getText().split("<");
        assert s.length == 2 : "preprocessor emitted invalid directive";
        final String directive = lineSeparator() + "#include <" + s[1] + lineSeparator();
        Main.logger.printDebug(() -> "converted directive: " + directive);
        return directive;
    }
}

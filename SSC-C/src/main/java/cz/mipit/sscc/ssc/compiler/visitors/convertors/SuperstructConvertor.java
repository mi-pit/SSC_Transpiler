package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.Function;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.TypedVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SuperstructConvertor extends AbstractConvertor<SSCParser.SuperStructSpecifierContext> {

    private SuperStruct lastSuperstruct;

    public SuperstructConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
        lastSuperstruct = null;
    }

    @Override
    public String convert(SSCParser.SuperStructSpecifierContext ctx) {
        final String thisSSName = dispatcher.visitTerminal(ctx.Identifier());

        if (ctx.superStructBody() == null) {
            return dispatcher.visitSuper(ctx);
        }

        final SuperStruct got = dispatcher.data.superStructs().get(thisSSName);
        // superstructs only have fields if they are defined
        if (got != null && !got.fields().isEmpty()) {
            throw dispatcher.getSSCSyntaxException("Superstruct with name '" + thisSSName + "' already exists", ctx);
        }

        final SuperStruct superStruct = got != null ? got : new SuperStruct(thisSSName);
        dispatcher.data.superStructs().put(thisSSName, superStruct);
        dispatcher.data.setCurrentSS(superStruct);

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            processMemberCtx(dispatcher, memberCtx, thisSSName);
        }

        dispatcher.data.setCurrentSS(null);

        if (lastSuperstruct != null) {
            Main.logger.printDebug("A non-emitted superstruct with name '"
                    + lastSuperstruct.name()
                    + "' is still present while processing superstruct '"
                    + thisSSName + "'"
            );
        }
        lastSuperstruct = superStruct;
        return lastSuperstruct.emitStructDefinition();
    }

    public Optional<String> emitDeclarations() {
        if (lastSuperstruct == null) {
            return Optional.empty();
        }
        return Optional.of(lastSuperstruct.emitMethodDeclarations());
    }

    public Optional<String> emit() {
        if (lastSuperstruct == null) {
            return Optional.empty();
        }
        final String methods = lastSuperstruct.emitMethodDefinitions();
        lastSuperstruct = null;

        return Optional.of(methods);
    }

    public static String qualifySuperstructIdentifier(SuperStruct superStruct, String unqualifiedName) {
        return superStruct.name() + "__" + unqualifiedName;
    }

    public static void processMemberCtx(
            final VisitorDispatcher dispatcher,
            final SSCParser.SuperStructMemberContext memberCtx,
            final String thisSSName
    ) {
        final var declSpecsCtx = (memberCtx.functionDefinition() != null
                ? memberCtx.functionDefinition().functionHeader().declarationSpecifiers()
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
            throw dispatcher.getSSCSyntaxException("No type specifier for superstruct member", memberCtx);
        }

        if (memberCtx.functionDefinition() != null) {
            processMemberFunction(dispatcher, memberCtx.functionDefinition(), thisSSName, declSpecs, noPrivateSpecs, isPrivate);
        } else {
            assert memberCtx.declaration() != null;
            processMemberField(dispatcher, memberCtx.declaration(), noPrivateSpecs, isPrivate);
        }
    }

    private static void processMemberField(
            final VisitorDispatcher dispatcher,
            SSCParser.DeclarationContext memberCtx,
            List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs,
            boolean isPrivate
    ) {
        final SSCParser.InitDeclaratorListContext initDeclaratorList =
                memberCtx.initDeclaratorList();

        final List<String> type = noPrivateSpecs
                .stream()
                .map(dispatcher::visitDeclarationSpecifier)
                .toList();

        if (initDeclaratorList.initDeclarator().isEmpty()) {
            throw dispatcher.getSSCSyntaxException(
                    "Init declarator empty `" + dispatcher.getLiteral(memberCtx) + "`",
                    initDeclaratorList
            );
        }

        for (SSCParser.InitDeclaratorContext initDecl : initDeclaratorList.initDeclarator()) {
            if (initDecl.initializer() != null) {
                throw dispatcher.getSSCSyntaxException(
                        "Cannot initialize superstruct field (must use a constructor)",
                        initDecl.initializer()
                );
            }
            final SSCParser.DeclaratorContext declarator = initDecl.declarator();
            if (declarator.directDeclarator().Identifier() == null) {
                throw dispatcher.getSSCSyntaxException(
                        "Field has no identifier",
                        declarator.directDeclarator()
                );
            }

            final int ptrs = SSCCUtil.getPointerLevel(declarator);
            final String name = dispatcher.visitTerminal(declarator.directDeclarator().Identifier());

            final Field field = new Field(isPrivate, new TypedVariable(type, ptrs, name));
            dispatcher.data.currentSS().ifPresent(ss -> ss.addField(field));
        }
    }

    public static void processMemberFunction(
            final VisitorDispatcher dispatcher,
            SSCParser.FunctionDefinitionContext functionCtx,
            String thisSSName,
            List<SSCParser.DeclarationSpecifierContext> declSpecs,
            List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs,
            boolean isPrivate
    ) {
        final FunctionHeaderData fnData = getFunctionHeaderData(
                dispatcher, functionCtx.functionHeader(),
                declSpecs, noPrivateSpecs
        );

        final String qualified = qualifySuperstructIdentifier(
                fnData.superStruct(),
                fnData.unqualifiedName()
        );
        dispatcher.pushFunction(qualified, functionCtx);

        if (!fnData.isStatic()) {
            final SuperstructVariable selfReferenceVariable =
                    new SuperstructVariable(fnData.superStruct().name(), 1, "this");

            Main.logger.printDebug(() -> "Adding self reference variable '"
                    + selfReferenceVariable
                    + "' to function '"
                    + dispatcher.getCurrentFunctionName()
                    + "'");
            dispatcher.data
                    .functionVariables()
                    .get(dispatcher.getCurrentFunctionName())
                    .add(selfReferenceVariable);
        }

        final List<String> parameters = parseFunctionParameters(dispatcher, fnData.declarator());

        final @Nullable String fnBody = functionCtx.functionBody() == null
                ? null
                : dispatcher.visitFunctionBody(functionCtx.functionBody());

        final Function functionDefinition = new Function(
                fnData,
                isPrivate,
                parseType(dispatcher, declSpecs, fnData.declarator()),
                parameters,
                fnBody,
                thisSSName
        );

        fnData.superStruct().addFunction(functionDefinition);

        dispatcher.popFunction();
    }

    public static FunctionHeaderData getFunctionHeaderData(
            VisitorDispatcher dispatcher,
            SSCParser.FunctionHeaderContext functionCtx,
            List<SSCParser.DeclarationSpecifierContext> declSpecs,
            List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs
    ) {
        assert functionCtx != null;
        assert dispatcher.data.currentSS().isPresent() : "Member of no struct";

        final boolean isStatic = hasDeclarationSpecifier(declSpecs, ds ->
                ds.storageClassSpecifier() != null && ds.storageClassSpecifier().Static() != null);
        final boolean isPure = hasDeclarationSpecifier(declSpecs, ds ->
                ds.functionSpecifier() != null && ds.functionSpecifier().Pure() != null);

        final List<String> withoutCustom = getDeclSpecsWithoutCustom(noPrivateSpecs, dispatcher);

        var declarator = functionCtx.declarator();
        var directDecl = declarator.directDeclarator();
        if (directDecl == null) {
            throw dispatcher.getSSCSyntaxException("Direct declarator is null", functionCtx);
        }

        final String unqualifiedName;
        if (directDecl.Identifier() != null) {
            unqualifiedName = dispatcher.visitTerminal(directDecl.Identifier());
        } else if (directDecl.LeftParen() == null || directDecl.RightParen() == null) {
            Main.logger.printDebug("No declarator parentheses. Trying to parse declarator.");
            unqualifiedName = dispatcher.visitDeclarator(declarator);
        } else {
            throw dispatcher.getSSCSyntaxException("Missing declarator identifier in function definition", directDecl);
        }

        final SuperStruct superStruct = dispatcher.data.currentSS().get();
        return new FunctionHeaderData(isStatic, isPure, withoutCustom, declarator, unqualifiedName, superStruct);
    }

    public static String parseType(
            final VisitorDispatcher dispatcher,
            List<SSCParser.DeclarationSpecifierContext> declSpecs,
            SSCParser.DeclaratorContext decl
    ) {
        final List<String> builder = new ArrayList<>();

        for (var spec : declSpecs) {
            if (spec.typeSpecifier() == null) {
                continue;
            }
            final SSCParser.TypeSpecifierContext typeSpec = spec.typeSpecifier();
            if (typeSpec.superStructSpecifier() == null) {
                builder.add(dispatcher.visitTypeSpecifier(spec.typeSpecifier()));
                continue;
            }
            final SSCParser.SuperStructSpecifierContext superStructSpec = typeSpec.superStructSpecifier();
            builder.add(dispatcher.visitSuperStructSpecifier(superStructSpec));
        }

        if (decl.pointer() != null) {
            builder.add(decl
                    .pointer()
                    .stream()
                    .map(dispatcher::visitPointer)
                    .collect(Collectors.joining(""))
            );
        }

        return String.join(" ", builder);
    }

    public static List<String> parseFunctionParameters(
            final VisitorDispatcher dispatcher,
            final SSCParser.DeclaratorContext ctx
    ) {
        final List<String> ls = new ArrayList<>(ctx.directDeclarator()
                .parameterTypeList()
                .stream()
                .map(SSCParser.ParameterTypeListContext::parameterList)
                .filter(Objects::nonNull)
                .flatMap(pl -> pl.parameterDeclaration().stream())
                .map(paramDeclCtx -> {
                    if (paramDeclCtx.declarationSpecifiers() == null) {
                        return dispatcher.visitParameterDeclaration(paramDeclCtx);
                    }

                    for (SSCParser.DeclarationSpecifierContext declSpec
                            : paramDeclCtx.declarationSpecifiers().declarationSpecifier()) {
                        if (declSpec.typeSpecifier() == null) {
                            continue;
                        }
                        final SSCParser.TypeSpecifierContext typeSpec = declSpec.typeSpecifier();
                        if (typeSpec.superStructSpecifier() != null) {
                            final String ssName = dispatcher.visitTerminal(typeSpec.superStructSpecifier().Identifier());
                            final SuperstructVariable ssVar = new SuperstructVariable(
                                    ssName,
                                    SSCCUtil.getPointerLevel(paramDeclCtx.declarator()),
                                    dispatcher.visitTerminal(paramDeclCtx.declarator().directDeclarator().Identifier())
                            );
                            dispatcher.data
                                    .functionVariables()
                                    .get(dispatcher.getCurrentFunctionName())
                                    .add(ssVar);
                            break;
                        }

                        final String str = dispatcher.visitTypeSpecifier(typeSpec);
                        if (dispatcher.data.superstructTypedefs().containsKey(str)) {
                            break;
                        }
                    }

                    return dispatcher.visitParameterDeclaration(paramDeclCtx);
                })
                .filter(s -> !s.isBlank())
                .toList());

        if (ctx.directDeclarator()
                .parameterTypeList()
                .stream()
                .anyMatch(ptl -> ptl.Ellipsis() != null)) {
            ls.add("...");
        }

        return ls;
    }

    public static boolean hasDeclarationSpecifier(List<SSCParser.DeclarationSpecifierContext> declSpecs,
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
    public static List<String> getDeclSpecsWithoutCustom(
            final List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs,
            final VisitorDispatcher dispatcher
    ) {
        final List<String> withoutCustom = new ArrayList<>();
        for (SSCParser.DeclarationSpecifierContext declSpec : noPrivateSpecs) {
            if (declSpec.typeSpecifier() != null) {
                continue;
            }

            if ((declSpec.functionSpecifier() == null || declSpec.functionSpecifier().Pure() == null)
                    && (declSpec.storageClassSpecifier() == null || declSpec.storageClassSpecifier().Static() == null)) {
                withoutCustom.add(
                        dispatcher.visitDeclarationSpecifier(declSpec)
                );
            }
        }
        return withoutCustom;
    }
}

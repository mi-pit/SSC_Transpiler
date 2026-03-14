package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.FunctionDefinition;
import cz.mipit.sscc.ssc.compiler.data.ss.SSMember;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.TypedVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SuperstructConvertor extends Convertor<SSCParser.SuperStructSpecifierContext> {
    public SuperstructConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.SuperStructSpecifierContext ctx) {
        final String thisSSName = ctx.Identifier().getText();

        if (ctx.superStructBody() == null) {
            return dispatcher.super_visitSuperStructSpecifier(ctx);
        }

        if (dispatcher.data.superStructs().containsKey(thisSSName)) {
            throw getSSCSyntaxException("Superstruct with name '" + thisSSName + "' already exists", ctx);
        }

        final SuperStruct superStruct = new SuperStruct(thisSSName);
        dispatcher.data.setCurrentSS(superStruct);
        dispatcher.data.superStructs().put(thisSSName, superStruct);

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            processMemberCtx(memberCtx, thisSSName);
        }

        dispatcher.data.setCurrentSS(null);

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
                .map(dispatcher::visitDeclarationSpecifier)
                .toList();

        if (initDeclaratorList.initDeclarator().isEmpty()) {
            throw getSSCSyntaxException(
                    "Init declarator empty `" + SSCCUtil.Text.getLiteral(memberCtx, dispatcher.tokens) + "`",
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

            final int ptrs = SSCCUtil.getPointerLevel(declarator);
            final String name = declarator.directDeclarator().Identifier().getText();

            final Field field = new Field(isPrivate, new TypedVariable(type, ptrs, name));
            dispatcher.data.currentSS().ifPresent(ss -> ss.addMember(SSMember.field(field)));
        }
    }

    private void processMemberFunction(SSCParser.FunctionDefinitionContext functionCtx,
                                       String thisSSName,
                                       List<SSCParser.DeclarationSpecifierContext> declSpecs,
                                       List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs,
                                       boolean isPrivate) {
        assert functionCtx != null;
        assert dispatcher.data.currentSS().isPresent() : "Member of no struct";

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
            unqualifiedName = dispatcher.visitDeclarator(functionCtx.declarator());
        } else {
            throw getSSCSyntaxException("Missing declarator identifier in function definition", directDecl);
        }

        final SuperStruct superStruct = dispatcher.data.currentSS().get();

        final String currentFunctionName = superStruct.name() + "__" + unqualifiedName;
        dispatcher.data.functionStack().push(currentFunctionName);
        dispatcher.initFunctionVariables(currentFunctionName, functionCtx);

        if (!isStatic) {
            final SuperstructVariable selfReferenceVariable =
                    new SuperstructVariable(superStruct.name(), 1, "this");

            dispatcher.addFunctionVariable(selfReferenceVariable);
        }

        final FunctionDefinition functionDefinition = new FunctionDefinition(
                isStatic,
                isPure,
                isPrivate,
                withoutCustom,
                parseType(declSpecs, declarator),
                unqualifiedName,
                parseFunctionParameters(functionCtx.declarator()),
                dispatcher.visitFunctionBody(functionCtx.functionBody()),
                thisSSName
        );

        superStruct.addMember(SSMember.function(functionDefinition));

        dispatcher.data.functionStack().pop();
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
            int pointer = 0;

            final List<String> curr = new ArrayList<>();
            for (final var declSpec : param.declarationSpecifiers().declarationSpecifier()) {
                if (declSpec.typeSpecifier() == null) {
                    curr.add(dispatcher.visitDeclarationSpecifier(declSpec));
                    continue;
                }
                final var typeSpecCtx = declSpec.typeSpecifier();
                if (typeSpecCtx.superStructSpecifier() == null) {
                    final Typedef<SuperStruct> val = dispatcher.data
                            .superstructTypedefs()
                            .get(dispatcher.visitTypeSpecifier(typeSpecCtx));

                    if (val == null) {
                        curr.add(dispatcher.visitTypeSpecifier(typeSpecCtx));
                        continue;
                    }

                    pointer += val.pointer();
                    ssName = val.getName();
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
                pointer += SSCCUtil.getPointerLevel(declarator);

                if (declarator.directDeclarator().Identifier() != null) {
                    final String varName = declarator.directDeclarator().Identifier().getText();
                    if (ssName != null) {
                        dispatcher.addFunctionVariable(new SuperstructVariable(ssName, pointer, varName));
                    }
                }

                curr.add(dispatcher.visitDeclarator(declarator));
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
                withoutCustom.add(dispatcher.visitDeclarationSpecifier(declSpec));
            }
        }
        return withoutCustom;
    }
}

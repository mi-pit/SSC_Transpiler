package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.TypedVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.ArrayList;
import java.util.List;

public class SuperstructConvertor extends AbstractConvertor<SSCParser.SuperStructSpecifierContext> {
    public SuperstructConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.SuperStructSpecifierContext ctx) {
        final String thisSSName = dispatcher.visitTerminal(ctx.Identifier());

        if (ctx.superStructBody() == null) {
            return dispatcher.visitSuper(ctx);
        }
        Main.logger.printDebug("Entering Superstruct Body");

        final SuperStruct got = dispatcher.data.superStructs().get(thisSSName);
        // superstructs only have fields if they are defined
        if (got != null && !got.fields().isEmpty()) {
            throw dispatcher.getSSCSyntaxException("Superstruct with name '" + thisSSName + "' already exists", ctx);
        }

        final SuperStruct superStruct = got != null ? got : new SuperStruct(thisSSName);
        dispatcher.data.superStructs().put(thisSSName, superStruct);
        dispatcher.data.setCurrentSuperstruct(superStruct);

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            processMemberCtx(memberCtx);
        }

        dispatcher.data.setCurrentSuperstruct(null);

        final String structDeclaration = superStruct.emitStructDeclaration();
        final String methodDeclarations = superStruct.emitMethodDeclarations();
        final String methodDefinitions = superStruct.emitMethodDefinitions();

        dispatcher.addExternalDeclarationToEmitBefore(structDeclaration);
        dispatcher.addExternalDeclarationToEmitBefore(methodDeclarations);

        dispatcher.addExternalDeclarationToEmitAfter(methodDefinitions);

        return superStruct.emitStructDefinition();
    }

    private void processMemberCtx(
            final SSCParser.SuperStructMemberContext memberCtx
    ) {
        final SSCParser.DeclarationSpecifiersContext declSpecsCtx = (memberCtx.functionDefinition() != null
                ? memberCtx.functionDefinition().functionHeader().declarationSpecifiers()
                : memberCtx.declaration().declarationSpecifiers());
        final List<SSCParser.DeclarationSpecifierContext> declSpecs = declSpecsCtx.declarationSpecifier();

        final boolean isPrivate = declSpecs
                .stream()
                .anyMatch(ds -> ds.functionSpecifier() != null
                        && ds.functionSpecifier().Private() != null);

        if (memberCtx.functionDefinition() != null) {
            processMemberFunction(memberCtx.functionDefinition(), declSpecs, isPrivate);
        } else {
            assert memberCtx.declaration() != null;
            processMemberField(memberCtx.declaration(), declSpecs, isPrivate);
        }
    }

    private void processMemberField(
            SSCParser.DeclarationContext memberCtx,
            List<SSCParser.DeclarationSpecifierContext> declSpecs,
            boolean isPrivate
    ) {
        final SSCParser.InitDeclaratorListContext initDeclaratorList =
                memberCtx.initDeclaratorList();

        final List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs = declSpecs
                .stream()
                .filter(declSpec -> declSpec.functionSpecifier() == null
                        || declSpec.functionSpecifier().Private() == null)
                .toList();
        if (noPrivateSpecs.isEmpty()) {
            throw dispatcher.getSSCSyntaxException("No type specifier for superstruct member", memberCtx);
        }

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

            final List<Pointer> ptrs = dispatcher.getPointersFromDeclarator(declarator);

            final String name = dispatcher.visitTerminal(declarator.directDeclarator().Identifier());

            final Field field = new Field(isPrivate, new TypedVariable(type, ptrs, name));
            dispatcher.data.currentSuperstruct().ifPresent(ss -> ss.addField(field));
        }
    }

    private void processMemberFunction(
            SSCParser.FunctionDefinitionContext functionCtx,
            List<SSCParser.DeclarationSpecifierContext> declSpecs,
            boolean isPrivate
    ) {
        assert dispatcher.data.currentSuperstruct().isPresent();

        final FunctionHeaderData fnData = FunctionHeaderData.parse(
                dispatcher,
                functionCtx.functionHeader(),
                declSpecs
        );

        final SuperStruct superstruct = dispatcher.data.currentSuperstruct().get();
        final String qualified = superstruct.qualifyName(fnData.unqualifiedName());

        dispatcher.pushFunction(qualified, functionCtx);

        if (!fnData.isStatic()) {
            final SuperstructVariable selfReferenceVariable =
                    new SuperstructVariable(superstruct.name(), Pointer.oneConst(), "this");

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

        final SuperstructMethod functionDefinition = new SuperstructMethod(
                dispatcher,
                superstruct,
                fnData,
                isPrivate,
                parameters,
                functionCtx.functionBody() == null
                        ? null
                        : dispatcher.visitFunctionBody(functionCtx.functionBody())
        );

        superstruct.addFunction(functionDefinition);

        dispatcher.popFunction();
    }


    public static List<String> parseFunctionParameters(
            final VisitorDispatcher dispatcher,
            final SSCParser.DeclaratorContext ctx
    ) {
        final List<String> params = new ArrayList<>(ctx
                .directDeclarator()
                .parameterTypeList()
                .stream()
                .map(paramTypeLsCtx -> paramTypeLsCtx.parameterList().parameterDeclaration())
                .flatMap(paramsCtxLs -> paramsCtxLs.stream().map(dispatcher::visitParameterDeclaration))
                .filter(str -> !str.isBlank())
                .toList()
        );

        if (ctx.directDeclarator()
                .parameterTypeList()
                .stream()
                .anyMatch(ptl -> ptl.Ellipsis() != null)) {
            params.add("...");
        }

        return params;
    }
}

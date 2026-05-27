package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class SuperstructConvertor extends AbstractConvertor<SSCParser.SuperStructSpecifierContext> {
    public SuperstructConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SuperStructSpecifierContext.class);
    }

    @Override
    public String convert(SSCParser.SuperStructSpecifierContext ctx) {
        final String thisSSName = dispatcher.visit(ctx.Identifier());

        if (ctx.superStructBody() == null) {
            return dispatcher.visitSuper(ctx);
        }
        Main.logger.printDebug("Entering Superstruct Body");

        final SuperStruct got = dispatcher.data.superStructs().get(thisSSName);
        // superstructs only have fields if they are defined
        if (got != null && !got.fields().isEmpty()) {
            throw dispatcher.getSSCLanguageException("Superstruct with name '" + thisSSName + "' already exists", ctx);
        }

        final SuperStruct superStruct = got != null ? got : new SuperStruct(thisSSName);
        dispatcher.data.superStructs().put(thisSSName, superStruct);
        dispatcher.data.setCurrentSuperstruct(superStruct);

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            processMemberCtx(memberCtx);
        }

        dispatcher.data.setCurrentSuperstruct(null);

        final String structDeclaration = superStruct.emitStructDeclaration();
        final String structDefinition = superStruct.emitStructDefinition();
        final String methodDeclarations = superStruct.emitMethodDeclarations();
        final String methodDefinitions = superStruct.emitMethodDefinitions();

        dispatcher.addExternalDeclarationToEmitBefore(structDefinition);
        dispatcher.addExternalDeclarationToEmitBefore(methodDeclarations);

        dispatcher.addExternalDeclarationToEmitAfter(methodDefinitions);

        return structDeclaration;
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
            throw dispatcher.getSSCLanguageException("No type specifier for superstruct member", memberCtx);
        }

        if (initDeclaratorList.initDeclarator().isEmpty()) {
            throw dispatcher.getSSCLanguageException("Init declarator list is empty", memberCtx);
        }

        // declarator ('=' initializer)?
        for (SSCParser.InitDeclaratorContext initDecl : initDeclaratorList.initDeclarator()) {
            if (initDecl.initializer() != null) {
                throw dispatcher.getSSCLanguageException(
                        "Cannot initialize superstruct field (must use a constructor)",
                        initDecl.initializer()
                );
            }
            // (pointer declarationSpecifiers?)* directDeclarator
            final SSCParser.DeclaratorContext declarator = initDecl.declarator();
            final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(declarator);
            if (identifier == null) {
                throw dispatcher.getSSCLanguageException(
                        "Field has no identifier",
                        declarator.directDeclarator()
                );
            }

            final String name = dispatcher.visit(identifier);

            final String declarationSpecifiersString = dispatcher.visit(memberCtx.declarationSpecifiers());
            final String declaratorString = dispatcher.visit(declarator);
            final String declaration = declarationSpecifiersString + " " + declaratorString;

            final Field field = new Field(isPrivate, declaration, name);
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
                        : dispatcher.visit(functionCtx.functionBody())
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
                .flatMap(paramsCtxLs -> paramsCtxLs.stream().map(dispatcher::visit))
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

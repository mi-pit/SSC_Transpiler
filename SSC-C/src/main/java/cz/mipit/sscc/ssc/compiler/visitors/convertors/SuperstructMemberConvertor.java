package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.FunctionSSCData;
import cz.mipit.sscc.ssc.compiler.data.ss.Field;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

public class SuperstructMemberConvertor extends AbstractConvertor<SSCParser.SuperStructMemberContext> {
    public SuperstructMemberConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SuperStructMemberContext.class);
    }

    // declaration | functionDefinition
    @Override
    public String convert(SSCParser.SuperStructMemberContext ctx) {
        assert dispatcher.data.currentSuperstruct().isPresent();

        final SuperStruct superstruct = dispatcher.data.currentSuperstruct().get();

        final SSCParser.DeclarationContext declarationCtx = ctx.declaration();
        final SSCParser.FunctionDefinitionContext functionCtx = ctx.functionDefinition();

        if (declarationCtx != null) {
            processDeclaration(ctx, declarationCtx, superstruct);
        } else {
            processFunction(functionCtx, superstruct);
        }

        return "";
    }


    private void processFunction(SSCParser.FunctionDefinitionContext functionCtx,
                                 SuperStruct superstruct) {
        if (functionCtx.functionHeader().declarationSpecifiers() == null) {
            throw dispatcher.getSSCLanguageException(
                    "Function definition has no declaration specifiers",
                    functionCtx
            );
        }

        assert functionCtx != null;
        final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(functionCtx.functionHeader().declarator());
        assert identifier != null;

        final String name = dispatcher.visit(identifier);

        final SuperstructMethod fn = SuperstructMethod.definition(
                dispatcher,
                FunctionSSCData.fromDeclarationSpecifiers(
                        functionCtx.functionHeader().declarationSpecifiers().declarationSpecifier()
                ),
                name,
                functionCtx
        );

        superstruct.defineMethod(fn);
    }

    private void processDeclaration(SSCParser.SuperStructMemberContext ctx,
                                    SSCParser.DeclarationContext declarationCtx,
                                    SuperStruct superstruct) {
        if (declarationCtx.attributeDeclaration() != null) {
            throw dispatcher.getSSCLanguageException(
                    "May not declare attributes here", declarationCtx.attributeDeclaration()
            );
        }

        if (declarationCtx.staticAssertDeclaration() != null) {
            final String s = dispatcher.visitChildren(ctx);
            dispatcher.addExternalDeclarationToEmitAfter(s);
        }

        // declarationSpecifiers initDeclaratorList? ';'
        final boolean isPrivate = declarationCtx
                .declarationSpecifiers()
                .declarationSpecifier()
                .stream()
                .anyMatch(declSpec -> declSpec.superstructMemberDeclarationSpecifier() != null
                                      && declSpec.superstructMemberDeclarationSpecifier().Private() != null);

        if (declarationCtx.initDeclaratorList() == null) {
            throw dispatcher.getSSCLanguageException(
                    "Declaration does not declare a field", declarationCtx
            );
        }

        final String declarationSpecifiers = dispatcher.visit(declarationCtx.declarationSpecifiers());

        // initDeclarator (',' initDeclarator)*
        for (SSCParser.InitDeclaratorContext initDeclaratorCtx : declarationCtx.initDeclaratorList().initDeclarator()) {
            final String declarator = dispatcher.visit(initDeclaratorCtx.declarator());
            final String ident = dispatcher.visit(SSCCUtil.getIdentifierFromDeclarator(initDeclaratorCtx.declarator()));
            superstruct.addField(
                    new Field(isPrivate, declarationSpecifiers, declarator, ident)
            );
        }
    }
}

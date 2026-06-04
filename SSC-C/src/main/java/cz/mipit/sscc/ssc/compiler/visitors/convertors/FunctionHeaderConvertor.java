package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.FunctionSSCData;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Optional;

public class FunctionHeaderConvertor extends AbstractConvertor<SSCParser.FunctionHeaderContext> {
    public FunctionHeaderConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.FunctionHeaderContext.class);
    }

    // attributeSpecifierSequence? declarationSpecifiers? declarator
    @Override
    public String convert(SSCParser.FunctionHeaderContext ctx) {
        return (dispatcher.state.isInATemplate() ? "static " : "")
               + helper(ctx);
    }

    private String helper(SSCParser.FunctionHeaderContext ctx) {
        final Optional<SuperStruct> optSS = dispatcher.state.currentSuperstruct();
        if (optSS.isEmpty()) {
            return dispatcher.visitSuper(ctx);
        }
        final SuperStruct superstruct = optSS.get();

        final SSCParser.DeclarationSpecifiersContext declSpecsCtx = ctx.declarationSpecifiers();
        if (declSpecsCtx == null) {
            throw dispatcher.getSSCLanguageException(
                    "Function has no declaration specifiers", ctx
            );
        }

        final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(ctx.declarator());
        assert identifier != null;

        final String unqualifiedName = dispatcher.visit(identifier);
        final String qualifiedName = superstruct.qualifyName(
                unqualifiedName
        );

        dispatcher.addTerminalReplacement(identifier, qualifiedName);
        dispatcher.state.currentFunctionSSCData =
                FunctionSSCData.fromDeclarationSpecifiers(declSpecsCtx.declarationSpecifier());

        final String ret = dispatcher.visitChildren(ctx);

        dispatcher.removeTerminalReplacement(identifier);
        dispatcher.state.currentFunctionSSCData = null;

        return ret;
    }
}

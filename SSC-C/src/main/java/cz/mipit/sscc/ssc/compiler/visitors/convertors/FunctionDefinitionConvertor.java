package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Optional;

public class FunctionDefinitionConvertor extends AbstractConvertor<SSCParser.FunctionDefinitionContext> {
    public FunctionDefinitionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.FunctionDefinitionContext.class);
    }

    @Override
    public String convert(SSCParser.FunctionDefinitionContext ctx) {
        final Optional<SuperStruct> optSS = dispatcher.data.currentSuperstruct();
        if (optSS.isEmpty()) {
            return dispatcher.visitSuper(ctx);
        }
        final SuperStruct superstruct = optSS.get();

        final SSCParser.DeclarationSpecifiersContext declSpecsCtx = ctx.functionHeader().declarationSpecifiers();
        if (declSpecsCtx == null) {
            throw dispatcher.getSSCLanguageException(
                    "Function has no declaration specifiers", ctx
            );
        }

        final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(ctx.functionHeader().declarator());
        assert identifier != null;

        final String qualifiedName = superstruct.qualifyName(
                dispatcher.visit(identifier)
        );

        dispatcher.pushFunction(qualifiedName, ctx);

        final String functionDefinitionString = dispatcher.visitSuper(ctx);

        dispatcher.popFunction();

        return functionDefinitionString;
    }
}

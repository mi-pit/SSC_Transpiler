package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;

public class FunctionDefinitionConvertor extends AbstractConvertor<SSCParser.FunctionDefinitionContext> {
    public FunctionDefinitionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.FunctionDefinitionContext ctx) {
        // Set currentFunctionName
        if (ctx.functionBody() == null) {
            throw dispatcher.getSSCLanguageException("Function definition without body", ctx);
        }
        assert ctx.functionBody() != null;
        assert ctx.functionBody().compoundStatement() != null;

        final String unqualifiedName = dispatcher.visitTerminal(
                SSCCUtil.getIdentifierFromDeclarator(ctx.functionHeader().declarator())
        );
        final String currentFunctionName = dispatcher.data
                .currentSuperstruct()
                .map(s -> s.qualifyName(unqualifiedName))
                .orElse(unqualifiedName);

        dispatcher.pushFunction(currentFunctionName, ctx);
        final String functionDefinitionString = dispatcher.visitSuper(ctx);
        dispatcher.popFunction();

        return functionDefinitionString;
    }
}

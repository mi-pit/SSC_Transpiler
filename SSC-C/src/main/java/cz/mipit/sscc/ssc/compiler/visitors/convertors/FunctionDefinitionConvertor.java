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
        final SSCParser.DeclarationSpecifiersContext declSpecsCtx = ctx.functionHeader().declarationSpecifiers();
        if (declSpecsCtx == null) {
            throw dispatcher.getSSCLanguageException(
                    "Function has no declaration specifiers", ctx
            );
        }

        final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(ctx.functionHeader().declarator());
        assert identifier != null;

        final Optional<SuperStruct> currentSuperstruct = dispatcher.data.currentSuperstruct();

        final String unqualifiedName = dispatcher.visit(identifier);
        final String qualifiedName = currentSuperstruct
                .map(ss -> ss.qualifyName(unqualifiedName))
                .orElse(unqualifiedName);

        dispatcher.pushFunction(qualifiedName, ctx);

        final String functionDefinitionString = dispatcher.visitSuper(ctx);

        dispatcher.popFunction();

        return functionDefinitionString;
    }
}

package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.Either;

import java.util.List;
import java.util.Optional;

public class FunctionDefinitionConvertor extends AbstractConvertor<SSCParser.FunctionDefinitionContext> {
    public FunctionDefinitionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.FunctionDefinitionContext ctx) {
        // Set currentFunctionName
        if (ctx.functionBody() == null) {
            throw getSSCSyntaxException("Function definition without body", ctx);
        }
        assert ctx.functionBody() != null;
        assert ctx.functionBody().compoundStatement() != null;

        final String unqualifiedName = dispatcher.visitTerminal(ctx.declarator().directDeclarator().Identifier());
        final String currentFunctionName = dispatcher.data
                .currentSS()
                .map(SuperStruct::name)
                .orElse(unqualifiedName);

        dispatcher.pushFunction(currentFunctionName, ctx);

        getFunctionSuperstructParams(ctx);

        final String functionDefinitionString = dispatcher.super_visitFunctionDefinition(ctx);
        dispatcher.popFunction();

        return functionDefinitionString;
    }


    private void getFunctionSuperstructParams(final SSCParser.FunctionDefinitionContext ctx) {
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

            final Optional<Either<String, Typedef<SuperStruct>>> maybeSSName = dispatcher.findSSNameInDeclSpecs(
                    paramDecl.declarationSpecifiers().declarationSpecifier()
            );
            if (maybeSSName.isEmpty()) {
                continue;
            }
            maybeSSName.get().map(
                    string -> dispatcher.tryCreateSuperstructVariableFromDeclarator(string, declarator),
                    typedef -> dispatcher.tryCreateSuperstructVariableFromDeclarator(typedef, declarator)
            ).ifPresent(dispatcher::addFunctionVariable);
        }
    }
}

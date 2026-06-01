package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

public class CustomDeclSpecConvertor extends AbstractConvertor<SSCParser.SuperstructMemberDeclarationSpecifierContext> {
    public CustomDeclSpecConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SuperstructMemberDeclarationSpecifierContext.class);
    }

    @Override
    public String convert(SSCParser.SuperstructMemberDeclarationSpecifierContext ctx) {
        if (dispatcher.data.currentSuperstruct().isEmpty()) {
            throw dispatcher.getSSCLanguageException(
                    "Cannot use '" + dispatcher.getLiteral(ctx) + "' declaration specifier outside of a superstruct", ctx
            );
        }
        // Todo? `return ""`
        return dispatcher.visitSuper(ctx);
    }
}

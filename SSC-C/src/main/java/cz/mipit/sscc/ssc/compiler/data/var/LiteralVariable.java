package cz.mipit.sscc.ssc.compiler.data.var;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.List;
import java.util.stream.Collectors;

public final class LiteralVariable extends Variable {
    private final List<SSCParser.DeclarationSpecifierContext> declSpecs;
    private final SSCParser.DeclaratorContext declarator;

    public LiteralVariable(
            String identifier,
            List<Pointer> pointers,
            List<SSCParser.DeclarationSpecifierContext> declSpecs,
            SSCParser.DeclaratorContext declarator
    ) {
        super(identifier, pointers);

        this.declSpecs = declSpecs;
        this.declarator = declarator;
    }

    public String getDeclaration(VisitorDispatcher dispatcher) {
        return declSpecs.stream().map(dispatcher::visit).collect(Collectors.joining(" "))
               + " "
               + dispatcher.visit(declarator);
    }
}

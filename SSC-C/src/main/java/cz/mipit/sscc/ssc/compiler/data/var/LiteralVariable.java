package cz.mipit.sscc.ssc.compiler.data.var;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LiteralVariable extends Variable {
    private final List<SSCParser.DeclarationSpecifierContext> declSpecs;
    private final SSCParser.DeclaratorContext declarator;
    private final VisitorDispatcher dispatcher;

    public LiteralVariable(
            VisitorDispatcher dispatcher,
            String identifier,
            List<Pointer> pointers,
            List<SSCParser.DeclarationSpecifierContext> declSpecs,
            SSCParser.DeclaratorContext declarator
    ) {
        super(identifier, pointers);

        this.dispatcher = Objects.requireNonNull(dispatcher);

        this.declSpecs = Objects.requireNonNull(declSpecs);
        this.declarator = Objects.requireNonNull(declarator);

        Main.logger.printDebug("Constructing LiteralVariable: " + this);
    }

    public String getDeclaration() {
        return getDeclSpecsPadded()
               + dispatcher.visit(declarator);
    }

    private String getDeclSpecsPadded() {
        return getDeclSpecsPadded(false);
    }

    public String getDeclarationForLambda(
            final boolean removeConst,
            final String newIdentifier
    ) {
        return getDeclSpecsPadded(removeConst)
               + SSCCUtil.getDeclaratorForLambdaPassover(dispatcher, this.declarator, newIdentifier);
    }

    private String getDeclSpecsPadded(boolean removeConst) {
        return declSpecs.stream()
                .filter(declSpec ->
                        !removeConst
                        || declSpec.typeQualifier() == null
                        || declSpec.typeQualifier().Const() == null
                )
                .map(dispatcher::visit)
                .collect(Collectors.joining(" ", "", " "));
    }


    public String toString() {
        return "LiteralVariable{ " + declSpecs.stream().map(dispatcher::getLiteral).collect(Collectors.joining(" "))
               + " " + dispatcher.getLiteral(declarator)
               + " }";
    }
}

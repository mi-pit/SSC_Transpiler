package cz.mipit.sscc.ssc.compiler.data.var;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class LiteralVariable extends LambdaVariable {
    private final List<SSCParser.DeclarationSpecifierContext> declSpecs;
    private final SSCParser.DeclaratorContext declarator;
    private final VisitorDispatcher dispatcher;
    private final boolean isCompound;

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

        this.isCompound = declSpecs
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .filter(Objects::nonNull)
                .anyMatch(typeSpec ->
                        typeSpec.superStructSpecifier() != null
                        || typeSpec.structOrUnionSpecifier() != null
                        || typeSpec.templateDispatch() != null
                );
    }

    public String getDeclarationForLambda(
            final boolean removeConst,
            final String newIdentifier
    ) {
        final boolean removeConstFromDeclSpecs = removeConst && this.declarator.pointer().isEmpty();
        final boolean removeConstFromRightmostPointer = removeConst && !this.declarator.pointer().isEmpty();
        return
                getDeclSpecsPadded(
                        removeConstFromDeclSpecs
                )
                + SSCCUtil.getDeclaratorForLambdaPassover(
                        dispatcher,
                        this.declarator,
                        newIdentifier,
                        removeConstFromRightmostPointer
                );
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

    @Override
    public boolean isCompound() {
        return isCompound;
    }

    public String toString() {
        return "LiteralVariable{ " + declSpecs.stream().map(dispatcher::getLiteral).collect(Collectors.joining(" "))
               + " " + dispatcher.getLiteral(declarator)
               + " }";
    }
}

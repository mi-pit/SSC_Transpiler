package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FunctionHeaderData {
    private final boolean isStatic;
    private final boolean isPure;
    private final List<String> cDeclarationSpecifiers;
    private final SSCParser.DeclaratorContext declarator;
    private final String unqualifiedName;

    public FunctionHeaderData(
            VisitorDispatcher dispatcher,
            boolean isStatic,
            boolean isPure,
            List<String> cDeclarationSpecifiers,
            SSCParser.DeclaratorContext declarator,
            String unqualifiedName
    ) {
        this.isStatic = isStatic;
        this.isPure = isPure;
        this.cDeclarationSpecifiers = Objects.requireNonNull(cDeclarationSpecifiers);
        this.declarator = Objects.requireNonNull(declarator);
        this.unqualifiedName = Objects.requireNonNull(unqualifiedName);

        if (cDeclarationSpecifiers.isEmpty()) {
            throw dispatcher.getSSCLanguageException("Function must have a return type", declarator);
        }
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isPure() {
        return isPure;
    }

    public List<String> cDeclarationSpecifiers() {
        return cDeclarationSpecifiers;
    }

    public SSCParser.DeclaratorContext declarator() {
        return declarator;
    }

    public String unqualifiedName() {
        return unqualifiedName;
    }


    public static FunctionHeaderData parse(
            VisitorDispatcher dispatcher,
            SSCParser.FunctionHeaderContext functionCtx,
            List<SSCParser.DeclarationSpecifierContext> declSpecs
    ) {
        assert functionCtx != null;
        assert dispatcher.data.currentSuperstruct().isPresent() : "Member of no struct";

        final List<SSCParser.SuperstructMemberDeclarationSpecifierContext> sscSpecifiers = declSpecs
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::superstructMemberDeclarationSpecifier)
                .filter(Objects::nonNull)
                .toList();
        final boolean isStatic = sscSpecifiers.stream().anyMatch(fnSpec -> fnSpec.StaticFunction() != null);
        final boolean isPure = sscSpecifiers.stream().anyMatch(fnSpec -> fnSpec.Pure() != null);

        final List<String> withoutCustom = getDeclSpecsWithoutCustom(declSpecs, dispatcher);

        final SSCParser.DeclaratorContext declarator = functionCtx.declarator();
        final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(declarator);
        if (identifier == null) {
            throw dispatcher.getSSCLanguageException(
                    "Missing declarator identifier in function definition",
                    declarator
            );
        }
        final String unqualifiedName = dispatcher.visit(identifier);

        return new FunctionHeaderData(
                dispatcher, isStatic, isPure, withoutCustom,
                declarator, unqualifiedName
        );
    }

    /**
     * filter out {@code pure}, {@code private} and object-static keywords
     */
    private static List<String> getDeclSpecsWithoutCustom(
            final List<SSCParser.DeclarationSpecifierContext> declSpecs,
            final VisitorDispatcher dispatcher
    ) {
        final List<String> withoutCustom = new ArrayList<>();
        for (SSCParser.DeclarationSpecifierContext declSpec : declSpecs) {
            if (declSpecIsCustom(declSpec)) {
                continue;
            }

            withoutCustom.add(dispatcher.visit(declSpec));
        }
        return withoutCustom;
    }

    private static boolean declSpecIsCustom(SSCParser.DeclarationSpecifierContext declSpec) {
        return declSpec.superstructMemberDeclarationSpecifier() != null;
    }
}

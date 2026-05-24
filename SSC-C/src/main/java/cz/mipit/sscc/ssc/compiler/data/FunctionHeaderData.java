package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

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
            throw dispatcher.getSSCSyntaxException("Function must have a return type", declarator);
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

        final List<SSCParser.FunctionSpecifierContext> fnSpecs = declSpecs
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::functionSpecifier)
                .filter(Objects::nonNull)
                .toList();
        final boolean isStatic = fnSpecs.stream().anyMatch(fnSpec -> fnSpec.StaticFunction() != null);
        final boolean isPure = fnSpecs.stream().anyMatch(fnSpec -> fnSpec.Pure() != null);

        final List<String> withoutCustom = getDeclSpecsWithoutCustom(declSpecs, dispatcher);

        final SSCParser.DeclaratorContext declarator = functionCtx.declarator();
        final SSCParser.DirectDeclaratorContext directDecl = declarator.directDeclarator();
        if (directDecl == null) {
            throw dispatcher.getSSCSyntaxException("Direct declarator is null", functionCtx);
        }

        final String unqualifiedName;
        if (directDecl.Identifier() != null) {
            unqualifiedName = dispatcher.visitTerminal(directDecl.Identifier());
        } else if (directDecl.LeftParen() == null || directDecl.RightParen() == null) {
            Main.logger.printDebug("No declarator parentheses. Trying to parse declarator.");
            unqualifiedName = dispatcher.visitDeclarator(declarator);
            if (unqualifiedName.chars().anyMatch(c -> !Character.isAlphabetic(c) && c != '_')) {
                throw dispatcher.getSSCSyntaxException("Invalid identifier", directDecl);
            }
        } else {
            throw dispatcher.getSSCSyntaxException("Missing declarator identifier in function definition", directDecl);
        }

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

            withoutCustom.add(dispatcher.visitDeclarationSpecifier(declSpec));
        }
        return withoutCustom;
    }

    private static boolean declSpecIsCustom(SSCParser.DeclarationSpecifierContext declSpec) {
        final SSCParser.FunctionSpecifierContext funcSpec = declSpec.functionSpecifier();
        if (funcSpec == null) {
            return false;
        }

        return funcSpec.Pure() != null || funcSpec.Private() != null || funcSpec.StaticFunction() != null;
    }
}

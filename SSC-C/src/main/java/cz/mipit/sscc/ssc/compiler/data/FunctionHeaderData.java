package cz.mipit.sscc.ssc.compiler.data;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
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

        final boolean isStatic = declSpecs.stream().anyMatch(ds1 ->
                ds1.storageClassSpecifier() != null && ds1.storageClassSpecifier().Static() != null);
        final boolean isPure = declSpecs.stream().anyMatch(ds ->
                ds.functionSpecifier() != null && ds.functionSpecifier().Pure() != null);

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
     * filter out types & {@code pure} and {@code static}
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
        if (declSpec.functionSpecifier() != null) {
            final SSCParser.FunctionSpecifierContext funcSpec = declSpec.functionSpecifier();
            if (funcSpec.Pure() != null || funcSpec.Private() != null)
                return true;
        }

        // since we're in a superstruct, `static` has a different meaning from the C keyword => is custom
        // todo? change the SSC Superstruct method `static` keyword to avoid this confusion
        //  (possibly to `static_fn`?)
        if (declSpec.storageClassSpecifier() != null
                && declSpec.storageClassSpecifier().Static() != null) {
            return true;
        }

        return false;
    }
}

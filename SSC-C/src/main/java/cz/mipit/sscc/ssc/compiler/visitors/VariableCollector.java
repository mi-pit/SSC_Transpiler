package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.util.Either;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VariableCollector {
    private final VisitorDispatcher dispatcher;

    public VariableCollector(final VisitorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    private void collectSuperstructVariables(
            SSCParser.DeclarationSpecifiersContext declSpecsCtx,
            List<SSCParser.DeclaratorContext> declarators
    ) {
        if (declarators.isEmpty()) {
            // e.g. `int;`
            return;
        }

        final List<SSCParser.SuperStructSpecifierContext> ssSpecs = declSpecsCtx
                .declarationSpecifier()
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .filter(Objects::nonNull)
                .map(SSCParser.TypeSpecifierContext::superStructSpecifier)
                .filter(Objects::nonNull)
                .toList();
        if (ssSpecs.isEmpty()) {
            return;
        }
        if (ssSpecs.size() > 1) {
            throw dispatcher.getSSCSyntaxException("Multiple superstruct specifiers in declaration", ssSpecs.get(1));
        }

        final List<SSCParser.DeclarationSpecifierContext> declSpecs = declSpecsCtx.declarationSpecifier();
        final Optional<Either<String, Typedef<SuperStruct>>> maybeEither = dispatcher.findSSNameInDeclSpecs(declSpecs);
        if (maybeEither.isEmpty()) {
            return;
        }
        final Either<String, Typedef<SuperStruct>> ssNameOrTypedef = maybeEither.get();

        for (final SSCParser.DeclaratorContext declarator : declarators) {
            final Optional<SuperstructVariable> mapped = ssNameOrTypedef.map(
                    str -> dispatcher.tryCreateSuperstructVariableFromDeclarator(str, declarator),
                    typedef -> dispatcher.tryCreateSuperstructVariableFromDeclarator(typedef, declarator)
            );
            mapped.ifPresent(v -> dispatcher.data.functionVariables().get(dispatcher.getCurrentFunctionName()).add(v));
        }
    }

    public void collect(SSCParser.ParameterDeclarationContext ctx) {
        final SSCParser.DeclaratorContext declarator = ctx.declarator();
        if (declarator == null) {
            return;
        }

        final SSCParser.DeclarationSpecifiersContext declSpecsCtx = ctx.declarationSpecifiers();

        collectSuperstructVariables(declSpecsCtx, List.of(declarator));
    }

    public void collect(SSCParser.DeclarationContext ctx) {
        if (ctx.staticAssertDeclaration() != null) {
            assert ctx.declarationSpecifiers() == null;
            return;
        }
        assert ctx.declarationSpecifiers() != null;
        final var declSpecsLs = ctx.declarationSpecifiers().declarationSpecifier();
        assert declSpecsLs != null;

        final boolean isTypedef = declSpecsLs
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::storageClassSpecifier)
                .filter(Objects::nonNull)
                .map(SSCParser.StorageClassSpecifierContext::Typedef)
                .anyMatch(Objects::nonNull);

        if (isTypedef) {
            collectSuperstructTypedefs(ctx, declSpecsLs);
        } else {
            collectSuperstructVariables(ctx);
        }
    }

    private void collectSuperstructTypedefs(
            final SSCParser.DeclarationContext ctx,
            final List<SSCParser.DeclarationSpecifierContext> declSpecsLs
    ) {
        final var initDeclListCtx = ctx.initDeclaratorList();
        if (initDeclListCtx == null) {
            /* e.g.
             * typedef __builtin_va_list __darwin_va_list;
             * typedef __darwin_va_list va_list;
             * typedef __builtin_va_list va_list;
             */
            return;
        }

        final var declaratorsList = initDeclListCtx
                .initDeclarator()
                .stream()
                .map(SSCParser.InitDeclaratorContext::declarator)
                .toList();

        final List<SSCParser.SuperStructSpecifierContext> ssSpecs = declSpecsLs
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .filter(Objects::nonNull)
                .map(SSCParser.TypeSpecifierContext::superStructSpecifier)
                .filter(Objects::nonNull)
                .toList();
        if (ssSpecs.isEmpty()) {
            return;
        }
        if (ssSpecs.size() > 1) {
            throw dispatcher.getSSCSyntaxException("Multiple superstruct types found in typedef", ctx);
        }

        final SSCParser.SuperStructSpecifierContext ssSpec = ssSpecs.getFirst();
        final String ssName = dispatcher.visitTerminal(ssSpec.Identifier());

        final SuperStruct ss = dispatcher.findSuperstructByName(ssName).orElse(new SuperStruct(ssName));
        for (final SSCParser.DeclaratorContext typedefDeclarator : declaratorsList) {
            final String typedeffedName = dispatcher.visitTerminal(typedefDeclarator.directDeclarator().Identifier());

            final Typedef<SuperStruct> typedef = new Typedef<>(
                    typedeffedName,
                    dispatcher.getPointersFromDeclarator(typedefDeclarator),
                    ss
            );

            dispatcher.data.superstructTypedefs().put(typedeffedName, typedef);
        }
    }

    private void collectSuperstructVariables(
            final SSCParser.DeclarationContext ctx
    ) {
        assert ctx.declarationSpecifiers() != null;
        assert !ctx.declarationSpecifiers().declarationSpecifier().isEmpty();

        if (ctx.initDeclaratorList() == null) {
            // e.g. `int;`
            return;
        }

        final List<SSCParser.DeclarationSpecifierContext> declSpecs = ctx.declarationSpecifiers().declarationSpecifier();
        final Optional<Either<String, Typedef<SuperStruct>>> maybeEither = dispatcher.findSSNameInDeclSpecs(declSpecs);
        if (maybeEither.isEmpty()) {
            return;
        }
        final Either<String, Typedef<SuperStruct>> ssNameOrTypedef = maybeEither.get();

        for (final var initDeclarator : ctx.initDeclaratorList().initDeclarator()) {
            final SSCParser.DeclaratorContext declarator = initDeclarator.declarator();
            final Optional<SuperstructVariable> mapped = ssNameOrTypedef.map(
                    str -> dispatcher.tryCreateSuperstructVariableFromDeclarator(str, declarator),
                    typedef -> dispatcher.tryCreateSuperstructVariableFromDeclarator(typedef, declarator)
            );
            mapped.ifPresent(v -> dispatcher.data.functionVariables().get(dispatcher.getCurrentFunctionName()).add(v));
        }
    }
}

package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
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

    public void collect(SSCParser.ParameterDeclarationContext ctx) {
        final SSCParser.DeclarationSpecifiersContext declSpecsCtx = ctx.declarationSpecifiers();
        final SSCParser.DeclaratorContext declarator = ctx.declarator();
        if (declarator == null) {
            return;
        }

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
            collectSuperstructVariablesFromDeclaration(ctx);
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

        final SuperStruct ss = dispatcher.data.superStructs().get(ssName);
        if (ss == null) {
            throw dispatcher.getSSCSyntaxException(
                    "Unknown superstruct type", ssSpec
            );
        }

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

    private void collectSuperstructVariablesFromDeclaration(
            final SSCParser.DeclarationContext ctx
    ) {
        assert ctx.declarationSpecifiers() != null;
        assert !ctx.declarationSpecifiers().declarationSpecifier().isEmpty();

        if (ctx.initDeclaratorList() == null) {
            // e.g. `int;` | `struct { ... };`
            return;
        }

        final SSCParser.DeclarationSpecifiersContext declSpecs = ctx.declarationSpecifiers();
        final List<SSCParser.DeclaratorContext> declarators = ctx
                .initDeclaratorList()
                .initDeclarator()
                .stream()
                .map(SSCParser.InitDeclaratorContext::declarator)
                .toList();

        collectSuperstructVariables(declSpecs, declarators);
    }

    private void collectSuperstructVariables(
            SSCParser.DeclarationSpecifiersContext declSpecsCtx,
            List<SSCParser.DeclaratorContext> declarators
    ) {
        if (declarators.isEmpty()) {
            return;
        }

        final List<SSCParser.TypeSpecifierContext> typeSpecifiers = declSpecsCtx
                .declarationSpecifier()
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .filter(Objects::nonNull)
                .toList();

        final List<SSCParser.SuperStructSpecifierContext> ssSpecs = typeSpecifiers
                .stream()
                .map(SSCParser.TypeSpecifierContext::superStructSpecifier)
                .filter(Objects::nonNull)
                .toList();

        if (ssSpecs.isEmpty() &&
                typeSpecifiers
                        .stream()
                        .map(SSCParser.TypeSpecifierContext::typedefName)
                        .noneMatch(Objects::nonNull)
        ) {
            collectSuperstructVariablesFromTemplateDispatchTypeSpecifier(
                    declSpecsCtx, declarators
            );
            return;
        }


        if (ssSpecs.size() > 1) {
            throw dispatcher.getSSCSyntaxException("Multiple superstruct specifiers in declaration", ssSpecs.get(1));
        }

        final List<SSCParser.DeclarationSpecifierContext> declSpecs = declSpecsCtx.declarationSpecifier();
        final Optional<Either<String, Typedef<SuperStruct>>> maybeEither = findSSNameInDeclSpecs(dispatcher, declSpecs);
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

    private void collectSuperstructVariablesFromTemplateDispatchTypeSpecifier(
            SSCParser.DeclarationSpecifiersContext declSpecsCtx,
            List<SSCParser.DeclaratorContext> declarators
    ) {
        final List<SSCParser.TemplateDispatchContext> ssTmpls = declSpecsCtx
                .declarationSpecifier()
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .filter(Objects::nonNull)
                .map(typeSpec -> {
                    if (typeSpec.templateDispatch() == null)
                        return null;
                    if (typeSpec.Superstruct() == null)
                        return null;

                    return typeSpec.templateDispatch();
                })
                .filter(Objects::nonNull)
                .toList();
        if (ssTmpls.isEmpty()) {
            return;
        }
        if (ssTmpls.size() > 1) {
            throw dispatcher.getSSCSyntaxException(
                    "Duplicate superstruct specifier", declSpecsCtx.declarationSpecifier().getLast()
            );
        }

        final String ssName = dispatcher.visitTemplateDispatch(ssTmpls.getFirst());

        Main.logger.printDebug("Found ss-tmpl reference of superstruct " + ssName);
        for (final var declarator : declarators) {
            final SuperstructVariable ssVar = new SuperstructVariable(
                    ssName,
                    dispatcher.getPointersFromDeclarator(declarator),
                    dispatcher.visitTerminal(declarator.directDeclarator().Identifier())
            );
            dispatcher.data.functionVariables().get(dispatcher.getCurrentFunctionName()).add(ssVar);
        }
    }

    private static Optional<Either<String, Typedef<SuperStruct>>> findSSNameInDeclSpecs(
            final VisitorDispatcher dispatcher,
            final List<SSCParser.DeclarationSpecifierContext> declSpecs
    ) {
        for (final SSCParser.DeclarationSpecifierContext declSpec : declSpecs) {
            if (declSpec.typeSpecifier() == null) {
                continue;
            }

            final SSCParser.TypeSpecifierContext typeSpec = declSpec.typeSpecifier();
            if (typeSpec.superStructSpecifier() == null) {
                final Typedef<SuperStruct> typedef = dispatcher.data.superstructTypedefs().get(dispatcher.visitTypeSpecifier(typeSpec));
                if (typedef == null) {
                    continue;
                }
                return Optional.of(Either.right(typedef));
            }

            final SSCParser.SuperStructSpecifierContext ssCtx = typeSpec.superStructSpecifier();
            final String ssName = dispatcher.visitTerminal(ssCtx.Identifier());
            return Optional.of(Either.left(ssName));
        }

        return Optional.empty();
    }
}

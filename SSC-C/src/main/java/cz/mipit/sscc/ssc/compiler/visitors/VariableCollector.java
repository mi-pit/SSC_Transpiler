package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.util.Either;
import org.antlr.v4.runtime.ParserRuleContext;

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
        if (declSpecsCtx == null) {
            return; // empty parameter list
        }

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
                .anyMatch(declSpec ->
                        declSpec.storageClassSpecifier() != null && declSpec.storageClassSpecifier().Typedef() != null
                );

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
        final SSCParser.InitDeclaratorListContext initDeclListCtx = ctx.initDeclaratorList();
        if (initDeclListCtx == null) {
            /* e.g.
             * typedef __builtin_va_list __darwin_va_list;
             * typedef __darwin_va_list va_list;
             * typedef __builtin_va_list va_list;
             */
            return;
        }

        final List<SSCParser.DeclaratorContext> declaratorsList = initDeclListCtx
                .initDeclarator()
                .stream()
                .map(SSCParser.InitDeclaratorContext::declarator)
                .toList();

        final List<String> ssSpecs = declSpecsLs
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .filter(Objects::nonNull)
                .map(type -> {
                    if (type.Superstruct() != null) {
                        return dispatcher.visit(type.templateDispatch());
                    }
                    if (type.superStructSpecifier() != null) {
                        return dispatcher.visit(type.superStructSpecifier().Identifier());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        if (ssSpecs.isEmpty()) {
            return;
        }
        if (ssSpecs.size() > 1) {
            throw dispatcher.getSSCLanguageException("Multiple superstruct types found in typedef", ctx);
        }

        final String ssName = ssSpecs.getFirst();
        final SuperStruct ss = dispatcher.state.getSuperstruct(ssName);
        if (ss == null) {
            throw dispatcher.getSSCLanguageException(
                    "Unknown superstruct type 'object " + ssName + "'",
                    declSpecsLs
                            .stream()
                            .filter(declSpec -> declSpec.typeSpecifier() != null)
                            .map(a -> (ParserRuleContext) a)
                            .findFirst()
                            .orElse(ctx)
            );
        }

        for (final SSCParser.DeclaratorContext typedefDeclarator : declaratorsList) {
            final String typedeffedName = dispatcher.visit(typedefDeclarator.directDeclarator().Identifier());

            final Typedef<SuperStruct> typedef = new Typedef<>(
                    typedeffedName,
                    Pointer.fromDeclarator(dispatcher::visit, typedefDeclarator),
                    ss
            );

            dispatcher.state.addSuperstructTypedef(typedef, typedefDeclarator);
            Main.logger.printDebug("Added typedef '" + typedeffedName + "' for superstruct '" + ssName + "'");
        }
    }

    private void collectSuperstructVariablesFromDeclaration(
            final SSCParser.DeclarationContext ctx
    ) {
        if (ctx.initDeclaratorList() == null) {
            // e.g. `int;` | `struct { ... };`
            return;
        }

        final SSCParser.DeclarationSpecifiersContext declSpecs = ctx.declarationSpecifiers();
        if (declSpecs == null) {
            throw dispatcher.getSSCLanguageException(
                    "No declaration specifiers in declaration", ctx
            );
        }

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

        final List<SSCParser.DeclarationSpecifierContext> declSpecs = declSpecsCtx.declarationSpecifier();
        final Optional<Either<String, Typedef<SuperStruct>>> maybeEither = findSSNameInDeclSpecs(declSpecs);
        if (maybeEither.isEmpty()) {
            return;
        }

        final Either<String, Typedef<SuperStruct>> ssNameOrTypedef = maybeEither.get();

        for (final SSCParser.DeclaratorContext declarator : declarators) {
            if (!declarator.directDeclarator().parameterTypeList().isEmpty()) {
                // Function declaration
                continue;
            }

            final Optional<SuperstructVariable> mapped = ssNameOrTypedef.map(
                    str -> tryCreateSuperstructVariableFromDeclarator(str, declarator),
                    typedef -> tryCreateSuperstructVariableFromDeclarator(typedef, declarator)
            );

            mapped.ifPresent(v ->
                    dispatcher.state.addFunctionVariable(v, declarator)
            );
        }
    }

    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final String ssName,
            final SSCParser.DeclaratorContext declarator
    ) {
        return tryCreateSuperstructVariableFromDeclarator(ssName, Pointer.none(), declarator);
    }

    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final Typedef<SuperStruct> typedef,
            final SSCParser.DeclaratorContext declarator
    ) {
        final SuperStruct ss = typedef.getRepresentedType();
        return tryCreateSuperstructVariableFromDeclarator(
                ss.name(),
                typedef.getPointers(),
                declarator
        );
    }

    private Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final String ssName,
            final List<Pointer> pointerBase,
            final SSCParser.DeclaratorContext declarator
    ) {
        if (declarator == null) {
            return Optional.empty();
        }
        final SSCParser.DirectDeclaratorContext directDecl = declarator.directDeclarator();
        if (directDecl.Identifier() == null) {
            return Optional.empty();
        }

        final List<Pointer> declaratorPointers = Pointer.fromDeclarator(dispatcher::visit, declarator);
        final String varName = dispatcher.visit(directDecl.Identifier());

        final SuperstructVariable ssVar = new SuperstructVariable(
                ssName,
                Pointer.combine(pointerBase, declaratorPointers),
                varName
        );
        return Optional.of(ssVar);
    }

    private Optional<Either<String, Typedef<SuperStruct>>> findSSNameInDeclSpecs(
            final List<SSCParser.DeclarationSpecifierContext> declSpecs
    ) {
        for (final SSCParser.DeclarationSpecifierContext declSpec : declSpecs) {
            if (declSpec.typeSpecifier() == null) {
                continue;
            }

            final SSCParser.TypeSpecifierContext typeSpec = declSpec.typeSpecifier();
            if (typeSpec.superStructSpecifier() != null) {
                final String ssName = dispatcher.visit(typeSpec.superStructSpecifier().Identifier());
                return Optional.of(Either.left(ssName));
            }

            if (typeSpec.Superstruct() != null && typeSpec.templateDispatch() != null) {
                final String ssName = dispatcher.visit(typeSpec.templateDispatch());
                return Optional.of(Either.left(ssName));
            }

            final Typedef<SuperStruct> typedef = dispatcher.state.getSuperstructTypedef(
                    dispatcher.visit(typeSpec)
            );
            if (typedef != null) {
                return Optional.of(Either.right(typedef));
            }

            return Optional.empty();
        }
        return Optional.empty();
    }
}

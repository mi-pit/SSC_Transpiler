package cz.mipit.sscc.ssc.compiler.data.ss;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionSSCData;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SuperstructMethod {
    private final FunctionSSCData metadata;

    private final String name;
    private final String header;
    private final @Nullable String definition;

    private final @Nullable SuperstructVariable returnType;

    private final ParserRuleContext context;


    private SuperstructMethod(
            FunctionSSCData metadata,
            String name,
            String header,
            String definition,
            SuperstructVariable returnType,
            ParserRuleContext context
    ) {
        Objects.requireNonNull(metadata);
        Objects.requireNonNull(name);
        Objects.requireNonNull(header);
        Objects.requireNonNull(context);

        this.metadata = metadata;
        this.name = name;

        this.header = header;
        this.definition = definition;

        this.returnType = returnType;
        if (returnType != null) {
            Main.logger.printDebug(() -> "Return type of '" + name + "' is 'object " + returnType.superstruct().name() + "'");
        }

        this.context = context;
    }

    public static SuperstructMethod header(
            FunctionSSCData metadata,
            String name,
            String header,
            VisitorDispatcher dispatcher,
            SSCParser.FunctionHeaderContext headerCtx
    ) {
        return new SuperstructMethod(
                metadata,
                name,
                header,
                null,
                getSuperstructReturnType(
                        dispatcher,
                        headerCtx.declarationSpecifiers(),
                        Pointer.fromDeclarator(dispatcher::visit, headerCtx.declarator())
                ),
                headerCtx
        );
    }

    public static SuperstructMethod definition(
            VisitorDispatcher dispatcher,
            FunctionSSCData metadata,
            String name,
            SSCParser.FunctionDefinitionContext context
    ) {
        final String definition = dispatcher.visit(context);
        final String header = definition.split("\\{")[0];

        return new SuperstructMethod(
                metadata,
                name,
                header,
                definition,
                getSuperstructReturnType(
                        dispatcher,
                        context.functionHeader().declarationSpecifiers(),
                        Pointer.fromDeclarator(dispatcher::visit, context.functionHeader().declarator())
                ),
                context);
    }

    private static SuperstructVariable getSuperstructReturnType(
            final VisitorDispatcher dispatcher,
            final SSCParser.DeclarationSpecifiersContext declarationSpecifiers,
            final List<Pointer> pointers
    ) {
        assert declarationSpecifiers != null;
        return declarationSpecifiers
                .declarationSpecifier()
                .stream()
                .map(SSCParser.DeclarationSpecifierContext::typeSpecifier)
                .filter(Objects::nonNull)
                .map(SSCParser.TypeSpecifierContext::superStructSpecifier)
                .filter(Objects::nonNull)
                .map(SSCParser.SuperStructSpecifierContext::Identifier)
                .map(dispatcher::visit)
                .map(dispatcher.state::getSuperstruct)
                .filter(Objects::nonNull)
                .map(ss -> new SuperstructVariable(ss, pointers, "<return type>"))
                .findFirst()
                .orElse(null);
    }

    public String header() {
        return header;
    }

    public FunctionSSCData metadata() {
        return metadata;
    }

    public Optional<String> definition() {
        return Optional.ofNullable(definition);
    }

    public String name() {
        return name;
    }

    public ParserRuleContext context() {
        return context;
    }

    public SuperstructVariable returnType() {
        return returnType;
    }

    @Override
    public String toString() {
        return "SuperstructMethod{" + metadata() + " | " + name + "}";
    }
}

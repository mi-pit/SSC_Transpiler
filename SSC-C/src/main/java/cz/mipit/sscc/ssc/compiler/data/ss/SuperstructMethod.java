package cz.mipit.sscc.ssc.compiler.data.ss;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.FunctionSSCData;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Objects;
import java.util.Optional;

public class SuperstructMethod {
    private final FunctionSSCData metadata;

    private final String name;
    private final String header;
    private final @Nullable String definition;

    private final ParserRuleContext context;


    private SuperstructMethod(
            FunctionSSCData metadata,
            String name,
            String header,
            String definition,
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

        this.context = context;
    }

    public static SuperstructMethod header(
            FunctionSSCData metadata,
            String name,
            String header,
            ParserRuleContext context
    ) {
        if (!(context instanceof SSCParser.FunctionHeaderContext)) {
            throw new IllegalArgumentException("context must be a SSCParser.FunctionHeaderContext");
        }

        return new SuperstructMethod(metadata, name, header, null, context);
    }

    public static SuperstructMethod definition(
            VisitorDispatcher dispatcher,
            FunctionSSCData metadata,
            String name,
            ParserRuleContext context
    ) {
        if (!(context instanceof SSCParser.FunctionDefinitionContext fnDef)) {
            throw new IllegalArgumentException("Context must be of a function definition or header");
        }
        final String definition = dispatcher.visit(fnDef);
        final String header = definition.split("\\{")[0];

        return new SuperstructMethod(metadata, name, header, definition, context);
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

    @Override
    public String toString() {
        return "SuperstructMethod{" + metadata() + " | " + name + "}";
    }
}

package cz.mipit.sscc.ssc.compiler.data.ss;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.FunctionMetadata;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Optional;

public class SuperstructMethod {
    private final FunctionMetadata metadata;

    private final String name;
    private final String header;
    private final @Nullable String definition;

    private final ParserRuleContext context;

    public SuperstructMethod(
            VisitorDispatcher dispatcher,
            FunctionMetadata metadata,
            String name,
            ParserRuleContext context
    ) {
        if (!(context instanceof SSCParser.FunctionDefinitionContext) &&
            !(context instanceof SSCParser.FunctionHeaderContext)) {
            throw new IllegalArgumentException("Context must be of a function definition or header");
        }

        this.metadata = metadata;
        this.name = name;

        if (context instanceof SSCParser.FunctionDefinitionContext fnDef) {
            this.definition = dispatcher.visit(fnDef);
            this.header = definition.split("\\{")[0];
        } else {
            this.definition = null;
            this.header = dispatcher.visit(context);
        }

        this.context = context;
    }

    public String header() {
        return header;
    }

    public FunctionMetadata metadata() {
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

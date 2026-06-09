package cz.mipit.sscc.ssc.compiler.data.tmpl;

import antlr.ssc.SSCParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class Template {
    private final String name;
    private final List<SSCParser.TemplateDefinitionContext> contexts;

    public Template(String name,
                    SSCParser.TemplateDefinitionContext ctx) {
        this.name = name;
        this.contexts = new ArrayList<>();
        this.contexts.add(ctx);
    }

    public String name() {
        return name;
    }

    public List<SSCParser.TemplateDefinitionContext> contexts() {
        return List.copyOf(contexts);
    }

    public void addContext(SSCParser.TemplateDefinitionContext context) {
        if (contexts.size() != 1
                || contexts.getFirst().superStructInterface() == null
                || context.superStructSpecifier() == null) {
            throw new UnsupportedOperationException("Can only add ss-specifier to ss-interface");
        }

        contexts.add(context);
    }


    @Override
    public String toString() {
        final StringJoiner outer = new StringJoiner(" | ");
        for (SSCParser.TemplateDefinitionContext context : contexts) {
            final StringJoiner inner = new StringJoiner(", ");
            for (TerminalNode ident : context.templateHeader().templateTypes().Identifier()) {
                inner.add(ident.getText());
            }
            outer.add("'%s<%s>'".formatted(name, inner));
        }

        return "Template={" + outer + "}";
    }
}

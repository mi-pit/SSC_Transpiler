package cz.mipit.sscc.ssc.compiler.data.tmpl;

import antlr.ssc.SSCParser;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.StringJoiner;

public class Template {
    private final SSCParser.FunctionTemplateDefinitionContext ctx;

    private final String name;

    public Template(String name,
                    SSCParser.FunctionTemplateDefinitionContext ctx) {
        this.ctx = ctx;
        this.name = name;
    }

    public SSCParser.FunctionTemplateDefinitionContext getContext() {
        return ctx;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner(", ");
        for (TerminalNode ident : ctx.templateHeader().templateTypes().Identifier()) {
            joiner.add(ident.getText());
        }

        return "Template='%s<%s>'".formatted(name, joiner);
    }
}

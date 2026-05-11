package cz.mipit.sscc.ssc.compiler.data.tmpl;

import antlr.ssc.SSCParser;

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
        return "Template='" + name + '\'';
    }
}

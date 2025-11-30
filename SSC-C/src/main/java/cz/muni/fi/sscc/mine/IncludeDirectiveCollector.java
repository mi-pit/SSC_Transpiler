package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.antlr.SSCBaseVisitor;
import cz.muni.fi.sscc.antlr.SSCParser;

import java.util.ArrayList;
import java.util.List;

public class IncludeDirectiveCollector extends SSCBaseVisitor<Void> {
    private final List<String> includes;

    public IncludeDirectiveCollector() {
        this.includes = new ArrayList<>();
    }

    @Override
    public Void visitDirective(SSCParser.DirectiveContext ctx) {
        if (ctx.IncludeDirective() == null || ctx.IncludeDirectiveSTD() == null) {
            includes.add(ctx.getText());
        }

        return null;
    }

    public List<String> getIncludes() {
        return includes;
    }
}

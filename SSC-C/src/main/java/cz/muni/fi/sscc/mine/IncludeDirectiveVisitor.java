package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.antlr.SSCBaseVisitor;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;

public class IncludeDirectiveVisitor extends SSCBaseVisitor<String> {
    private final CommonTokenStream tokens;

    public IncludeDirectiveVisitor(CommonTokenStream tokens) {
        this.tokens = tokens;
    }

    @Override
    public String visitDirective(SSCParser.DirectiveContext ctx) {
        System.out.println("[DEBUG] Directive: " + Util.getContextText(ctx, tokens));
        return super.visitDirective(ctx);
    }
}

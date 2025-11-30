package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.util.Util;
import org.antlr.v4.runtime.CommonTokenStream;

public class IncludeDirectiveRemoverVisitor extends ConvertorVisitor {
    public IncludeDirectiveRemoverVisitor(CommonTokenStream tokens) {
        super(tokens);
    }

    @Override
    public String visitDirective(SSCParser.DirectiveContext ctx) {
        if (ctx.IncludeDirectiveSTD() == null && ctx.IncludeDirective() == null) {
            // def, if, ...
            return super.visitDirective(ctx);
        }

        Util.printDebug("Include directive: '" + Util.getContextText(ctx, tokens) + "'");
        return "";
    }
}

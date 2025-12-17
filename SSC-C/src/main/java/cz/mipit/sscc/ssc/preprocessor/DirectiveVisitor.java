package cz.mipit.sscc.ssc.preprocessor;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.ConvertorVisitor;
import org.antlr.v4.runtime.CommonTokenStream;

public class DirectiveVisitor extends ConvertorVisitor {
    public DirectiveVisitor(CommonTokenStream tokens) {
        super(tokens);
    }

    @Override
    public String visitDirective(SSCParser.DirectiveContext ctx) {
        final String filename = ctx.Directive().getText().split("\"");
        return super.visitDirective(ctx);
    }
}

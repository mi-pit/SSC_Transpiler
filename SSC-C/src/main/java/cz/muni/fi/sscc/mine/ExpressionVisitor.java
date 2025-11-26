package cz.muni.fi.sscc.mine;

import cz.muni.fi.sscc.antlr.SSCBaseVisitor;
import cz.muni.fi.sscc.antlr.SSCParser;
import cz.muni.fi.sscc.mine.data.SuperStructRepre;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.Collection;

public abstract class ExpressionVisitor extends SSCBaseVisitor<String> {
    protected final CommonTokenStream tokens;
    protected final Collection<SuperStructRepre> superstructs;

    protected ExpressionVisitor(CommonTokenStream tokens, Collection<SuperStructRepre> sss) {
        this.tokens = tokens;
        this.superstructs = sss;
    }

    @Override
    public String visitUnaryExpression(SSCParser.UnaryExpressionContext ctx) {
        if (ctx.unaryOperator() != null) {
            return ctx.unaryOperator().getText() + visitChildren(ctx); // Fixme? this seems hacky
        }
        return visitChildren(ctx);
    }
}

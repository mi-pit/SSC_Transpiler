package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

public class TernaryOperatorConvertor extends AbstractConvertor<SSCParser.ConditionalExpressionContext> {
    public TernaryOperatorConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.ConditionalExpressionContext ctx) {
        if (ctx.If() == null) {
            return dispatcher.visitSuper(ctx);
        }
        assert ctx.If() != null;
        assert ctx.Then() != null;
        assert ctx.Else() != null;
        assert ctx.logicalOrExpression() != null;
        assert ctx.expression() != null;
        assert ctx.conditionalExpression() != null;

        final String fstPartString = dispatcher.visitLogicalOrExpression(ctx.logicalOrExpression());
        final String middlePartString = dispatcher.visitExpression(ctx.expression());
        final String lastPartString = dispatcher.visitConditionalExpression(ctx.conditionalExpression());
        return "(%s ? %s : %s)".formatted(fstPartString, middlePartString, lastPartString);
    }
}

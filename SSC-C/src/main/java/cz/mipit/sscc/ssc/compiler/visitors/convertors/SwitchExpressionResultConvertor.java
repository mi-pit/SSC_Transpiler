package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

public class SwitchExpressionResultConvertor extends AbstractConvertor<SSCParser.SwitchExpressionResultContext> {
    public SwitchExpressionResultConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SwitchExpressionResultContext.class);
    }

    @Override
    public String convert(SSCParser.SwitchExpressionResultContext ctx) {
        assert dispatcher.getCurrentFunctionName() != null;

        final String currentFunctionName = dispatcher.getCurrentFunctionName();

        return "";
    }
}

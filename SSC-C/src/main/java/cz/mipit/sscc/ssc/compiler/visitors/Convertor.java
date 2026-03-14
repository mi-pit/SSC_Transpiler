package cz.mipit.sscc.ssc.compiler.visitors;

import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class Convertor<T extends ParserRuleContext> {
    protected final VisitorDispatcher dispatcher;

    protected Convertor(VisitorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    abstract public String convert(T ctx);

    protected SSCSyntaxException getSSCSyntaxException(String message, ParserRuleContext ctx) {
        return dispatcher.getSSCSyntaxException(message, ctx);
    }
}

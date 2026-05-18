package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractConvertor<T extends ParserRuleContext> implements Convertor<T> {
    protected final VisitorDispatcher dispatcher;

    protected AbstractConvertor(VisitorDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}

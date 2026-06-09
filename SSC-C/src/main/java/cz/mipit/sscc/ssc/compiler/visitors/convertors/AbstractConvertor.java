package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.ParserRuleContext;

public abstract class AbstractConvertor<T extends ParserRuleContext> implements Convertor<T> {
    protected final VisitorDispatcher dispatcher;
    private final Class<T> contextClass;

    protected AbstractConvertor(
            final VisitorDispatcher dispatcher,
            final Class<T> convertedClass
    ) {
        this.dispatcher = dispatcher;
        this.contextClass = convertedClass;
    }

    @Override
    public Class<T> getContextClass() {
        return contextClass;
    }
}

package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import org.antlr.v4.runtime.ParserRuleContext;

public interface Convertor<T extends ParserRuleContext> {
    /**
     * Converts the context to a string.
     * <p>
     * Implementations should use the dispatcher to recursively visit child contexts when applicable.
     * </p>
     */
    String convert(T ctx);
}

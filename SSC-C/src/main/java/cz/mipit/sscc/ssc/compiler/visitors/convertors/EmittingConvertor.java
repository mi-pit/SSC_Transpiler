package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Optional;

public interface EmittingConvertor<T extends ParserRuleContext> extends Convertor<T> {
    Optional<String> emit();
}

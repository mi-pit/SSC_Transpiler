package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;

public class FlagsConvertor extends AbstractConvertor<SSCParser.FlagsSpecifierContext> {
    public FlagsConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public String convert(SSCParser.FlagsSpecifierContext ctx) {
        if (ctx.flagsInitializerList() == null) {
            return dispatcher.visitSuper(ctx);
        }

        final String identifier = ctx.Identifier() == null ? "" : dispatcher.visitTerminal(ctx.Identifier());
        final var valuesListCtx = ctx.flagsInitializerList();

        final Map<String, Long> valuesMap = new TreeMap<>();

        final byte distinctCount = getDistinctFlagsCount(ctx, valuesListCtx, valuesMap);


        final int bitsNeeded =
                distinctCount <= 8 ? 8
                        : distinctCount <= 16 ? 16
                        : distinctCount <= 32 ? 32
                        : 64;

        final String type = "uint" + bitsNeeded + "_t";

        final StringBuilder valuesString = new StringBuilder();
        for (final Map.Entry<String, Long> entry : valuesMap.entrySet()) {
            valuesString
                    .append(SSCCUtil.Text.INDENT)
                    .append("%s = 0x%X,".formatted(entry.getKey(), entry.getValue()))
                    .append(lineSeparator());
        }

        return String.format("""
                        enum %s%s {
                        %s}""",
                identifier,
                dispatcher.hasType(type) ? (" : " + type) : "",
                valuesString
        );
    }

    private byte getDistinctFlagsCount(SSCParser.FlagsSpecifierContext ctx,
                                       SSCParser.FlagsInitializerListContext valuesListCtx,
                                       Map<String, Long> valuesMap) {
        byte distinctCount = 0;
        long nextValue = 1;
        for (final SSCParser.FlagsInitializerContext initializer : valuesListCtx.flagsInitializer()) {
            final List<String> identifiers = initializer
                    .Identifier()
                    .stream()
                    .skip(1)
                    .map(dispatcher::visitTerminal)
                    .toList();

            final long currValue;
            if (initializer.Identifier().size() > 1) {
                currValue = getFlagValue(valuesMap, identifiers, initializer);
            } else {
                currValue = nextValue;
                nextValue *= 2;
                assert (nextValue & (nextValue - 1)) == 0;
            }

            if (valuesMap.put(dispatcher.visitTerminal(initializer.Identifier(0)), currValue) != null) {
                throw getSSCSyntaxException("Duplicate identifier in flags specifier", initializer);
            }

            ++distinctCount;
            if (distinctCount > 64) {
                throw getSSCSyntaxException("Too many flags in flags specifier (max is 64)", ctx);
            }
        }
        return distinctCount;
    }

    private long getFlagValue(final Map<String, Long> valuesMap,
                              List<String> identifiers,
                              ParserRuleContext ctx) {
        long total = 0;
        for (final String identifier : identifiers) {
            final Long value = valuesMap.get(identifier);
            if (value == null) {
                throw getSSCSyntaxException(
                        "Flags may only be initialized with values from the same set",
                        ctx);
            }
            total |= value;
        }
        return total;
    }
}

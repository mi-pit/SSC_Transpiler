package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;

public class FlagsConvertor extends AbstractConvertor<SSCParser.FlagsSpecifierContext> {
    private static final String[] POSSIBLE_INTEGER_INITIALIZERS = new String[]{
            "0", "0u", "0l", "0ll", "0ull",
    };

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

        final String desiredType = "uint" + bitsNeeded + "_t";
        final String usedType = dispatcher.hasType(desiredType)
                ? (" : " + desiredType)
                : "";

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
                usedType,
                valuesString
        );
    }

    private byte getDistinctFlagsCount(SSCParser.FlagsSpecifierContext ctx,
                                       SSCParser.FlagsInitializerListContext valuesListCtx,
                                       Map<String, Long> valuesMap) {
        byte distinctCount = 0;
        long nextValue = 1;
        for (final SSCParser.FlagsInitializerContext initializer : valuesListCtx.flagsInitializer()) {
            final String currentFlagIdentifier = dispatcher.visitTerminal(initializer.Identifier(0));
            final List<String> assignedIdentifiers = initializer
                    .Identifier()
                    .stream()
                    .skip(1)
                    .map(dispatcher::visitTerminal)
                    .toList();

            final long currValue;
            if (!assignedIdentifiers.isEmpty()) {
                currValue = getFlagValue(valuesMap, assignedIdentifiers, initializer);
            } else if (initializer.IntegerConstant() != null) {
                currValue = getFlagValueNumeric(initializer);
            } else {
                assert initializer.Assign() == null;
                currValue = nextValue;
                nextValue *= 2;
                assert (nextValue & (nextValue - 1)) == 0;
            }

            if (valuesMap.put(currentFlagIdentifier, currValue) != null) {
                throw dispatcher.getSSCSyntaxException("Duplicate identifier in flags specifier", initializer);
            }

            ++distinctCount;
            if (distinctCount > 64) {
                throw dispatcher.getSSCSyntaxException("Too many flags in flags specifier (max is 64)", ctx);
            }
        }
        return distinctCount;
    }

    private long getFlagValueNumeric(SSCParser.FlagsInitializerContext initializer) {
        final TerminalNode integerConstantNode = initializer.IntegerConstant();

        final String string = dispatcher.visitTerminal(integerConstantNode)
                .toLowerCase();
        for (String value : POSSIBLE_INTEGER_INITIALIZERS) {
            if (string.equals(value)) {
                return 0;
            }
        }

        throw dispatcher.getSSCSyntaxException(
                "Value of a flags initializer must be one of "
                        + Arrays.toString(POSSIBLE_INTEGER_INITIALIZERS),
                initializer
        );
    }

    private long getFlagValue(final Map<String, Long> valuesMap,
                              final List<String> identifiers,
                              final ParserRuleContext ctx) {
        long total = 0;
        for (final String identifier : identifiers) {
            final Long value = valuesMap.get(identifier);
            if (value == null) {
                throw dispatcher.getSSCSyntaxException(
                        "Flags may only be initialized with values from the same set", ctx
                );
            }
            total |= value;
        }
        return total;
    }
}

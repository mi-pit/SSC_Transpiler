package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import antlr.ssc.Symbol;
import antlr.ssc.TypeClassification;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.lang.System.lineSeparator;

record PossibleEnumType(String pre, String post) {
    String toCType(int bitsNeeded) {
        return pre + bitsNeeded + post;
    }
}

public class FlagsConvertor extends AbstractConvertor<SSCParser.FlagsSpecifierContext> {
    private static final int[] POSSIBLE_BIT_VALUES = {
            8, 16, 32, 64
    };

    private static final String[] POSSIBLE_INTEGER_INITIALIZERS = {
            "0",
            "0u", "0l", "0ll", "0ull",
    };

    private static final PossibleEnumType FROM_SSCLIB_TYPES = new PossibleEnumType("u", "");
    private static final List<PossibleEnumType> POSSIBLE_ENUM_TYPES = List.of(
            FROM_SSCLIB_TYPES,
            new PossibleEnumType("uint", "_t"),
            new PossibleEnumType("uint_least", "_t")
    );

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
        final byte distinctCount = getDistinctFlags(ctx, valuesListCtx, valuesMap);

        final int bitsNeeded =
                distinctCount <= 8 ? 8
                        : distinctCount <= 16 ? 16
                        : distinctCount <= 32 ? 32
                        : 64;

        final String usedType = getTypeAssignment(bitsNeeded);

        final StringBuilder valuesString = new StringBuilder();
        for (final Map.Entry<String, Long> entry : valuesMap.entrySet()) {
            valuesString
                    .append(SSCCUtil.Text.INDENT)
                    .append(entry.getKey())
                    .append(" = ")
                    .append(entry.getValue())
                    .append(",")
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

    private String getTypeAssignment(int bitsNeeded) {
        return " : " + getDesiredType(bitsNeeded);
    }

    private String getDesiredType(int bitsNeeded) {
        for (final PossibleEnumType possibleEnumType : POSSIBLE_ENUM_TYPES) {
            final String desiredType = possibleEnumType.toCType(bitsNeeded);
            if (dispatcher.hasType(desiredType)) {
                return desiredType;
            }
        }

        dispatcher.addExternalDeclarationToEmitBefore(
                "#include <ssclib/headers/core/types.h>"
        );

        final Symbol globalScope = dispatcher.data.symbolTable().getGlobalScope();

        final String finalType = FROM_SSCLIB_TYPES.toCType(bitsNeeded);

        final HashSet<TypeClassification> typeSpecifierSet = new HashSet<>();
        typeSpecifierSet.add(TypeClassification.TypeSpecifier_);

        for (final int bits : POSSIBLE_BIT_VALUES) {
            final Symbol uInt = new Symbol();
            uInt.setClassification(typeSpecifierSet);

            uInt.setName(FROM_SSCLIB_TYPES.toCType(bits));

            dispatcher.data.symbolTable().defineInScope(globalScope, uInt);
        }

        return finalType;
    }

    private byte getDistinctFlags(SSCParser.FlagsSpecifierContext ctx,
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
                assert currValue == 0;
            } else {
                assert initializer.Assign() == null;
                currValue = nextValue;
                nextValue *= 2;
                assert SSCCUtil.Maths.isPowerOfTwo(nextValue);
            }

            if (valuesMap.put(currentFlagIdentifier, currValue) != null) {
                throw dispatcher.getSSCLanguageException("Duplicate identifier in flags specifier", initializer);
            }

            ++distinctCount;
            if (distinctCount > 64) {
                throw dispatcher.getSSCLanguageException("Too many flags in flags specifier (max is 64)", ctx);
            }
        }
        return distinctCount;
    }

    private long getFlagValueNumeric(SSCParser.FlagsInitializerContext initializer) {
        final String string = dispatcher
                .visitTerminal(initializer.IntegerConstant())
                .toLowerCase();
        for (String value : POSSIBLE_INTEGER_INITIALIZERS) {
            if (string.equals(value)) {
                return 0;
            }
        }

        throw dispatcher.getSSCLanguageException(
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
                throw dispatcher.getSSCLanguageException(
                        "Flags may only be initialized with values from the same set", ctx
                );
            }
            total |= value;
        }
        return total;
    }
}

package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.ssc.compiler.data.ss.Function;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.List;
import java.util.StringJoiner;

public class SuperstructInterfaceConvertor extends AbstractConvertor<SSCParser.SuperStructInterfaceContext> {
    public SuperstructInterfaceConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * 1. Discard `object ‹Ident› interface {}` tokens
     * 2. Register superstruct if not already
     * 3. For all functions:
     * 3.1. Qualify name
     * 3.2. Add self-ref (if applicable)
     */
    @Override
    public String convert(SSCParser.SuperStructInterfaceContext ctx) {
        Main.logger.printDebug("Entering Superstruct Interface");

        final StringJoiner joiner = new StringJoiner(System.lineSeparator());

        final String ssName = dispatcher.visitTerminal(ctx.Identifier());
        joiner.add("/* Superstruct Interface `" + ssName + "`; START */");

        final SuperStruct interfaceOf = dispatcher.data.superStructs()
                .computeIfAbsent(ssName, SuperStruct::new);

        joiner.add(
                /* declare the struct to be able to use it in the function declarations */
                interfaceOf.emitStructDeclaration()
        );

        dispatcher.data.setCurrentSuperstruct(interfaceOf);
        Main.logger.printDebug("Added a new superstruct from interface: " + ssName);

        // attributeSpecifierSequence? cDeclarationSpecifiers? declarator ';'
        for (SSCParser.FunctionHeaderContext context : ctx.functionHeader()) {
            final List<SSCParser.DeclarationSpecifierContext> declSpecs = context.declarationSpecifiers() == null
                    ? List.of()
                    : context.declarationSpecifiers().declarationSpecifier();
            final List<SSCParser.DeclarationSpecifierContext> noPrivateSpecs = declSpecs
                    .stream()
                    .filter(declSpec -> declSpec.functionSpecifier() == null
                            || declSpec.functionSpecifier().Private() == null)
                    .toList();

            final boolean isPrivate = SuperstructConvertor.hasDeclarationSpecifier(
                    declSpecs,
                    ds -> ds.functionSpecifier() != null && ds.functionSpecifier().Private() != null
            );

            final FunctionHeaderData result = SuperstructConvertor.getFunctionHeaderData(
                    dispatcher, context, declSpecs, noPrivateSpecs
            );

            final Function functionDefinition = new Function(
                    result,
                    isPrivate,
                    SuperstructConvertor.parseType(dispatcher, declSpecs, result.declarator()),
                    SuperstructConvertor.parseFunctionParameters(dispatcher, result.declarator()),
                    null,
                    ssName
            );
            interfaceOf.addFunction(functionDefinition);

            joiner.add(functionDefinition.getDeclaration());
        }

        dispatcher.data.setCurrentSuperstruct(null);

        joiner.add("/* Superstruct Interface `" + ssName + "`; END */");
        return joiner.toString();
    }
}

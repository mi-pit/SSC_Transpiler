package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.List;
import java.util.StringJoiner;

public class SuperstructInterfaceConvertor extends AbstractConvertor<SSCParser.SuperStructInterfaceContext> {
    public SuperstructInterfaceConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher);
    }

    /**
     * <ol>
     *  <li> Discard `object ‹Ident› interface {}` tokens </li>
     *      <li> Register superstruct if not already </li>
     *      <li> For all functions:
     *          <ol>
     *              <li> Qualify name </li>
     *              <li> Add self-ref (if applicable) </li
     *          </ol>
     *  </li>
     * </ol>
     */
    @Override
    public String convert(SSCParser.SuperStructInterfaceContext ctx) {
        // TODO: check if methods declared here are implemented
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

            final boolean isPrivate = declSpecs.stream().anyMatch(
                    ds -> ds.functionSpecifier() != null && ds.functionSpecifier().Private() != null
            );

            final FunctionHeaderData result = FunctionHeaderData.getFunctionHeaderData(
                    dispatcher, context, declSpecs
            );

            final SuperstructMethod functionDefinition = new SuperstructMethod(
                    dispatcher,
                    result,
                    isPrivate,
                    SuperstructConvertor.parseFunctionParameters(dispatcher, result.declarator()),
                    null
            );
            interfaceOf.addFunction(functionDefinition);

            joiner.add(functionDefinition.getDeclaration());
        }

        dispatcher.data.setCurrentSuperstruct(null);

        joiner.add("/* Superstruct Interface `" + ssName + "`; END */");
        return joiner.toString();
    }
}

package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionHeaderData;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class SuperstructInterfaceConvertor extends AbstractConvertor<SSCParser.SuperStructInterfaceContext> {
    private final Map<String, SSCParser.SuperStructInterfaceContext> interfaceContexts = new HashMap<>();
    private int COUNTER = 0;

    public SuperstructInterfaceConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SuperStructInterfaceContext.class);
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
        Main.logger.printDebug("Entering Superstruct Interface");

        final StringJoiner joiner = new StringJoiner(System.lineSeparator());

        final String ssName = dispatcher.visit(ctx.Identifier());
        joiner.add("/* Superstruct Interface `" + ssName + "`; START */");

        if (interfaceContexts.containsKey(ssName)) {
            throw dispatcher.getSSCCallbackException(
                    "Interface of superstruct '" + ssName + "' is already defined",
                    ctx.Identifier(), interfaceContexts.get(ssName).Identifier()
            );
        }

        final SuperStruct interfaceOf = dispatcher.data.superStructs()
                .computeIfAbsent(ssName, name -> {
                    final SuperStruct ss = new SuperStruct(name);
                    interfaceContexts.put(name, ctx);
                    return ss;
                });

        joiner.add(
                /* declare the struct to be able to use it in the function declarations */
                interfaceOf.emitStructDeclaration() + ";"
        );

        dispatcher.data.pushSuperstruct(interfaceOf);
        Main.logger.printDebug("Added a new superstruct from interface: " + ssName);

        // attributeSpecifierSequence? cDeclarationSpecifiers? declarator ';'
        for (SSCParser.FunctionHeaderContext fnHeaderCtx : ctx.functionHeader()) {
            final List<SSCParser.DeclarationSpecifierContext> declSpecs = fnHeaderCtx.declarationSpecifiers() == null
                    ? List.of()
                    : fnHeaderCtx.declarationSpecifiers().declarationSpecifier();

            final FunctionHeaderData functionData = FunctionHeaderData.parse(dispatcher, fnHeaderCtx, declSpecs);

            // Fixme: think of an elegant solution to avoid interface parameters
            //  getting attributed to their surrounding functions
            dispatcher.pushFunction(
                    "<function interface>: '" + functionData.unqualifiedName()
                    + "' (" + COUNTER++ + ")"
            );

            final SuperstructMethod functionDefinition = new SuperstructMethod(
                    dispatcher,
                    interfaceOf,
                    functionData,
                    declSpecs.stream().anyMatch(ds ->
                            ds.superstructMemberDeclarationSpecifier() != null && ds.superstructMemberDeclarationSpecifier().Private() != null
                    ),
                    SuperstructConvertor.parseFunctionParameters(dispatcher, functionData.declarator()),
                    null
            );
            interfaceOf.addFunction(functionDefinition);

            joiner.add(functionDefinition.getDeclaration());

            dispatcher.popFunction();
        }

        dispatcher.data.popSuperstruct();

        joiner.add("/* Superstruct Interface `" + ssName + "`; END */");
        return joiner.toString();
    }
}

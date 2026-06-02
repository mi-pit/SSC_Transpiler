package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class SuperstructInterfaceConvertor extends AbstractConvertor<SSCParser.SuperStructInterfaceContext> {
    private int counter = 0;
    private final Map<String, SSCParser.SuperStructInterfaceContext> interfaceContexts = new HashMap<>();

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

        final String ssName = dispatcher.visit(ctx.Identifier());

        final StringJoiner joiner = new StringJoiner(
                ";\n",
                "/* Superstruct Interface `" + ssName + "`; START */\n",
                ";/* Superstruct Interface `" + ssName + "`; END */\n"
        );

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
                interfaceOf.emitStructDeclaration()
        );

        dispatcher.data.pushSuperstruct(interfaceOf);
        Main.logger.printDebug("Added a new superstruct from interface: " + ssName);

        // attributeSpecifierSequence? cDeclarationSpecifiers? declarator ';'
        ctx.functionHeader()
                .stream()
                .map(fh -> {
                    final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(fh.declarator());
                    final String unqualifiedName = dispatcher.visit(identifier);

                    dispatcher.pushFunction(
                            "<function interface>: '" + unqualifiedName
                            + "' (" + counter++ + ")",
                            null
                    );

                    final String s = dispatcher.visit(fh);

                    dispatcher.popFunction();
                    return s;
                })
                .forEach(joiner::add);

        dispatcher.data.popSuperstruct();

        return joiner.toString();
    }
}

package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.FunctionSSCData;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class SuperstructInterfaceConvertor extends AbstractConvertor<SSCParser.SuperStructInterfaceContext> {
    private static final AtomicInteger counter = new AtomicInteger();
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
                ";\n/* Superstruct Interface `" + ssName + "`; END */\n"
        );

        if (interfaceContexts.containsKey(ssName)) {
            throw dispatcher.getSSCCallbackException(
                    "Interface of superstruct '" + ssName + "' is already defined",
                    ctx.Identifier(),
                    interfaceContexts.get(ssName).Identifier()
            );
        }

        final SuperStruct interfaceOf = getOrDefineSuperstruct(ssName, ctx);

        joiner.add(
                /* declare the struct to be able to use it in the function declarations */
                interfaceOf.emitStructDeclaration()
        );

        Main.logger.printDebug("Added a new superstruct from interface: " + ssName);

        dispatcher.state.pushSuperstruct(interfaceOf, ctx);

        // attributeSpecifierSequence? cDeclarationSpecifiers? declarator ';'
        ctx.functionHeader()
                .stream()
                .map(getHeaderVisitor(interfaceOf))
                .forEach(joiner::add);

        dispatcher.state.popSuperstruct();

        return joiner.toString();
    }

    private Function<SSCParser.FunctionHeaderContext, String> getHeaderVisitor(
            final SuperStruct interfaceOf
    ) {
        return fh -> {
            final TerminalNode identifier = SSCCUtil.getIdentifierFromDeclarator(fh.declarator());
            final String unqualifiedName = dispatcher.visit(identifier);

            dispatcher.pushFunction(
                    "<function interface (" + counter.getAndIncrement() + ")>: '" + unqualifiedName + "'",
                    null
            );
            final String s = dispatcher.visit(fh);
            dispatcher.popFunction();

            final SuperstructMethod fn = SuperstructMethod.header(
                    FunctionSSCData.fromDeclarationSpecifiers(
                            fh.declarationSpecifiers().declarationSpecifier()
                    ),
                    unqualifiedName,
                    s,
                    fh
            );

            interfaceOf.declareMethod(fn);

            return s;
        };
    }

    private SuperStruct getOrDefineSuperstruct(String ssName, SSCParser.SuperStructInterfaceContext ctx) {
        final SuperStruct interfaceOf = dispatcher.state.getSuperstruct(ssName);
        if (interfaceOf != null) {
            return interfaceOf;
        }

        final SuperStruct newSS = new SuperStruct(ssName);
        interfaceContexts.put(ssName, ctx);
        return newSS;
    }
}

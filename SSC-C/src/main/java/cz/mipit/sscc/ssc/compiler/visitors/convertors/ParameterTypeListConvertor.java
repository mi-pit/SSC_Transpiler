package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import cz.mipit.sscc.util.collection.Enumerated;
import cz.mipit.sscc.util.collection.Enumerator;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ParameterTypeListConvertor extends AbstractConvertor<SSCParser.ParameterTypeListContext> {
    public ParameterTypeListConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.ParameterTypeListContext.class);
    }

    // parameterTypeList
    //    : parameterList (',' '...')?
    //    | '...'
    @Override
    public String convert(SSCParser.ParameterTypeListContext ctx) {
        if (dispatcher.getCurrentFunctionName() == null || dispatcher.state.getCurrentFunctionSSCData() == null
        ) {
            return dispatcher.visitSuper(ctx);
        }

        final Optional<SuperStruct> maybeCurrentSuperstruct = dispatcher.state.currentSuperstruct();
        if (maybeCurrentSuperstruct.isEmpty()) {
            return dispatcher.visitSuper(ctx);
        }

        final SuperStruct superstruct = maybeCurrentSuperstruct.get();

        if (dispatcher.state.getCurrentFunctionSSCData().isMeta()) {
            return dispatcher.visitSuper(ctx);
        }

        final List<String> parameterListList = new ArrayList<>();

        registerSelfReferenceVariable(superstruct, ctx);

        final StringBuilder selfReference = new StringBuilder();
        if (dispatcher.state.getCurrentFunctionSSCData().isPure()) {
            selfReference.append("const ");
        }
        selfReference
                .append("struct ")
                .append(superstruct.name())
                .append(" *const this");

        parameterListList.add(selfReference.toString());

        dispatcher.state.setCurrentFunctionSSCData(null);


        // parameterDeclaration (',' parameterDeclaration)*
        final List<SSCParser.ParameterDeclarationContext> parameterDeclaration = ctx.parameterList().parameterDeclaration();
        for (final Enumerated<SSCParser.ParameterDeclarationContext> iParam : Enumerator.of(parameterDeclaration)) {
            if ("void".equals(dispatcher.getLiteral(iParam.item()))) {
                if (iParam.index() != 0) {
                    throw dispatcher.getSSCLanguageException(
                            "Void parameter", iParam.item()
                    );
                }
                continue;
            }

            final String s = dispatcher.visit(iParam.item());

            // parameterDeclaration may be empty for the first context if there are no parameters
            if (s.isBlank()) {
                if (iParam.index() != 0) {
                    throw dispatcher.getSSCLanguageException(
                            "Blank parameter", iParam.item()
                    );
                }
                break;
            }

            parameterListList.add(s);
        }

        if (ctx.Ellipsis() != null) {
            parameterListList.add(dispatcher.visit(ctx.Ellipsis()));
        }

        assert parameterListList.stream().noneMatch(String::isBlank);

        if (parameterListList.isEmpty()) {
            parameterListList.add("void");
        }

        return String.join(", ", parameterListList);
    }


    private void registerSelfReferenceVariable(
            final SuperStruct superstruct,
            final ParseTree ctx
    ) {
        final SuperstructVariable selfReferenceVariable =
                new SuperstructVariable(superstruct, Pointer.oneConst(), "this");

        dispatcher.state.addSuperstructVariable(selfReferenceVariable, ctx);
    }
}

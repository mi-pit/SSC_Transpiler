package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
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
        for (final SSCParser.ParameterDeclarationContext param : parameterDeclaration) {
            final String s = dispatcher.visit(param);

            // parameterDeclaration may be empty for the first context if there are no parameters
            if (s.isBlank()) {
                break;
            }

            if ("void".equals(dispatcher.getLiteral(param))) {
                continue;
            }

            parameterListList.add(s);
        }

        if (ctx.Ellipsis() != null) {
            parameterListList.add(dispatcher.visit(ctx.Ellipsis()));
        }

        if (parameterListList.stream().anyMatch(String::isBlank))
            throw new AssertionError();

        if (parameterListList.isEmpty()) {
            parameterListList.add("void");
        }

        return String.join(", ", parameterListList);
    }


    private void registerSelfReferenceVariable(
            SuperStruct superstruct, ParseTree ctx
    ) {
        final SuperstructVariable selfReferenceVariable =
                new SuperstructVariable(superstruct.name(), Pointer.oneConst(), "this");

        dispatcher.state.addFunctionVariable(selfReferenceVariable, ctx);
    }
}

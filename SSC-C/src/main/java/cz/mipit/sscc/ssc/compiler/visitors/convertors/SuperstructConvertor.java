package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SuperstructConvertor extends AbstractConvertor<SSCParser.SuperStructSpecifierContext> {
    private final Map<SuperStruct, SSCParser.SuperStructSpecifierContext> superstructContexts = new HashMap<>();

    public SuperstructConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.SuperStructSpecifierContext.class);
    }

    @Override
    public String convert(SSCParser.SuperStructSpecifierContext ctx) {
        final String thisSSName = dispatcher.visit(ctx.Identifier());

        dispatcher.state.registerSuperstructIfNotDefined(thisSSName, ctx);

        if (ctx.superStructBody() == null) {
            return dispatcher.visitSuper(ctx);
        }
        Main.logger.printDebug("Entering Superstruct Body");

        final SuperStruct superStruct = Objects.requireNonNull(dispatcher.state.getSuperstruct(thisSSName));

        if (superStruct.isDefined()) {
            throw dispatcher.getSSCCallbackException(
                    "Superstruct with name '" + thisSSName + "' already exists",
                    ctx.Identifier(), superstructContexts.get(superStruct).Identifier()
            );
        }
        superStruct.setDefined();

        superstructContexts.put(superStruct, ctx);

        dispatcher.state.pushSuperstruct(superStruct, ctx);

        for (SSCParser.SuperStructMemberContext memberCtx : ctx.superStructBody().superStructMember()) {
            dispatcher.visit(memberCtx);
        }

        dispatcher.state.popSuperstruct();

        final String structDeclaration = superStruct.emitStructDeclaration();
        final String structDefinition = superStruct.emitStructDefinition();
        final String methodDeclarations = superStruct.emitMethodDeclarations();
        final String methodDefinitions = superStruct.emitMethodDefinitions(
                fnDef -> dispatcher.getSSCLanguageException(
                        "Method '" + fnDef.name() + "' in superstruct '" + thisSSName + "' is not defined.", fnDef.context()
                )
        );

        dispatcher.addExternalDeclarationToEmitBefore("""
                /* SuperstructSpecifier -- definition; START */
                %s
                /* SuperstructSpecifier -- definition; END */
                """.formatted(structDefinition)
        );
        dispatcher.addExternalDeclarationToEmitBefore("""
                /* SuperstructSpecifier -- method declarations; START */
                %s
                /* SuperstructSpecifier -- method declarations; END */
                """.formatted(methodDeclarations)
        );

        dispatcher.addExternalDeclarationToEmitAfter(
                """
                        /* SuperstructSpecifier -- method definitions; START */
                        %s
                        /* SuperstructSpecifier -- method definitions; END */
                        """.formatted(methodDefinitions)
        );

        return structDeclaration;
    }
}

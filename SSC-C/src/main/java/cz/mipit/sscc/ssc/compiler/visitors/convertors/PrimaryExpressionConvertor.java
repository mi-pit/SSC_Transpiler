package cz.mipit.sscc.ssc.compiler.visitors.convertors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperstructMethod;
import cz.mipit.sscc.ssc.compiler.visitors.VisitorDispatcher;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Optional;
import java.util.stream.Collectors;

public class PrimaryExpressionConvertor
        extends AbstractConvertor<SSCParser.PrimaryExpressionContext> {
    public PrimaryExpressionConvertor(VisitorDispatcher dispatcher) {
        super(dispatcher, SSCParser.PrimaryExpressionContext.class);
    }

    @Override
    public String convert(SSCParser.PrimaryExpressionContext ctx) {
        if (ctx.DoubleColon() == null) {
            return dispatcher.visitSuper(ctx);
        }
        Main.logger.printDebug(() -> "Double colon in '" + dispatcher.getLiteral(ctx) + "'");

        if (ctx.Identifier().isEmpty() || ctx.Identifier().size() > 2) {
            throw dispatcher.getSSCLanguageException(
                    "Invalid number of identifiers", ctx
            );
        }

        final int identIdx;
        final String superstructName;
        final ParseTree firstChildCtx; // exception arg
        if (ctx.templateDispatch() != null) {
            superstructName = dispatcher.visit(ctx.templateDispatch());
            identIdx = 0;
            firstChildCtx = ctx.templateDispatch();
        } else {
            superstructName = dispatcher.visit(ctx.Identifier().getFirst());
            identIdx = 1;
            firstChildCtx = ctx.Identifier().getFirst();
        }

        if (ctx.Identifier().size() != identIdx + 1) {
            throw dispatcher.getSSCLanguageException(
                    "Invalid number of identifiers", ctx
            );
        }
        final String superstructMethodName = dispatcher.visit(ctx.Identifier().getLast());

        final SuperStruct superstruct = dispatcher.state.getSuperstruct(superstructName);
        if (superstruct == null) {
            Main.logger.printDebug(
                    () -> "Superstruct '" + superstructName + "' not found. " +
                          "Available superstructs: " + dispatcher.state.visibleSuperstructs()
                                  .stream()
                                  .map(SuperStruct::name)
                                  .collect(Collectors.joining(", "))
            );
            throw dispatcher.getSSCLanguageException(
                    "Could not find superstruct '" + superstructName + "'",
                    firstChildCtx
            );
        }

        verifyStaticCall(ctx, superstruct, superstructName, superstructMethodName);

        return superstruct.qualifyName(superstructMethodName);
    }

    private void verifyStaticCall(final ParserRuleContext ctx,
                                  final SuperStruct superstruct,
                                  final String className,
                                  final String methodName) {
        if (!className.equals(superstruct.name()))
            throw new AssertionError();

        final Optional<SuperStruct> maybeCurrentSS = dispatcher.state.currentSuperstruct();
        if (maybeCurrentSS.isPresent() && maybeCurrentSS.get().name().equals(className)) {
            return;
        }

        final Optional<SuperstructMethod> maybeMethod = superstruct.findMethod(methodName);
        if (maybeMethod.isEmpty()) {
            throw dispatcher.getSSCLanguageException("Superstruct '" + className
                                                     + "' has no meta method called '" + methodName
                                                     + "'", ctx);
        }
        final SuperstructMethod method = maybeMethod.get();

        if (method.metadata().isPrivate()) {
            Main.logger.printDebug(() -> "Method '" + methodName + "' is private. Going to check if it may be used here...");
            if (maybeCurrentSS.isEmpty()
                || !maybeCurrentSS.get().name().equals(className)) {
                throw dispatcher.getSSCLanguageException(
                        "Cannot access private static method `" + methodName + "` from outside the superstruct",
                        ctx
                );
            }
        }
    }
}

package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.AbstractConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.EmittingConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FlagsConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FunctionDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.LambdaConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.PostfixExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TernaryOperatorConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.data.CompilerData;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.Either;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;


public class VisitorDispatcher extends BaseConvertorVisitor {
    private final AbstractConvertor<SSCParser.PostfixExpressionContext> postfixExpressionConvertor;
    private final AbstractConvertor<SSCParser.FunctionDefinitionContext> functionConvertor;
    private final AbstractConvertor<SSCParser.ConditionalExpressionContext> ternaryOperatorConvertor;
    private final AbstractConvertor<SSCParser.FlagsSpecifierContext> flagsConvertor;

    private final EmittingConvertor<SSCParser.LambdaFunctionContext> lambdaConvertor;
    private final EmittingConvertor<SSCParser.SuperStructSpecifierContext> superstructConvertor;

    private final VariableCollector collector;

    public final CompilerData data;

    public VisitorDispatcher(CommonTokenStream tokens, InputFile currentFile) {
        super(tokens, currentFile);

        data = new CompilerData();

        collector = new VariableCollector(this);

        flagsConvertor = new FlagsConvertor(this);
        lambdaConvertor = new LambdaConvertor(this);
        ternaryOperatorConvertor = new TernaryOperatorConvertor(this);
        superstructConvertor = new SuperstructConvertor(this);
        functionConvertor = new FunctionDefinitionConvertor(this);
        postfixExpressionConvertor = new PostfixExpressionConvertor(this);
    }

    public String visitSuper(final ParserRuleContext ctx) {
        return visitChildren(ctx);
    }

    @Override
    public String visitSuperStructSpecifier(final SSCParser.SuperStructSpecifierContext ctx) {
        return superstructConvertor.convert(ctx);
    }

    public String super_visitSuperStructSpecifier(final SSCParser.SuperStructSpecifierContext ctx) {
        return super.visitSuperStructSpecifier(ctx);
    }

    @Override
    public String visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
        return functionConvertor.convert(ctx);
    }

    public String super_visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
        return super.visitFunctionDefinition(ctx);
    }

    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        return postfixExpressionConvertor.convert(ctx);
    }

    public String super_visitPostfixExpression(final SSCParser.PostfixExpressionContext ctx) {
        return super.visitPostfixExpression(ctx);
    }

    @Override
    public String visitConditionalExpression(SSCParser.ConditionalExpressionContext ctx) {
        return ternaryOperatorConvertor.convert(ctx);
    }

    public String super_visitConditionalExpression(final SSCParser.ConditionalExpressionContext ctx) {
        return super.visitConditionalExpression(ctx);
    }

    @Override
    public String visitFlagsSpecifier(SSCParser.FlagsSpecifierContext ctx) {
        return flagsConvertor.convert(ctx);
    }

    public String super_visitFlagsSpecifier(final SSCParser.FlagsSpecifierContext ctx) {
        return super.visitFlagsSpecifier(ctx);
    }


    @Override
    public String visitLambdaFunction(SSCParser.LambdaFunctionContext ctx) {
        return lambdaConvertor.convert(ctx);
    }

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        final String external = super.visitExternalDeclaration(ctx);

        // Emit lambda definitions right after leaving external declaration to have the proper scope.
        // lambdas are not themselves function definitions so they do not exit here
        final Optional<String> lambdas = lambdaConvertor.emit();

        // if superstruct convertor has methods => external is a super struct declaration
        // methods must be defined AFTER the struct
        final Optional<String> ssMethods = superstructConvertor.emit();

        final StringBuilder res = new StringBuilder();
        lambdas.ifPresent(res::append);
        res.append(external);
        ssMethods.ifPresent(res::append);
        return res.toString();
    }

    @Override
    public String visitDeclaration(SSCParser.DeclarationContext ctx) {
        collector.collect(ctx);
        return super.visitDeclaration(ctx);
    }


    public void pushFunction(String name, Supplier<SSCSyntaxException> supplier) {
        data.functionStack().push(name);
        if (data.functionVariables().put(name, new HashSet<>()) != null) {
            throw supplier.get();
        }
    }

    public void popFunction() {
        data.functionStack().pop();
    }

    public String getCurrentFunctionName() {
        return data.functionStack().peek();
    }

    public void addFunctionVariable(SuperstructVariable v) {
        data.functionVariables().get(getCurrentFunctionName()).add(v);
    }


    /* ==== GETTERS ==== */

    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final String ssName,
            final SSCParser.DeclaratorContext declarator
    ) {
        return tryCreateSuperstructVariableFromDeclarator(ssName, 0, declarator);
    }

    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final Typedef<SuperStruct> typedef,
            final SSCParser.DeclaratorContext declarator
    ) {
        return tryCreateSuperstructVariableFromDeclarator(
                typedef.getRepresentedType().name(),
                typedef.pointer(),
                declarator
        );
    }

    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final String ssName,
            final int pointerBase,
            final SSCParser.DeclaratorContext declarator
    ) {
        if (declarator == null) {
            return Optional.empty();
        }
        final var directDecl = declarator.directDeclarator();
        if (directDecl.Identifier() == null) {
            return Optional.empty();
        }

        final int declaratorPointer = SSCCUtil.getPointerLevel(declarator);
        final String varName = this.visitTerminal(directDecl.Identifier());

        final SuperstructVariable ssVar = new SuperstructVariable(ssName, pointerBase + declaratorPointer, varName);
        return Optional.of(ssVar);
    }

    public Optional<Either<String, Typedef<SuperStruct>>> findSSNameInDeclSpecs(
            final List<SSCParser.DeclarationSpecifierContext> declSpecs
    ) {
        for (final var declSpec : declSpecs) {
            if (declSpec.typeSpecifier() == null) {
                continue;
            }

            final var typeSpec = declSpec.typeSpecifier();
            if (typeSpec.superStructSpecifier() == null) {
                final Typedef<SuperStruct> typedef = data.superstructTypedefs().get(this.visitTypeSpecifier(typeSpec));
                if (typedef == null) {
                    continue;
                }
                return Optional.of(Either.right(typedef));
            }

            final var ssCtx = typeSpec.superStructSpecifier();
            final String ssName = this.visitTerminal(ssCtx.Identifier());
            return Optional.of(Either.left(ssName));
        }

        return Optional.empty();
    }

    /**
     * Always returns a valid superstruct.
     * If one is not found -> throw.
     */
    public SuperStruct getSuperStructFromVariable(final SSCParser.PostfixExpressionContext ctx,
                                                  final SuperstructVariable var) {
        final SuperStruct optSS = data.superStructs().get(var.ssName());
        if (optSS == null) {
            throw getSSCSyntaxException(
                    "`superstruct " + var.ssName() + "` "
                            + "(type of variable \"" + var.getName() + "\") is not properly defined",
                    ctx
            );
        }
        return optSS;
    }

    public Optional<SuperStruct> findSuperstructByName(final String className) {
        return Optional.ofNullable(data.superStructs().get(className));
    }

    public Optional<SuperstructVariable> findSuperstructVariable(String functionName, String objectName) {
        for (SuperstructVariable var : data.functionVariables().get(functionName)) {
            if (var.getName().equals(objectName)) {
                return Optional.of(var);
            }
        }
        /* check global variables too */
        for (SuperstructVariable var : data.functionVariables().get(null)) {
            if (var.getName().equals(objectName)) {
                return Optional.of(var);
            }
        }
        return Optional.empty();
    }


    public void debugPrintFunctionVariables() {
        for (final Map.Entry<@Nullable String, Set<SuperstructVariable>> entry : data.functionVariables().entrySet()) {
            final String funcName = entry.getKey();
            final Set<SuperstructVariable> variables = entry.getValue();
            if (variables.isEmpty()) {
                continue;
            }

            Main.logger.printDebug(() -> "For scope " + (funcName == null ? "global" : "'" + funcName + "'"));
            for (final SuperstructVariable variable : variables) {
                Main.logger.printDebug(() -> "        " + variable);
            }
        }
    }

    public SSCSyntaxException getSSCSyntaxException(String message, ParserRuleContext ctx) {
        return super.getSSCSyntaxException(message, ctx);
    }

    public String getLiteral(final ParserRuleContext ctx) {
        return SSCCUtil.Text.getLiteral(ctx, tokens);
    }

    public InputFile getCurrentFile() {
        return currentFile;
    }
}

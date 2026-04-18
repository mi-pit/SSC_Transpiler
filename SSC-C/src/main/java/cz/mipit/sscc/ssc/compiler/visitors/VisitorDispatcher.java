package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.Main;
import cz.mipit.sscc.file.InputFile;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.Convertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FlagsConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FunctionDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.LambdaConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.PostfixExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TernaryOperatorConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.data.CompilerData;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.Either;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;


public class VisitorDispatcher extends BaseConvertorVisitor {
    public final CompilerData data;

    private final Convertor<SSCParser.PostfixExpressionContext> postfixExpressionConvertor;
    private final Convertor<SSCParser.FunctionDefinitionContext> functionConvertor;
    private final Convertor<SSCParser.ConditionalExpressionContext> ternaryOperatorConvertor;
    private final Convertor<SSCParser.FlagsSpecifierContext> flagsConvertor;

    private final LambdaConvertor lambdaConvertor;
    private final SuperstructConvertor superstructConvertor;
    private final TemplateDefinitionConvertor templateConvertor;


    private final VariableCollector collector;

    public final List<String> methodsToEmit;
    private final Map<String, String> templateTypedefs;


    public VisitorDispatcher(CommonTokenStream tokens, InputFile currentFile) {
        super(tokens, currentFile);

        data = new CompilerData();

        collector = new VariableCollector(this);

        flagsConvertor = new FlagsConvertor(this);
        ternaryOperatorConvertor = new TernaryOperatorConvertor(this);
        functionConvertor = new FunctionDefinitionConvertor(this);
        postfixExpressionConvertor = new PostfixExpressionConvertor(this);

        superstructConvertor = new SuperstructConvertor(this);
        lambdaConvertor = new LambdaConvertor(this);
        templateConvertor = new TemplateDefinitionConvertor(this);

        methodsToEmit = new ArrayList<>();
        templateTypedefs = new HashMap<>();
    }

    public String visitSuper(final ParserRuleContext ctx) {
        return super.visitChildren(ctx);
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
    public String visitTemplateDispatch(SSCParser.TemplateDispatchContext ctx) {
        return templateConvertor.convertTemplateDispatch(ctx);
    }

    @Override
    public String visitFunctionTemplateDefinition(SSCParser.FunctionTemplateDefinitionContext ctx) {
        templateConvertor.visitTemplateDefinition(ctx);

        // templates only exist when called
        return "";
    }

    @Override
    public String visitLambdaFunction(SSCParser.LambdaFunctionContext ctx) {
        return lambdaConvertor.convert(ctx);
    }

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        final String external = super.visitExternalDeclaration(ctx);

        final StringBuilder res = new StringBuilder();

        res.append(external);

        // if superstruct convertor has methods => external is a super struct declaration
        // methods must be defined AFTER the struct
        superstructConvertor.emit().ifPresent(res::append);

        final String emitted = String.join(System.lineSeparator(), methodsToEmit);
        methodsToEmit.clear();

        return emitted + res;
    }

    @Override
    public String visitDeclaration(SSCParser.DeclarationContext ctx) {
        collector.collect(ctx);
        final String converted = super.visitDeclaration(ctx);

        final StringBuilder builder = new StringBuilder(converted);

        superstructConvertor.emitDeclarations().ifPresent(builder::append);

        return builder.toString();
    }

    @Override
    public String visitTypeSpecifier(SSCParser.TypeSpecifierContext ctx) {
        if (getCurrentFunctionName() == null
                || !(ctx.getChild(0) instanceof SSCParser.TypedefNameContext typedefName)) {
            return super.visitTypeSpecifier(ctx);
        }

        return templateTypedefs.getOrDefault(
                visitTypedefName(typedefName),
                super.visitTypeSpecifier(ctx)
        );
    }

    /* ==== DATA ==== */

    public void addTemplateTypedef(String origName, String newName) {
        Objects.requireNonNull(getCurrentFunctionName(), "Cannot add typedefs to a non-existent function");
        templateTypedefs.put(origName, newName);
    }

    /**
     * @param name     (fully qualified if applicable) function name
     * @param supplier supplier for an exception in case name was already in use
     */
    public void pushFunction(String name, Supplier<SSCSyntaxException> supplier) {
        data.functionStack().push(name);
        if (data.functionVariables().put(name, new HashSet<>()) != null) {
            if (supplier != null)
                throw supplier.get();
        }
    }

    public void pushFunction(String name, ParserRuleContext ctx) {
        pushFunction(
                name,
                () -> getSSCSyntaxException("Duplicate function name: '" + name + "'", ctx)
        );
    }

    public void popFunction() {
        data.functionStack().poll();
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

    public InputFile getCurrentFile() {
        return currentFile;
    }
}

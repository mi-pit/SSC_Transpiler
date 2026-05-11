package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import antlr.ssc.SymbolTable;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.Convertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FlagsConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FunctionDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.LambdaConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.PostfixExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructInterfaceConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateDispatchConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TernaryOperatorConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.data.CompilerData;
import cz.mipit.sscc.ssc.exceptions.children.SSCSyntaxException;
import cz.mipit.sscc.util.Either;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.VisitorInput;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static cz.mipit.sscc.Main.logger;


public class VisitorDispatcher extends BaseConvertorVisitor {
    public final CompilerData data;
    private final SymbolTable symbolTable; // TODO: move to data

    private final List<String> methodsToEmit; // to be emitted when exiting the next external declaration

    private final Convertor<SSCParser.PostfixExpressionContext> postfixExpressionConvertor;
    private final Convertor<SSCParser.FunctionDefinitionContext> functionConvertor;
    private final Convertor<SSCParser.ConditionalExpressionContext> ternaryOperatorConvertor;
    private final Convertor<SSCParser.FlagsSpecifierContext> flagsConvertor;

    private final Convertor<SSCParser.SuperStructInterfaceContext> superstructInterfaceConvertor;

    private final LambdaConvertor lambdaConvertor;
    private final SuperstructConvertor superstructConvertor;

    private final TemplateDispatchConvertor templateDispatchConvertor;
    private final TemplateDefinitionConvertor templateDefinitionConvertor;

    private final VariableCollector collector;


    public VisitorDispatcher(VisitorInput input) {
        super(input.tokens(), input.inputFile());

        data = new CompilerData();
        symbolTable = input.symbolTable();

        collector = new VariableCollector(this);

        flagsConvertor = new FlagsConvertor(this);
        ternaryOperatorConvertor = new TernaryOperatorConvertor(this);
        functionConvertor = new FunctionDefinitionConvertor(this);
        postfixExpressionConvertor = new PostfixExpressionConvertor(this);

        superstructInterfaceConvertor = new SuperstructInterfaceConvertor(this);

        superstructConvertor = new SuperstructConvertor(this);
        lambdaConvertor = new LambdaConvertor(this);

        templateDispatchConvertor = new TemplateDispatchConvertor(this);
        templateDefinitionConvertor = new TemplateDefinitionConvertor(this);

        methodsToEmit = new ArrayList<>();
    }

    public String visitSuper(final ParserRuleContext ctx) {
        return super.visitChildren(ctx);
    }


    @Override
    public String visitSuperStructSpecifier(final SSCParser.SuperStructSpecifierContext ctx) {
        return superstructConvertor.convert(ctx);
    }

    @Override
    public String visitSuperStructInterface(SSCParser.SuperStructInterfaceContext ctx) {
        return superstructInterfaceConvertor.convert(ctx);
    }

    @Override
    public String visitFunctionDefinition(final SSCParser.FunctionDefinitionContext ctx) {
        return functionConvertor.convert(ctx);
    }

    @Override
    public String visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        return postfixExpressionConvertor.convert(ctx);
    }

    @Override
    public String visitConditionalExpression(SSCParser.ConditionalExpressionContext ctx) {
        return ternaryOperatorConvertor.convert(ctx);
    }

    @Override
    public String visitFlagsSpecifier(SSCParser.FlagsSpecifierContext ctx) {
        return flagsConvertor.convert(ctx);
    }

    @Override
    public String visitTemplateDispatch(SSCParser.TemplateDispatchContext ctx) {
        return templateDispatchConvertor.convert(ctx);
    }

    @Override
    public String visitFunctionTemplateDefinition(SSCParser.FunctionTemplateDefinitionContext ctx) {
        return templateDefinitionConvertor.convert(ctx);
    }

    @Override
    public String visitLambdaFunction(SSCParser.LambdaFunctionContext ctx) {
        return lambdaConvertor.convert(ctx);
    }

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        final String external = super.visitExternalDeclaration(ctx);

        final StringBuilder res = new StringBuilder();
        // if superstruct convertor has methods => external is a super struct declaration
        // methods must be defined AFTER the struct
        superstructConvertor.emit().ifPresent(res::append);

        final String emitted = String.join(System.lineSeparator(), methodsToEmit);
        methodsToEmit.clear();

        return emitted + external + res;
    }

    @Override
    public String visitDeclaration(SSCParser.DeclarationContext ctx) {
        collector.collect(ctx);
        final String converted = super.visitDeclaration(ctx);

        final StringBuilder builder = new StringBuilder(converted);

        superstructConvertor.emitDeclarations().ifPresent(builder::append);

        return builder.toString();
    }


    /* ==== DATA ==== */

    public void addMethodToEmit(String method) {
        methodsToEmit.add(method);
    }

    public boolean hasType(String typeName) {
        return symbolTable.resolve(typeName) != null;
    }

    /**
     * @param name     (fully qualified if applicable) function name
     * @param supplier supplier for an exception in case name was already in use
     */
    public void pushFunction(String name, Supplier<SSCSyntaxException> supplier) {
        Objects.requireNonNull(name, "Function name cannot be null");

        if (data.functionVariables().put(name, new HashSet<>()) != null) {
            if (supplier != null)
                throw supplier.get();
        }

        data.functionStack().push(name);
    }

    public void pushFunction(String name, ParserRuleContext functionCtx) {
        pushFunction(
                name,
                () -> getSSCSyntaxException("Duplicate function name: '" + name + "'", functionCtx)
        );
    }

    public void popFunction() {
        data.functionStack().poll();
    }

    public String getCurrentFunctionName() {
        return data.functionStack().peek();
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
            if (var.getIdentifier().equals(objectName)) {
                return Optional.of(var);
            }
        }
        /* check global variables too */
        for (SuperstructVariable var : data.functionVariables().get(null)) {
            if (var.getIdentifier().equals(objectName)) {
                return Optional.of(var);
            }
        }
        return Optional.empty();
    }


    public void debugPrintDump() {
        logger.printDebug("");
        logger.printDebug("Dumping debug info...");

        for (final Map.Entry<@Nullable String, Set<SuperstructVariable>> entry : data.functionVariables().entrySet()) {
            final String funcName = entry.getKey();
            final Set<SuperstructVariable> variables = entry.getValue();
            if (variables.isEmpty()) {
                continue;
            }

            logger.printDebug(() -> "For scope " + (funcName == null ? "global" : "'" + funcName + "'"));
            for (final SuperstructVariable variable : variables) {
                logger.printDebug(() -> "        " + variable);
            }
        }

        for (final var entry : data.templates().entrySet()) {
            logger.printDebug(() -> entry.getValue().toString());
        }
    }

    public void addReplacements(Map<String, String> typeArgMap) {
        this.replacements.putAll(typeArgMap);
    }

    public void addReplacement(String key, String value) {
        this.replacements.put(key, value);
    }

    public void removeReplacements(Map<String, String> typeArgMap) {
        for (final String key : typeArgMap.keySet()) {
            this.replacements.remove(key);
        }
    }

    public void removeReplacement(String key) {
        this.replacements.remove(key);
    }
}

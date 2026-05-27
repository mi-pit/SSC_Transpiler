package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.Convertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FlagsConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FunctionDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.LambdaConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.PostfixExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.PrimaryExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructInterfaceConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateDispatchConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TernaryOperatorConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.fmt.FormattingConvertor;
import cz.mipit.sscc.ssc.exceptions.SSCTranspilerException;
import cz.mipit.sscc.ssc.exceptions.children.SSCLanguageException;
import cz.mipit.sscc.util.VisitorInput;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import static cz.mipit.sscc.Main.logger;


public class VisitorDispatcher extends FormattingConvertor {
    public final CompilerData data;

    private final List<String> externalDeclarationsToEmitBefore;
    private final List<String> externalDeclarationsToEmitAfter;

    private final Convertor<SSCParser.ConditionalExpressionContext> ternaryOperatorConvertor;
    private final Convertor<SSCParser.PostfixExpressionContext> postfixExpressionConvertor;
    private final Convertor<SSCParser.PrimaryExpressionContext> primaryExpressionConvertor;
    private final Convertor<SSCParser.FunctionDefinitionContext> functionConvertor;

    private final Convertor<SSCParser.FlagsSpecifierContext> flagsConvertor;
    private final Convertor<SSCParser.LambdaFunctionContext> lambdaConvertor;

    private final Convertor<SSCParser.TemplateDispatchContext> templateDispatchConvertor;
    private final Convertor<SSCParser.TemplateDefinitionContext> templateDefinitionConvertor;


    private final Convertor<SSCParser.SuperStructInterfaceContext> superstructInterfaceConvertor;
    private final Convertor<SSCParser.SuperStructSpecifierContext> superstructConvertor;


    private final VariableCollector collector;


    public VisitorDispatcher(VisitorInput input) {
        super(input.tokens(), input.inputFile());

        data = new CompilerData(input.symbolTable());

        collector = new VariableCollector(this);

        postfixExpressionConvertor = new PostfixExpressionConvertor(this);
        primaryExpressionConvertor = new PrimaryExpressionConvertor(this);
        functionConvertor = new FunctionDefinitionConvertor(this);
        ternaryOperatorConvertor = new TernaryOperatorConvertor(this);
        flagsConvertor = new FlagsConvertor(this);

        superstructInterfaceConvertor = new SuperstructInterfaceConvertor(this);

        superstructConvertor = new SuperstructConvertor(this);
        lambdaConvertor = new LambdaConvertor(this);

        templateDispatchConvertor = new TemplateDispatchConvertor(this);
        templateDefinitionConvertor = new TemplateDefinitionConvertor(this);

        externalDeclarationsToEmitBefore = new ArrayList<>();
        externalDeclarationsToEmitAfter = new ArrayList<>();
    }


    @Override
    public String visitSuperStructSpecifier(final SSCParser.SuperStructSpecifierContext ctx) {
        return superstructConvertor.convert(ctx);
    }

    public String super_visitSuperStructSpecifier(final SSCParser.SuperStructSpecifierContext ctx) {
        return super.visitSuperStructSpecifier(ctx);
    }

    @Override
    public String visitSuperStructInterface(SSCParser.SuperStructInterfaceContext ctx) {
        return superstructInterfaceConvertor.convert(ctx);
    }

    @Override
    public String visitPrimaryExpression(SSCParser.PrimaryExpressionContext ctx) {
        return primaryExpressionConvertor.convert(ctx);
    }

    public String super_visitPrimaryExpression(SSCParser.PrimaryExpressionContext ctx) {
        return super.visitPrimaryExpression(ctx);
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

    public String super_visitPostfixExpression(SSCParser.PostfixExpressionContext ctx) {
        return super.visitPostfixExpression(ctx);
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
    public String visitTemplateDefinition(SSCParser.TemplateDefinitionContext ctx) {
        return templateDefinitionConvertor.convert(ctx);
    }

    @Override
    public String visitLambdaFunction(SSCParser.LambdaFunctionContext ctx) {
        return lambdaConvertor.convert(ctx);
    }

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        final StringJoiner joiner = new StringJoiner(System.lineSeparator());

        final String external = super.visitExternalDeclaration(ctx);

        if (!externalDeclarationsToEmitBefore.isEmpty()) {
            joiner.add(
                    String.join(System.lineSeparator(), externalDeclarationsToEmitBefore)
            );
            externalDeclarationsToEmitBefore.clear();
        }

        joiner.add(external);

        if (!externalDeclarationsToEmitAfter.isEmpty()) {
            joiner.add(
                    String.join(System.lineSeparator(), externalDeclarationsToEmitAfter)
            );
            externalDeclarationsToEmitAfter.clear();
        }

        return joiner.toString();
    }

    @Override
    public String visitDeclaration(SSCParser.DeclarationContext ctx) {
        collector.collect(ctx);
        return super.visitDeclaration(ctx);
    }

    @Override
    public String visitParameterDeclaration(SSCParser.ParameterDeclarationContext ctx) {
        collector.collect(ctx);
        return super.visitParameterDeclaration(ctx);
    }


    /* ==== DATA ==== */

    public void addExternalDeclarationToEmitAfter(String code) {
        externalDeclarationsToEmitAfter.add(code);
    }

    public void addExternalDeclarationToEmitBefore(String code) {
        externalDeclarationsToEmitBefore.add(code);
    }

    public boolean hasType(String typeName) {
        return data.symbolTable().resolve(typeName) != null;
    }


    private final Map<String, ParserRuleContext> _functionDefinitions = new HashMap<>();

    public void pushFunction(String name, ParserRuleContext functionCtx) {
        Objects.requireNonNull(name, "Function name cannot be null");

        if (data.functionVariables().containsKey(name)) {
            class StackedException extends SSCTranspilerException {
                StackedException(SSCTranspilerException exception) {
                    super(
                            Type.Language,
                            exception.getMessage()
                            + System.lineSeparator()
                            + COLOR_LOCATOR
                            + "\tPrevious definition here: ",
                            _functionDefinitions.get(name),
                            tokens,
                            currentFile
                    );
                }
            }

            final SSCLanguageException exception = getSSCLanguageException("Duplicate function name: '" + name + "'", functionCtx);
            throw new StackedException(exception);
        }
        data.functionVariables().put(name, new HashSet<>());
        data.functionStack().push(name);
        _functionDefinitions.put(name, functionCtx);
    }

    public void pushFunction(String name) {
        pushFunction(name, null);
    }

    public void popFunction() {
        data.functionStack().poll();
    }

    public String getCurrentFunctionName() {
        return data.functionStack().peek();
    }


    /* ==== GETTERS ==== */

    public List<Pointer> getPointersFromDeclarator(SSCParser.DeclaratorContext declarator) {
        return declarator
                .pointer()
                .stream()
                .map(pointerCtx -> pointerCtx
                        .typeQualifierList()
                        .stream()
                        .flatMap(tqLs -> tqLs.typeQualifier().stream())
                        .map(this::visitTypeQualifier)
                        .toList()
                )
                .map(Pointer::qualified)
                .toList();
    }


    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final String ssName,
            final SSCParser.DeclaratorContext declarator
    ) {
        return tryCreateSuperstructVariableFromDeclarator(ssName, Pointer.none(), declarator);
    }

    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final Typedef<SuperStruct> typedef,
            final SSCParser.DeclaratorContext declarator
    ) {
        final SuperStruct ss = typedef.getRepresentedType();
        return tryCreateSuperstructVariableFromDeclarator(
                ss.name(),
                typedef.getPointers(),
                declarator
        );
    }

    public Optional<SuperstructVariable> tryCreateSuperstructVariableFromDeclarator(
            final String ssName,
            final List<Pointer> pointerBase,
            final SSCParser.DeclaratorContext declarator
    ) {
        if (declarator == null) {
            return Optional.empty();
        }
        final SSCParser.DirectDeclaratorContext directDecl = declarator.directDeclarator();
        if (directDecl.Identifier() == null) {
            return Optional.empty();
        }

        final List<Pointer> declaratorPointers = getPointersFromDeclarator(declarator);
        final String varName = this.visitTerminal(directDecl.Identifier());

        final SuperstructVariable ssVar = new SuperstructVariable(
                ssName,
                Pointer.combine(pointerBase, declaratorPointers),
                varName
        );
        return Optional.of(ssVar);
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

        logger.printDebug("Superstructs:");
        for (final SuperStruct ss : data.superStructs().values()) {
            logger.printDebug(() -> "\t" + ss);
        }

        logger.printDebug("Templates:");
        for (final Map.Entry<String, Template> entry : data.templates().entrySet()) {
            logger.printDebug(() -> "\t" + entry.getValue());
        }

        logger.printDebug("Typedefs:");
        for (final Map.Entry<String, Typedef<SuperStruct>> entry : data.superstructTypedefs().entrySet()) {
            logger.printDebug(() -> "\t" + entry.getKey() + " -> " + entry.getValue());
        }

        logger.printDebug("Function superstruct variables:");
        for (final Map.Entry<@Nullable String, Set<SuperstructVariable>> fnNameToSSVars : data.functionVariables().entrySet()) {
            final String funcDisplayName = fnNameToSSVars.getKey() == null ? "<global>" : "'" + fnNameToSSVars.getKey() + "'";
            final Set<SuperstructVariable> variables = fnNameToSSVars.getValue();

            if (variables.isEmpty()) {
                // logger.printDebug("\tEmpty scope " + funcDisplayName);
                continue;
            }

            logger.printDebug(() -> "\tFor scope " + funcDisplayName + ":");
            for (final SuperstructVariable variable : variables) {
                logger.printDebug(() -> "        " + variable);
            }
        }

        logger.printDebug("Debug dump complete");
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

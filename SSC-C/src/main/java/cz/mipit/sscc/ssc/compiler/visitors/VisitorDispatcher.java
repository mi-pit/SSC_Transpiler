package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.data.var.Pointer;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.Convertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.CustomDeclSpecConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FlagsConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FunctionDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.FunctionHeaderConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.LambdaConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.ParameterTypeListConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.PostfixExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.PrimaryExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructInterfaceConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SuperstructMemberConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.SwitchExpressionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateDefinitionConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TemplateDispatchConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.convertors.TernaryOperatorConvertor;
import cz.mipit.sscc.ssc.compiler.visitors.fmt.FormattingConvertor;
import cz.mipit.sscc.util.VisitorInput;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.StringJoiner;

import static cz.mipit.sscc.Main.logger;


public class VisitorDispatcher extends FormattingConvertor {
    public final CompilerData data;

    private final List<String> externalDeclarationsToEmitBefore = new ArrayList<>();
    private final List<String> externalDeclarationsToEmitAfter = new ArrayList<>();

    private final List<String> blockListItemsToEmitBefore = new ArrayList<>();

    private final Stack<Map<String, String>> replacements = new Stack<>();
    public final Map<TerminalNode, String> terminalReplacements = new HashMap<>();

    private final VariableCollector collector;

    private final Map<Class<? extends ParserRuleContext>, Convertor<? extends ParserRuleContext>> convertors;


    public VisitorDispatcher(VisitorInput input) {
        super(input.tokens(), input.file());

        data = new CompilerData(input.symbolTable());

        collector = new VariableCollector(this);

        final List<Convertor<? extends ParserRuleContext>> convertorsList = List.of(
                new PostfixExpressionConvertor(this),
                new PrimaryExpressionConvertor(this),
                new TernaryOperatorConvertor(this),

                new FlagsConvertor(this),
                new LambdaConvertor(this),
                new SwitchExpressionConvertor(this),

                new FunctionHeaderConvertor(this),
                new FunctionDefinitionConvertor(this),
                new CustomDeclSpecConvertor(this),
                new SuperstructMemberConvertor(this),
                new ParameterTypeListConvertor(this),

                new SuperstructInterfaceConvertor(this),
                new SuperstructConvertor(this),

                new TemplateDispatchConvertor(this),
                new TemplateDefinitionConvertor(this)
        );

        convertors = new HashMap<>();

        for (final Convertor<? extends ParserRuleContext> convertor : convertorsList) {
            if (convertors.put(convertor.getContextClass(), convertor) != null) {
                throw new IllegalStateException("Duplicate convertor found for " + convertor.getContextClass());
            }
        }
    }


    @Override
    public String visit(ParseTree tree) {
        if (!(tree instanceof ParserRuleContext prc)) {
            return visitSuper(tree);
        }

        final Class<? extends ParserRuleContext> treeClass = prc.getClass();
        if (!convertors.containsKey(treeClass)) {
            return visitSuper(tree);
        }

        return applyConvertor(convertors.get(treeClass), prc);
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        if (terminalReplacements.containsKey(node)) {
            assert node.getSymbol().getType() == SSCParser.Identifier;
            return terminalReplacements.get(node);
        }

        return switch (node.getSymbol().getType()) {
            case SSCParser.Identifier -> {
                if (replacements.isEmpty() || !replacements.peek().containsKey(node.getText())) {
                    yield node.getText();
                }
                yield replacements.peek().get(node.getText());
            }

            case Token.EOF,
                 SSCParser.StaticFunction,
                 SSCParser.Pure,
                 SSCParser.Private -> "";

            case SSCParser.Superstruct -> "struct";
            case SSCParser.FlagsSet -> "enum";

            case SSCParser.Then -> "?";

            default -> node.getText();
        };
    }

    public String visitSuper(ParseTree tree) {
        return super.visit(tree);
    }

    private <T extends ParserRuleContext> String applyConvertor(
            final Convertor<T> convertor,
            final ParserRuleContext ctx
    ) {
        final Class<T> cnvClass = convertor.getContextClass();

        if (ctx.getClass() != cnvClass)
            throw new AssertionError("Trying to convert '"
                                     + ctx.getClass() + "' using convertor of class '" + cnvClass + "'");

        return convertor.convert(cnvClass.cast(ctx));
    }

    @Override
    public String visitExternalDeclaration(SSCParser.ExternalDeclarationContext ctx) {
        // visit first
        final String external = super.visitChildren(ctx);

        final StringJoiner joiner = new StringJoiner(System.lineSeparator());
        dumpListToJoiner(externalDeclarationsToEmitBefore, joiner, "");
        joiner.add(external);
        dumpListToJoiner(externalDeclarationsToEmitAfter, joiner, "");

        return joiner.toString();
    }

    @Override
    public String visitBlockItem(SSCParser.BlockItemContext ctx) {
        final String item = super.visitBlockItem(ctx);

        final StringJoiner joiner = new StringJoiner(System.lineSeparator());

        dumpListToJoiner(blockListItemsToEmitBefore, joiner, getIndent());
        joiner.add(item);

        return joiner.toString();
    }

    private static void dumpListToJoiner(List<String> ls, StringJoiner joiner, String indent) {
        for (final String item : ls) {
            joiner.add(indent + item);
        }
        ls.clear();
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

    public void addBlockListItemToEmitBefore(String code) {
        blockListItemsToEmitBefore.add(code);
    }


    public boolean hasType(String typeName) {
        return data.symbolTable().resolve(typeName) != null;
    }


    /// Used only in {@link VisitorDispatcher#pushFunction(String, ParserRuleContext)}
    private final Map<String, ParserRuleContext> _functionDefinitions = new HashMap<>();

    /**
     * Pushes a new function stack
     *
     * @param name        Function name
     * @param functionCtx Context of the function used in error messages -- thrown if name is duplicate.
     *                    Context may be null if it is certain that function name is not duplicate (e.g. lambda functions)
     */
    public void pushFunction(String name, ParserRuleContext functionCtx) {
        Objects.requireNonNull(name, "Function name cannot be null");

        if (_functionDefinitions.containsKey(name)) {
            throw getSSCCallbackException(
                    "Duplicate function name: '" + name + "'",
                    functionCtx, _functionDefinitions.get(name)
            );
        }

        data.initializeFunctionVariables(name);
        data.functionStack().push(name);
        _functionDefinitions.put(name, functionCtx);
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

    /// Searches current function & global variables
    public Optional<SuperstructVariable> findSuperstructVariable(String objectName) {
        final String functionName = getCurrentFunctionName();

        for (SuperstructVariable var : data.functionVariables(functionName)) {
            if (var.getIdentifier().equals(objectName)) {
                return Optional.of(var);
            }
        }

        for (SuperstructVariable var : data.functionVariables(null)) {
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
        for (final Map.Entry<@Nullable String, Set<SuperstructVariable>> fnNameToSSVars : data.functionVariables.entrySet()) {
            final String name = fnNameToSSVars.getKey();
            if (name != null && name.startsWith("<")) {
                continue;
            }

            final String funcDisplayName = name == null ? "<global>" : "'" + name + "'";
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


    public void pushReplacementsFrame() {
        this.replacements.push(new HashMap<>());
    }

    public void popReplacementsFrame() {
        this.replacements.pop();
    }


    public void addReplacements(Map<String, String> typeArgMap) {
        this.replacements.peek().putAll(typeArgMap);
    }

    public void addReplacement(String key, String value) {
        this.replacements.peek().put(key, value);
    }

    public void removeReplacements(Map<String, String> typeArgMap) {
        for (final String key : typeArgMap.keySet()) {
            this.replacements.peek().remove(key);
        }
    }

    public void removeReplacement(String key) {
        this.replacements.peek().remove(key);
    }
}

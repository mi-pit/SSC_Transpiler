package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SSCParser;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
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
import java.util.Stack;
import java.util.StringJoiner;

import static cz.mipit.sscc.Main.logger;


public class VisitorDispatcher extends FormattingConvertor {
    public final CompilerState state;

    private final List<String> externalDeclarationsToEmitBefore = new ArrayList<>();
    private final List<String> externalDeclarationsToEmitAfter = new ArrayList<>();

    private final List<String> blockListItemsToEmitBefore = new ArrayList<>();

    private final Stack<Map<String, String>> replacements = new Stack<>();
    private final Map<TerminalNode, String> terminalReplacements = new HashMap<>();

    private final VariableCollector collector;

    private final Map<Class<? extends ParserRuleContext>, Convertor<? extends ParserRuleContext>> convertors;


    public VisitorDispatcher(VisitorInput input) {
        super(input.tokens(), input.file());

        state = new CompilerState(input.symbolTable(), this);

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
    public String visitCompoundStatement(SSCParser.CompoundStatementContext ctx) {

        state.pushScope();
        final String s = super.visitCompoundStatement(ctx);
        state.popScope();

        return s;
    }

    @Override
    public String visitBlockItemList(SSCParser.BlockItemListContext ctx) {
        return super.visitBlockItemList(ctx);
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
        return state.symbolTable().resolve(typeName) != null;
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

        state.functionStack().push(name);
        state.pushScope(name);
        _functionDefinitions.put(name, functionCtx);
    }

    public void popFunction() {
        state.popScope();
        state.functionStack().poll();
    }

    public String getCurrentFunctionName() {
        return state.functionStack().peek();
    }


    /* ==== GETTERS ==== */


    /// Searches current function & global variables
    public Optional<SuperstructVariable> findSuperstructVariable(String objectName) {
        for (Map.Entry<String, SuperstructVariable> entry : state.currentVariables().entrySet()) {
            if (entry.getKey().equals(objectName)) {
                return Optional.of(entry.getValue());
            }
        }

        return Optional.empty();
    }


    public void debugPrintDump() {
        logger.printDebug("");
        logger.printDebug("Dumping debug info...");

        logger.printDebug("Superstructs:");
        // TODO
//        for (final SuperStruct ss : state.superStructs.values()) {
//            logger.printDebug("\t" + ss);
//        }

        logger.printDebug("Templates:");
        for (final Map.Entry<String, Template> entry : state.templates().entrySet()) {
            logger.printDebug("\t" + entry.getValue());
        }

        logger.printDebug("Scopes:");
        logger.printDebug(state.debugInfo());

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


    /// Replaces that specific node with the other argument when visiting next
    public void addTerminalReplacement(TerminalNode node, String replacement) {
        terminalReplacements.put(node, replacement);
    }

    public void removeTerminalReplacement(TerminalNode node) {
        terminalReplacements.remove(node);
    }
}

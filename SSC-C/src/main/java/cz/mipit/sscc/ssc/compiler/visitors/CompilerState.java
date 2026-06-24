package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SymbolTable;
import cz.mipit.sscc.ssc.compiler.data.FunctionSSCData;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.data.var.LambdaVariable;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.util.SSCCUtil;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;


public final class CompilerState {
    private static final String GLOBAL_SCOPE_NAME = "<global>";

    private static abstract class WithContext<T> {
        T var;
        ParseTree ctx;

        WithContext(T var, ParseTree ctx) {
            this.var = Objects.requireNonNull(var);
            this.ctx = Objects.requireNonNull(ctx);
        }

        abstract String identifier();

        abstract String kind();
    }

    private record Scope(
            Optional<String> name,
            Map<String, WithContext<SuperStruct>> superstructs,
            Map<String, WithContext<Typedef<SuperStruct>>> superstructTypedefs,
            Map<String, WithContext<SuperstructVariable>> superstructVariables,

            Map<String, WithContext<LambdaVariable>> literalVariables
    ) {
        private static <T> String mapToString(Collection<Map.Entry<String, WithContext<T>>> s) {
            return s.stream()
                    .map(e -> Map.entry(e.getKey(), e.getValue().var))
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        private String start(int depth) {
            final String deep = SSCCUtil.Text.INDENT.repeat(depth);
            return String.format("""
                            %s%s
                            %s{
                            """,
                    deep, name.map(s -> s + ": ").orElse("<unnamed> @" + depth),
                    deep
            );
        }

        private String end(int depth) {
            final String deep = SSCCUtil.Text.INDENT.repeat(depth);
            final String deeper = SSCCUtil.Text.INDENT.repeat(depth + 1);

            final String superstructs_ = mapToString(superstructs.entrySet());
            final String typedefs = mapToString(superstructTypedefs.entrySet());
            final String vars = mapToString(superstructVariables.entrySet());

            return String.format("""
                            %s%s
                            %s%s
                            %s%s
                            %s}
                            """,
                    deeper, superstructs_,
                    deeper, typedefs,
                    deeper, vars,
                    deep
            );
        }
    }

    private final SymbolTable symbolTable;

    private final Deque<Scope> scopes;

    private final StringBuilder debugScopesStack = new StringBuilder();
    private int debugScopesStack_currentDepth = 0;

    private final Map<String, Template> templates = new HashMap<>();
    private int templateStack;

    private Stack<SuperStruct> superstructStack;
    private final Deque<String> functionCallStack;

    private FunctionSSCData currentFunctionSSCData;

    private final VisitorDispatcher dispatcher;

    public boolean inAnInterface = false;


    public CompilerState(SymbolTable symbolTable, VisitorDispatcher dispatcher) {
        this.symbolTable = symbolTable;
        this.dispatcher = dispatcher;

        scopes = new ArrayDeque<>();
        pushScope(GLOBAL_SCOPE_NAME);

        superstructStack = new Stack<>();
        functionCallStack = new ArrayDeque<>();

        templateStack = 0;
    }

    public void pushScope(String name) {
        final Scope scope = new Scope(
                Optional.ofNullable(name),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>(),
                new HashMap<>()
        );
        scopes.push(scope);

        debugScopesStack
                .append(scope.start(debugScopesStack_currentDepth++));
    }

    public void pushScope() {
        pushScope(null);
    }

    public void popScope() {
        if (scopes.size() <= 1)
            throw new UnsupportedOperationException("Cannot pop global scope");

        _popScope();
    }

    private void _popScope() {
        final Scope popped = scopes.pop();
        debugScopesStack
                .append("\n")
                .append(popped.end(--debugScopesStack_currentDepth));
    }


    public FunctionSSCData getCurrentFunctionSSCData() {
        return currentFunctionSSCData;
    }

    public void setCurrentFunctionSSCData(FunctionSSCData currentFunctionSSCData) {
        this.currentFunctionSSCData = currentFunctionSSCData;
    }


    private final Stack<Stack<SuperStruct>> superstructPopStack = new Stack<>();

    public void enterTemplate() {
        templateStack++;

        superstructPopStack.push(superstructStack);
        superstructStack = new Stack<>();
    }

    public void leaveTemplate() {
        if (templateStack == 0) {
            throw new UnsupportedOperationException("not in a template");
        }
        templateStack--;
        assert templateStack >= 0;

        final Stack<SuperStruct> newSuperstructStack = superstructStack;

        superstructStack = superstructPopStack.pop();
        for (final SuperStruct newSuperstruct : newSuperstructStack) {
            superstructStack.push(newSuperstruct);
        }
    }

    public boolean isInATemplate() {
        return templateStack > 0;
    }


    public SuperStruct getSuperstruct(String name) {
        return getAllVisible(Scope::superstructs).get(name);
    }


    private static class SuperstructWithContext extends WithContext<SuperStruct> {
        SuperstructWithContext(SuperStruct var, ParseTree ctx) {
            super(var, ctx);
        }

        @Override
        String identifier() {
            return var.name();
        }

        @Override
        String kind() {
            return "superstruct";
        }
    }

    public void registerSuperstructIfNotDefined(String name, ParseTree ctx) {
        if (getSuperstruct(name) != null) {
            return;
        }

        addToScope(
                false,
                getGlobalScope(),
                Scope::superstructs,
                name,
                new SuperstructWithContext(new SuperStruct(name), ctx)
        );
    }

    public void pushSuperstruct(
            final SuperStruct superstruct,
            final ParseTree ctx
    ) {
        addToScope(
                false,
                isInATemplate()
                        ? getGlobalScope()
                        : getCurrentScope(),
                Scope::superstructs,
                superstruct.name(),
                new SuperstructWithContext(superstruct, ctx)
        );

        superstructStack.push(superstruct);
    }

    public Optional<SuperStruct> currentSuperstruct() {
        if (superstructStack.isEmpty())
            return Optional.empty();
        return Optional.of(superstructStack.peek());
    }

    public void popSuperstruct() {
        superstructStack.pop();
    }


    public Typedef<SuperStruct> getSuperstructTypedef(String typedefName) {
        return getAllVisible(Scope::superstructTypedefs).get(typedefName);
    }

    public void addSuperstructTypedef(
            Typedef<SuperStruct> typedef,
            ParseTree ctx
    ) {
        addToScope(
                Scope::superstructTypedefs,
                typedef.getIdentifier(),
                new WithContext<>(typedef, ctx) {
                    @Override
                    String identifier() {
                        return typedef.getIdentifier();
                    }

                    @Override
                    String kind() {
                        return "typedef";
                    }
                }
        );
    }


    public void addVariable(LambdaVariable var, ParseTree ctx) {
        addToScope(
                Scope::literalVariables,
                var.getIdentifier(),
                new WithContext<>(var, ctx) {
                    @Override
                    String identifier() {
                        return var.getIdentifier();
                    }

                    @Override
                    String kind() {
                        return "variable";
                    }
                }
        );
    }

    public LambdaVariable getLiteralVariable(String name) {
        return getAllVisible(Scope::literalVariables).get(name);
    }

    public Optional<SuperstructVariable> getSuperstructVariable(String name) {
        return Optional.ofNullable(getAllVisible(Scope::superstructVariables).get(name));
    }

    public Collection<LambdaVariable> visibleVariables() {
        return getAllVisible(Scope::literalVariables).values();
    }

    public List<LambdaVariable> nonGlobalVariables() {
        final List<LambdaVariable> nonGlobalVariables =
                new ArrayList<>(getAllVisible(Scope::literalVariables).values());
        nonGlobalVariables.removeAll(
                getGlobalScope().literalVariables.values().stream().map(wc -> wc.var).toList()
        );
        return nonGlobalVariables;
    }


    /**
     * @param var variable to be added to current scope
     * @param ctx for exception message creation
     */
    public void addSuperstructVariable(
            final SuperstructVariable var,
            ParseTree ctx
    ) {
        addToScope(
                Scope::superstructVariables,
                var.getIdentifier(),
                new WithContext<>(var, ctx) {
                    @Override
                    String identifier() {
                        return var.getIdentifier();
                    }

                    @Override
                    String kind() {
                        return "variable";
                    }
                }
        );
    }

    private <T> void addToScope(
            final Function<Scope, Map<String, WithContext<T>>> mapGetter,
            final String name,
            final WithContext<T> val
    ) {
        addToScope(true, getCurrentScope(), mapGetter, name, val);
    }

    private <T> void addToScope(
            boolean checkDuplicate,
            Scope scope,
            final Function<Scope, Map<String, WithContext<T>>> mapGetter,
            final String name,
            final WithContext<T> val
    ) {
        Objects.requireNonNull(val);
        assert !scopes.isEmpty();

        final WithContext<T> old = mapGetter.apply(scope).put(name, val);

        if (checkDuplicate && old != null) {
            throw dispatcher.getSSCCallbackException(
                    old.kind() + " '" + old.identifier() + "' already exists in scope"
                    + (scope.name.map(s -> " '" + s + "'").orElse("")),
                    val.ctx,
                    old.ctx
            );
        }
    }


    private Scope getGlobalScope() {
        final Scope global = scopes.getLast();

        if (global.name.isEmpty() || !global.name.get().equals(GLOBAL_SCOPE_NAME))
            throw new AssertionError();

        return global;
    }

    private Scope getCurrentScope() {
        return scopes.peek();
    }

    public Map<String, SuperstructVariable> currentVariables() {
        return getAllVisible(Scope::superstructVariables);
    }

    private <T> Map<String, T> getAllVisible(
            Function<Scope, Map<String, WithContext<T>>> setGetter
    ) {
        final Map<String, T> visibleVariables = new HashMap<>();

        for (final Scope scope : scopes) {
            for (final Map.Entry<String, WithContext<T>> e : setGetter.apply(scope).entrySet()) {
                visibleVariables.put(
                        e.getKey(),
                        e.getValue().var
                );
            }
        }

        return visibleVariables;
    }


    public Collection<SuperStruct> visibleSuperstructs() {
        return getAllVisible(Scope::superstructs).values();
    }


    public Map<String, Template> templates() {
        return templates;
    }

    public Deque<String> functionStack() {
        return functionCallStack;
    }

    public SymbolTable symbolTable() {
        return symbolTable;
    }


    String debugInfo() {
        _popScope();
        assert debugScopesStack_currentDepth == 0;
        return debugScopesStack.toString();
    }
}

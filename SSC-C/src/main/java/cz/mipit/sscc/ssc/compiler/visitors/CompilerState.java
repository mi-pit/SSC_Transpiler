package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SymbolTable;
import cz.mipit.sscc.ssc.compiler.data.FunctionSSCData;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.util.SSCCUtil;
import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
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
            this.var = var;
            this.ctx = ctx;
        }

        abstract String identifier();

        abstract String kind();
    }

    private record Scope(
            Optional<String> name,
            Map<@NotNull String, WithContext<SuperStruct>> superstructs,
            Map<@NotNull String, WithContext<Typedef<SuperStruct>>> superstructTypedefs,
            Map<@NotNull String, WithContext<SuperstructVariable>> superstructVariables
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

    private final Map<@NotNull String, Template> templates = new HashMap<>();
    private int templateStack;

    private Stack<SuperStruct> superstructStack;
    private final Deque<@NotNull String> functionCallStack;

    private FunctionSSCData currentFunctionSSCData;

    private final VisitorDispatcher dispatcher;


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
                new WithContext<>(superstruct, ctx) {
                    @Override
                    String identifier() {
                        return var.name();
                    }

                    @Override
                    String kind() {
                        return "superstruct";
                    }
                }
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


    /**
     * @param var variable to be added to current scope
     * @param ctx for exception message creation
     */
    public void addFunctionVariable(
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

    public Map<@Nullable String, SuperstructVariable> currentVariables() {
        return getAllVisible(Scope::superstructVariables);
    }

    private <T> Map<String, T> getAllVisible(
            Function<Scope, Map<String, WithContext<T>>> setGetter
    ) {
        final Map<String, T> visibleVariables = new HashMap<>();
        final Map<String, ParseTree> contexts = new HashMap<>();

        for (final Scope scope : scopes) {
            for (final Map.Entry<String, WithContext<T>> e : setGetter.apply(scope).entrySet()) {
                final T prev = visibleVariables.put(
                        e.getKey(),
                        e.getValue().var
                );

                if (prev != null) {
                    dispatcher.warn(
                            "Variable '" + e.getKey() + "' shadows previous definition",
                            e.getValue().ctx, contexts.get(e.getKey())
                    );
                }

                contexts.put(e.getKey(), e.getValue().ctx);
            }
        }

        return visibleVariables;
    }


    public Collection<SuperStruct> visibleSuperstructs() {
        return getAllVisible(Scope::superstructs).values();
    }

    public Collection<Typedef<SuperStruct>> visibleTypedefs() {
        return getAllVisible(Scope::superstructTypedefs).values();
    }

    public Collection<SuperstructVariable> visibleVariables() {
        return getAllVisible(Scope::superstructVariables).values();
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

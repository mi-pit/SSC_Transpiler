package cz.mipit.sscc.ssc.compiler.visitors;

import antlr.ssc.SymbolTable;
import cz.mipit.sscc.ssc.compiler.data.FunctionMetadata;
import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.tmpl.Template;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.util.annotations.NotNull;
import cz.mipit.sscc.util.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CompilerData {
    private final SymbolTable symbolTable;

    private final Map<@NotNull String, SuperStruct> superStructs;
    private final Deque<SuperStruct> superstructStack;

    private final Map<@NotNull String, Typedef<SuperStruct>> superstructTypedefs;

    private final Map<@NotNull String, Template> templates;

    private final Map<@Nullable String, Set<SuperstructVariable>> functionVariables;
    private final Deque<@NotNull String> functionCallStack;


    public FunctionMetadata currentFunctionMetadata;


    public CompilerData(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        superStructs = new HashMap<>();
        superstructTypedefs = new HashMap<>();
        functionVariables = new HashMap<>();
        templates = new HashMap<>();

        superstructStack = new ArrayDeque<>();
        functionCallStack = new ArrayDeque<>();

        functionVariables.put(null, new HashSet<>());
    }

    public Map<String, SuperStruct> superStructs() {
        return superStructs;
    }

    public void pushSuperstruct(final SuperStruct currentSS) {
        superstructStack.push(currentSS);
    }

    public Optional<SuperStruct> currentSuperstruct() {
        return Optional.ofNullable(superstructStack.peek());
    }

    public void popSuperstruct() {
        superstructStack.poll();
    }

    public Map<String, Typedef<SuperStruct>> superstructTypedefs() {
        return superstructTypedefs;
    }

    public Map<String, Set<SuperstructVariable>> functionVariables() {
        return functionVariables;
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
}

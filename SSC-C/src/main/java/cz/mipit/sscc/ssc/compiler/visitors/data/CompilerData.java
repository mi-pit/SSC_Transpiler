package cz.mipit.sscc.ssc.compiler.visitors.data;

import antlr.ssc.SymbolTable;
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
    private @Nullable SuperStruct currentSS;

    private final SymbolTable symbolTable; // TODO: move to data

    private final Map<@NotNull String, SuperStruct> superStructs;
    private final Map<@NotNull String, Typedef<SuperStruct>> superstructTypedefs;
    private final Map<@Nullable String, Set<SuperstructVariable>> functionVariables;

    private final Map<String, Template> templates;

    private final Deque<@NotNull String> functionCallStack;

    public CompilerData(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;

        currentSS = null;

        superStructs = new HashMap<>();
        superstructTypedefs = new HashMap<>();
        functionVariables = new HashMap<>();

        templates = new HashMap<>();

        functionCallStack = new ArrayDeque<>();

        functionVariables.put(null, new HashSet<>());
    }

    public Map<String, SuperStruct> superStructs() {
        return superStructs;
    }

    public void setCurrentSuperstruct(final SuperStruct currentSS) {
        this.currentSS = currentSS;
    }

    public Optional<SuperStruct> currentSuperstruct() {
        return Optional.ofNullable(currentSS);
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

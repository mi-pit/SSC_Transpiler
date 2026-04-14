package cz.mipit.sscc.ssc.compiler.visitors.data;

import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
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
    private final Map<String, SuperStruct> superStructs;
    private final Map<@NotNull String /* typedef name */, Typedef<SuperStruct>> superstructTypedefs;
    private final Map<@Nullable String, Set<SuperstructVariable>> functionVariables;
    private final Deque<String> functionCallStack;

    public CompilerData() {
        currentSS = null;
        superStructs = new HashMap<>();
        superstructTypedefs = new HashMap<>();
        functionVariables = new HashMap<>();
        functionCallStack = new ArrayDeque<>();

        functionVariables.put(null, new HashSet<>());
    }

    public void setCurrentSS(final SuperStruct currentSS) {
        this.currentSS = currentSS;
    }

    public Map<String, SuperStruct> superStructs() {
        return superStructs;
    }

    public Optional<SuperStruct> currentSS() {
        return Optional.ofNullable(currentSS);
    }

    public Map<String, Typedef<SuperStruct>> superstructTypedefs() {
        return superstructTypedefs;
    }

    public Map<String, Set<SuperstructVariable>> functionVariables() {
        return functionVariables;
    }

    public Deque<String> functionStack() {
        return functionCallStack;
    }
}

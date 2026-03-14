package cz.mipit.sscc.ssc.compiler.visitors;

import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;
import cz.mipit.sscc.ssc.compiler.data.var.SuperstructVariable;
import cz.mipit.sscc.ssc.compiler.data.var.Typedef;
import cz.mipit.sscc.util.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class VisitorData {
    private final Map<String, SuperStruct> superStructs;
    private SuperStruct currentSS;
    private final Map<String /* typedef name */, Typedef<SuperStruct>> superstructTypedefs;
    private final Map<@Nullable String, Set<SuperstructVariable>> functionVariables;
    private final Deque<String> functionCallStack;

    private VisitorData(Map<String, SuperStruct> superStructs,
                        SuperStruct currentSS,
                        Map<String, Typedef<SuperStruct>> superstructTypedefs,
                        Map<@Nullable String, Set<SuperstructVariable>> functionVariables,
                        Deque<String> functionCallStack) {
        this.superStructs = Objects.requireNonNull(superStructs);
        this.currentSS = currentSS;
        this.superstructTypedefs = Objects.requireNonNull(superstructTypedefs);
        this.functionVariables = Objects.requireNonNull(functionVariables);
        this.functionCallStack = Objects.requireNonNull(functionCallStack);

        functionVariables.put(null, new HashSet<>());
    }

    public VisitorData() {
        this(
                new HashMap<>(),
                null,
                new HashMap<>(),
                new HashMap<>(),
                new ArrayDeque<>()
        );
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

    public Map<String /* typedef name */, Typedef<SuperStruct>> superstructTypedefs() {
        return superstructTypedefs;
    }

    public Map<@Nullable String, Set<SuperstructVariable>> functionVariables() {
        return functionVariables;
    }

    public Deque<String> functionStack() {
        return functionCallStack;
    }
}

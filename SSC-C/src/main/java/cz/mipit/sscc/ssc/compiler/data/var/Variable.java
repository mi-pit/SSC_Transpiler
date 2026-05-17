package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class Variable {
    protected final String identifier;
    protected final List<Pointer> pointers;

    protected Variable(String identifier, List<Pointer> pointers) {
        this.identifier = Objects.requireNonNull(identifier);
        this.pointers = pointers;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<Pointer> getPointers() {
        return pointers;
    }

    protected final String createAbstractDeclarator() {
        return pointers.stream().map(Pointer::toCString).collect(Collectors.joining(" ")) + " " + identifier;
    }
}

package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents stuff like `*const * ‹identifier›`, which is common between all kinds of variables
 */
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


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        Variable variable = (Variable) o;
        return Objects.equals(identifier, variable.identifier) && Objects.equals(pointers, variable.pointers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, pointers);
    }
}

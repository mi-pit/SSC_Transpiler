package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.Objects;

public abstract class Variable {
    protected final String identifier;
    protected final int pointer;

    protected Variable(String identifier, int pointer) {
        this.identifier = Objects.requireNonNull(identifier);
        this.pointer = pointer;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int pointer() {
        return pointer;
    }

    protected final String abstractDeclarator() {
        return "*".repeat(pointer) + identifier;
    }
}

package cz.mipit.sscc.ssc.compiler.data;

import java.util.Objects;

public abstract class Variable {
    protected final String name;
    protected final boolean pointer;

    protected Variable(String name, boolean pointer) {
        this.name = Objects.requireNonNull(name);
        this.pointer = pointer;
    }

    public String getName() {
        return name;
    }

    public boolean isPointer() {
        return pointer;
    }
}

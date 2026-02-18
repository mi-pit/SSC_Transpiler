package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.Objects;

public abstract class Variable {
    protected final String name;
    protected final int pointer;

    protected Variable(String name, int pointer) {
        this.name = Objects.requireNonNull(name);
        this.pointer = pointer;
    }

    public String getName() {
        return name;
    }

    public int pointer() {
        return pointer;
    }

    public abstract String getDeclarator();

    protected final String abstractDeclarator() {
        return "*".repeat(pointer) + name;
    }
}

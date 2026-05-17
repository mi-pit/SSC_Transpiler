package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;

public class TypedVariable extends Variable {
    private final List<String> type;

    public TypedVariable(
            final List<String> type,
            final List<Pointer> pointers,
            final String name
    ) {
        super(name, pointers);
        this.type = type;
    }

    public String getDeclarator() {
        return String.join(" ", type) + " " + createAbstractDeclarator();
    }

    @Override
    public String toString() {
        return "TypedVariable{%s %s}".formatted(type, createAbstractDeclarator());
    }
}

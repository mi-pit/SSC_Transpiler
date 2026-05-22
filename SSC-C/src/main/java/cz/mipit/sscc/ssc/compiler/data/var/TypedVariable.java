package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;

public class TypedVariable extends Variable {
    private final List<String> declarationSpecifiers;

    public TypedVariable(
            final List<String> declarationSpecifiers,
            final List<Pointer> pointers,
            final String name
    ) {
        super(name, pointers);
        this.declarationSpecifiers = declarationSpecifiers;
    }

    public String getDeclarator() {
        return String.join(" ", declarationSpecifiers) + " " + createAbstractDeclarator();
    }

    @Override
    public String toString() {
        return "TypedVariable{%s %s}".formatted(declarationSpecifiers, createAbstractDeclarator());
    }
}

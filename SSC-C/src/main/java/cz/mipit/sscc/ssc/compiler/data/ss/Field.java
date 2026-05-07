package cz.mipit.sscc.ssc.compiler.data.ss;

import cz.mipit.sscc.ssc.compiler.data.var.TypedVariable;

import java.util.Objects;

public class Field {
    private final boolean isPrivate;

    private final TypedVariable variable;

    public Field(boolean isPrivate, TypedVariable variable) {
        this.isPrivate = isPrivate;
        this.variable = Objects.requireNonNull(variable);
    }

    public String getWhole() {
        return variable.getDeclarator();
    }

    public String getName() {
        return variable.getIdentifier();
    }

    public boolean isPrivate() {
        return isPrivate;
    }


    @Override
    public String toString() {
        return "Field{" + getWhole() + "}";
    }
}

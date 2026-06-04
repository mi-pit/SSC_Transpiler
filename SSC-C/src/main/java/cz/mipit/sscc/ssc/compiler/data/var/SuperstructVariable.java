package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;
import java.util.Objects;

public class SuperstructVariable extends Variable {
    private final String ssName;

    public SuperstructVariable(String ssName, List<Pointer> pointer, String name) {
        super(name, pointer);
        this.ssName = Objects.requireNonNull(ssName);
    }

    public String getSuperstructName() {
        return ssName;
    }

    @Override
    public String toString() {
        return String.format("[SuperStruct %s %s]", ssName, super.createAbstractDeclarator());
    }
}

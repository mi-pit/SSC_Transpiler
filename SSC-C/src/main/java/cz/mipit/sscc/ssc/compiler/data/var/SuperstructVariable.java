package cz.mipit.sscc.ssc.compiler.data.var;

import cz.mipit.sscc.ssc.compiler.data.ss.SuperStruct;

import java.util.List;
import java.util.Objects;

public class SuperstructVariable extends Variable {
    private final SuperStruct superstruct;

    public SuperstructVariable(SuperStruct superstruct, List<Pointer> pointer, String name) {
        super(name, pointer);
        this.superstruct = Objects.requireNonNull(superstruct);
    }

    public SuperStruct superstruct() {
        return superstruct;
    }

    @Override
    public String toString() {
        return String.format("[ss %s %s]", superstruct.name(), super.createAbstractDeclarator());
    }
}

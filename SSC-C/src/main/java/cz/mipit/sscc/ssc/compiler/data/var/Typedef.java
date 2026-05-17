package cz.mipit.sscc.ssc.compiler.data.var;

import java.util.List;

public class Typedef<T> extends Variable {
    private final T of;

    public Typedef(String name, List<Pointer> pointer, T of) {
        super(name, pointer);
        this.of = of;
    }

    public T getRepresentedType() {
        return of;
    }
}
